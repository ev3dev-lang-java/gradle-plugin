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
    BuildPreferences build = null

    @Inject
    Extension(Project mainProj) {
        proj = mainProj
        pref = mainProj.objects.newInstance(Preferences)
        paths = mainProj.objects.newInstance(PathPreferences)
        build = mainProj.objects.newInstance(BuildPreferences)
    }

    void pref(Action<? super Preferences> action) {
        action.execute(pref)
    }

    void paths(Action<? super PathPreferences> action) {
        action.execute(paths)
    }

    void build(Action<? super BuildPreferences> action) {
        action.execute(build)
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
        return build.splashPath(proj)
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

    RemoteCommandTask cmdTask(String grpName, String name, commands, String desc) {
        return proj.tasks.create(name, RemoteCommandTask).with {
            setCommands commands
            setGroup grpName
            setDescription desc
            return it
        }
    }

    RemoteCommandTask sudoTask(String grpName, String name, commands, String desc) {
        def list = []

        commands.each {
            list += "echo -e \"${-> pref.brickPassword}\" | sudo -S ${-> it.toString()}"
        }

        return cmdTask(grpName, name, list, desc)
    }

    void serviceTask(String grpName, String serviceName) {
        serviceTask(grpName, serviceName, "stop")
        serviceTask(grpName, serviceName, "restart")
    }

    RemoteCommandTask serviceTask(String grpName, String serviceName, String action) {
        String taskName = action + serviceName.capitalize()
        String taskDesc = "${action.capitalize()} the $serviceName service."

        return sudoTask(grpName, taskName, ["systemctl $action $serviceName"], taskDesc)
    }
}
