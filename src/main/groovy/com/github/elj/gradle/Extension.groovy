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

    /**
     *
     * @return
     */
    String basename() {
        String suffix = pref.slimJar ? "" : "-all"
        return proj.name + "-" + proj.version + suffix
    }

    /**
     * Get on-brick path to the program's JAR in the program directory.
     * @return On-brick path
     */
    String brickProgramPath() {
        return paths.programDir + "/" + basename() + ".jar"
    }

    /**
     * Get on-brick path to the program's JAR in the library directory.
     * @return On-brick path
     */
    String brickLibraryPath() {
        return paths.libraryDir + "/" + basename() + ".jar"
    }

    /**
     * Get on-brick path to the program's launcher/wrapper shell script.
     * @return On-brick path
     */
    String brickWrapperPath() {
        return paths.wrapperDir + "/" + basename() + ".sh"
    }

    /**
     * Get on-brick path to the program's splash.
     * @return On-brick path
     */
    String brickSplashPath() {
        return paths.splashDir + "/" + basename() + ".txt"
    }

    /**
     * Get the path to the built program JAR.
     * @return Local path to the program JAR.
     */
    Path localProgramPath() {
        return Paths.get(proj.buildDir.toString(), "libs", basename() + ".jar")
    }

    /**
     * Get the path to the generated launcher shell script.
     * @return Local path to the generated wrapper.
     */
    Path localWrapperPath() {
        return Paths.get(proj.buildDir.toString(), "launcher.sh")
    }

    /**
     * Get the path to the splash file.
     * @return Local path to the splash.
     */
    Path localSplashPath() {
        return build.splashPath.call(proj)
    }

    /**
     * Generate command for running this program.
     * @param wrapper Whether the command will be embedded into the JAR launcher on the brick
     * @return Commandline for running the program JAR.
     */
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
            String shLine = "echo \"${pref.sshPassword}\" | sudo -S $javaCmd"
            String proper = shLine.replaceAll("\"", "\\\\\"")
            prefixArr += "/bin/sh -c \"$proper\""
        } else {
            prefixArr += javaArr
        }
        return prefixArr.join(" ")
    }

    /**
     * Generate classpath string for the current project.
     *
     * @param forJar Whether the classpath will be used in the JAR manifest or not.
     * @return Java classpath string
     */
    String getClassPath(boolean forJar) {
        def jarList = []

        if (pref.slimJar) {
            proj.configurations.runtimeClasspath.each { path ->
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

    /**
     * Create a new command-running task.
     *
     * @param grpName Group to add the tasks to.
     * @param name Name of the task.
     * @param commands List of commands to run.
     * @param desc Description of the task
     * @return Command-running task.
     */
    RemoteCommandTask cmdTask(String grpName, String name, commands, String desc) {
        return proj.tasks.create(name, RemoteCommandTask).with {
            setCommands commands
            setGroup grpName
            setDescription desc
            return it
        }
    }

    /**
     * Create a new setup.sh task.
     *
     * @param grpName group to add the task to.
     * @param name Name of the task.
     * @param command Setup subcommand to run.
     * @param desc Description of the task.
     * @return Setup-running task.
     */
    SetupTask setupTask(String grpName, String name, command, String desc) {
        return proj.tasks.create(name, SetupTask).with {
            setCommand command
            setGroup grpName
            setDescription desc
            return it
        }
    }

    /**
     * Create a new command-running task. The command will be run as root.
     *
     * @param grpName Group to add the tasks to.
     * @param name Name of the task.
     * @param commands List of commands to run.
     * @param desc Description of the task
     * @return Command-running task.
     */
    RemoteCommandTask sudoTask(String grpName, String name, commands, String desc) {
        def list = []

        commands.each {
            list += "echo -e \"${-> pref.sshPassword}\" | sudo -S ${-> it.toString()}"
        }

        return cmdTask(grpName, name, list, desc)
    }

    /**
     * Create new service management tasks (stop, restart).
     *
     * @param grpName Group to add the tasks to.
     * @param serviceName Name of the systemd service to control.
     */
    void serviceTask(String grpName, String serviceName) {
        serviceTask(grpName, serviceName, "stop")
        serviceTask(grpName, serviceName, "restart")
    }

    /**
     * Create a new service management task.
     *
     * @param grpName Group to add the task to.
     * @param serviceName Name of the systemd service to control.
     * @param action Action to perform on the service -- usually start, stop, restart or status.
     * @return Task which executes systemctl as root.
     */
    RemoteCommandTask serviceTask(String grpName, String serviceName, String action) {
        String taskName = action + serviceName.capitalize()
        String taskDesc = "${action.capitalize()} the $serviceName service."

        return sudoTask(grpName, taskName, ["systemctl $action $serviceName"], taskDesc)
    }
}
