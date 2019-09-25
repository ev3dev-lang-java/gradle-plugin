package com.github.elj.gradle

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

class GradlePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.pluginManager.apply(JavaPlugin)
        project.pluginManager.apply(ShadowPlugin)

        Extension ext = project.extensions.create("brick", Extension, project)

        registerInstallerTasks(ext)
        registerSystemTasks(ext)
        registerDeploymentTasks(project, ext)
    }

    private static void registerInstallerTasks(Extension ext) {
        final String group = "ELJ-Setup"

        ext.with {
            setupTask(group, "setupEverything", "setupSmall",
                    "All-In-One installer; installs just JRI/JRE (enough for most uses) and OpenCV/RXTX libraries.")

            setupTask(group, "setupEverythingExpert", "setupBig",
                    "All-In-One installer; installs full JDK on the brick (overkill for normal use) and OpenCV/RXTX libraries.")
        }
    }

    private static void registerSystemTasks(Extension ext) {
        String group = "ELJ-System"

        ext.with {
            serviceTask(group, "bluetooth")
            serviceTask(group, "ntp")
            serviceTask(group, "nmbd")
            serviceTask(group, "brickman")
            cmdTask(group, "getDebianDistro",
                    ["cat /etc/os-release"], "Get the /etc/os-release file from the brick")
            cmdTask(group, "free",
                    ["free"], "Print free memory summary.")
            cmdTask(group, "ps",
                    ["ps aux | sort -n -k 4"], "Print list of running processes.")
            sudoTask(group, "shutdown",
                    ["shutdown -h now"], "Shutdown the brick.")
            cmdTask(group, "ev3devInfo",
                    ["ev3dev-sysinfo -m"], "Get output of ev3dev-sysinfo -m.")
        }
    }

    private static void registerDeploymentTasks(Project proj, Extension ext) {
        String group = "ELJ-Deployment"

        ext.with {
            cmdTask(group, "testConnection",
                    ["ls"], "Test connection to the brick.")
            cmdTask(group, "pkillJava",
                    ["pkill java"], "Kill running Java instances.")
            cmdTask(group, "undeploy",
                    ["rm -f ${-> ext.brickProgramPath()} ${-> ext.brickWrapperPath()} ${-> ext.brickSplashPath()}"],
                    "Remove previously uploaded JAR.")
            cmdTask(group, "undeployLibrary",
                    ["rm -f ${-> ext.brickLibraryPath()}"],
                    "Remove previously uploaded JAR (library mode).")

            cmdTask(group, "run",
                    ["${-> ext.getJavaCommand(false)}"],
                    "Run the program that is currently loaded on the brick.")
        }

        proj.with {
            task("templateWrapper").with {
                setGroup group
                setDescription "Generate shell script wrapper for the program."
                doLast({ task ->
                    String contents = ext.build.templateWrapper.call(proj)

                    try {
                        Files.write(Paths.get(proj.buildDir.toString(), "launcher.sh"),
                                contents.getBytes(StandardCharsets.UTF_8))
                    } catch (IOException e) {
                        throw new GradleException("Error writing wrapper.", e)
                    }
                })
            }

            tasks.create("deploy", DeploymentTask).with {
                setGroup group
                setDescription "Deploy a new build of the program to the brick."
                setIsLibrary false
                dependsOn "clean", "templateWrapper", "${-> ext.pref.slimJar ? "jar" : "shadowJar"}"
            }

            tasks.create("deployLibrary", DeploymentTask).with {
                setGroup group
                setDescription "Deploy a new build of a library to the brick."
                setIsLibrary true
                dependsOn "clean", "${-> ext.pref.slimJar ? "jar" : "shadowJar"}"
            }

            task("deployRun").with {
                setGroup group
                setDescription "Deploy a new build of the program to the brick and then run it."
                dependsOn "deploy", "run"
            }
        }
    }
}
