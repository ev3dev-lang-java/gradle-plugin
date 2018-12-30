package com.github.elj.gradle

import org.gradle.api.Action
import org.gradle.api.Project

import javax.inject.Inject
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

class Extension {
    private Project proj

    Preferences pref = null
    PathPreferences paths = null

    @Inject
    Extension(Project mainProj) {
        proj = mainProj
        pref = mainProj.objects.newInstance(Preferences)
        paths = mainProj.objects.newInstance(PathPreferences)
    }

    void pref(Action<? super Preferences> action) {
        action.execute(pref)
    }

    void paths(Action<? super PathPreferences> action) {
        action.execute(paths)
    }


    String basename() {
        String suffix = pref.slimJar ? "" : "-all"
        return proj.name + "-" + proj.version + suffix
    }

    String brickProgramPath() {
        return paths.programDir + "/" + basename() + ".jar"
    }

    String brickWrapperPath() {
        return paths.wrapperDir + "/" + basename() + ".sh"
    }

    String brickSplashPath() {
        return paths.splashDir + "/" + basename() + ".txt"
    }

    Path localProgramPath() {
        return Paths.get(proj.buildDir.toString(), "libs", basename() + ".jar")
    }

    Path localWrapperPath() {
        return Paths.get(proj.buildDir.toString(), "launcher.sh")
    }

    Path localSplashPath() {
        return Paths.get(proj.projectDir.toString(), "gradle", "splash.txt")
    }

    String getJavaCommand(boolean wrapper) {
        def javaArr = []
        javaArr += "java"
        javaArr += pref.jvmFlags

        if (pref.useEmbeddedPaths) {
            javaArr += "-jar"
            javaArr += "\"${brickProgramPath()}\""
        } else {
            javaArr += "-cp"
            javaArr += "\"${getClassPath(false)}\""
            javaArr += pref.mainClass
        }

        def prefixArr = []
        if (!wrapper) {
            if (pref.useTime) {
                prefixArr += "time"
            }
            if (pref.useBrickrun) {
                prefixArr += "brickrun --"
            }
        }

        if (pref.useSudo) {
            String javaCmd = javaArr.join(" ")
            String shLine = "echo \"${pref.brickPassword}\" | sudo -S $javaCmd"
            String proper = shLine.replaceAll("\"", "\\\\\"")
            prefixArr += "/bin/sh -c \"$proper\""
        } else {
            prefixArr += javaArr
        }
        return prefixArr.join(" ")
    }

    String getClassPath(boolean forJar) {
        def jarList = []

        if (pref.slimJar) {
            proj.configurations.runtime.each { path ->
                jarList += "file://${paths.libraryDir}/${path.getName()}"
            }
        }

        if (pref.libOpenCV) {
            jarList += "file://${paths.opencvJar}"
        }

        if (pref.libRXTX) {
            jarList += "file://${paths.rxtxJar}"
        }

        jarList += pref.libCustom

        if (forJar) {
            return jarList.join(" ")
        } else {
            jarList += brickProgramPath()

            jarList.stream()
                    .map({ url -> url.toString().replaceAll("^file://", "") })
                    .collect(Collectors.joining(":"))
        }
    }

    RemoteCommandTask createCommandTask(String grpName, String name, commands, String desc) {
        return proj.tasks.create(name, RemoteCommandTask).tap {
            setCommands commands
            setGroup grpName
            setDescription desc
        }
    }

    RemoteCommandTask createSudoCommandTask(String grpName, String name, commands, String desc) {
        def list = []

        commands.each {
            list += "echo -e \"${-> pref.brickPassword}\" | sudo -S ${-> it.toString()}"
        }

        return createCommandTask(grpName, name, list, desc)
    }

    void createServiceTasks(String grpName, String serviceName) {
        createServiceTask(grpName, serviceName, "stop")
        createServiceTask(grpName, serviceName, "restart")
    }

    RemoteCommandTask createServiceTask(String grpName, String serviceName, String action) {
        String taskName = action + serviceName.capitalize()
        String taskDesc = "${action.capitalize()} the $serviceName service."

        return createSudoCommandTask(grpName, taskName, ["systemctl $action $serviceName"], taskDesc)
    }
}
