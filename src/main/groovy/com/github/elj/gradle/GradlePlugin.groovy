package com.github.elj.gradle

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class GradlePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.pluginManager.apply(JavaPlugin)
        project.pluginManager.apply(ShadowPlugin)

        Extension ext = project.extensions.create("brick", Extension, project)

        registerInstallerTasks(project, ext)
        registerSystemTasks(ext)
        registerDeploymentTasks(project, ext)
    }

    private static void registerInstallerTasks(Project proj, Extension ext) {
        final String group = "ELJ-Installer"

        ext.with {
            cmdTask(group, "getInstaller",
                    [
                            "mkdir -p /home/robot/java",
                            "/bin/sh -c \"if grep -i jessie /etc/os-release; then wget https://raw.githubusercontent.com/ev3dev-lang-java/installer/master/installer-jessie.sh -O /home/robot/java/installer.sh; else wget https://raw.githubusercontent.com/ev3dev-lang-java/installer/master/installer.sh -O /home/robot/java/installer.sh; fi\"",
                            "chmod +x /home/robot/java/installer.sh"
                    ],
                    "Download component installer on the brick.")

            sudoTask(group, "updateAPT",
                    ["/home/robot/java/installer.sh update"],
                    "Update APT repositories.")

            cmdTask(group, "helpInstall",
                    ["/home/robot/java/installer.sh help"],
                    "Print the installer help.")

            sudoTask(group, "installJava",
                    ["/home/robot/java/installer.sh java"],
                    "Install Java on the brick.")

            sudoTask(group, "installOpenCV",
                    ["/home/robot/java/installer.sh opencv"],
                    "Install OpenCV libraries on the brick.")

            sudoTask(group, "installRXTX",
                    ["/home/robot/java/installer.sh rxtx"],
                    "Install RXTX library on the brick.")

            sudoTask(group, "installJavaLibraries",
                    ["/home/robot/java/installer.sh javaLibs"],
                    "Install Java libraries on the brick.")

            cmdTask(group, "javaVersion",
                    ["java -version"],
                    "Print Java version which is present on the brick.")
        }

        proj.with {
            task("uploadGradleLibraries").with {
                setGroup group
                setDescription "Install libraries specified in the project dependencies to the brick."

                doLast({ task ->
                    SSH ssh = null
                    SFTP sftp = null
                    try {
                        try {
                            ssh = new SSH(ext);
                            sftp = ssh.openFileMode();
                        } catch (GradleException e) {
                            throw e
                        } catch (Exception e) {
                            throw new GradleException("SSH connection failed", e)
                        }

                        sftp.with {
                            try {
                                mkdir ext.paths.libraryDir
                            } catch (Exception e) {
                                throw new GradleException("Creation of on-brick library directory failed.", e)
                            }

                            try {
                                proj.configurations.runtime.each {
                                    Path src = it.toPath()
                                    String dst = ext.paths.libraryDir + "/" + src.fileName.toString()
                                    int mode = 644

                                    put src, dst, mode
                                }
                            } catch (Exception e) {
                                throw new GradleException("Upload of Gradle libraries failed.", e)
                            }
                        }
                    } catch (GradleException e) {
                        throw e
                    } catch (Exception e) {
                        throw new GradleException("Library upload failed.", e)
                    } finally {
                        sftp?.close()
                        ssh?.close()
                    }
                })
            }
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

            task("deploy").with {
                setGroup group
                setDescription "Deploy a new build of the program to the brick."
                dependsOn "clean", "templateWrapper", "${-> ext.pref.slimJar ? "jar" : "shadowJar"}"

                doLast({ task ->
                    SSH ssh = null
                    SFTP sftp = null
                    try {
                        try {
                            ssh = new SSH(ext);
                            sftp = ssh.openFileMode();
                        } catch (GradleException e) {
                            throw e
                        } catch (Exception e) {
                            throw new GradleException("SSH connection failed", e)
                        }

                        sftp.with {
                            try {
                                mkdir ext.paths.wrapperDir
                                mkdir ext.paths.splashDir
                                mkdir ext.paths.programDir
                            } catch (Exception e) {
                                throw new GradleException("Creation of on-brick directories failed.", e)
                            }

                            try {
                                put ext.localProgramPath(), ext.brickProgramPath(), 0644
                                put ext.localSplashPath(), ext.brickSplashPath(), 0644
                                put ext.localWrapperPath(), ext.brickWrapperPath(), 0755
                            } catch (Exception e) {
                                throw new GradleException("Upload of program files failed.", e)
                            }

                            ext.build.uploads.call(proj).each {
                                Path src = it[0] as Path
                                if (!src.toFile().exists()) {
                                    throw new GradleException("Source file '${src.toString()}' does not exist.")
                                }
                                put it[0] as Path, it[1] as String, it[2] as int
                            }
                        }
                    } catch (GradleException e) {
                        throw e
                    } catch (Exception e) {
                        throw new GradleException("Program upload failed.", e)
                    } finally {
                        sftp?.close()
                        ssh?.close()
                    }
                })
            }

            task("deployLibrary").with {
                setGroup group
                setDescription "Deploy a new build of a library to the brick."
                dependsOn "clean", "${-> ext.pref.slimJar ? "jar" : "shadowJar"}"

                doLast({ task ->
                    SSH ssh = null
                    SFTP sftp = null
                    try {
                        try {
                            ssh = new SSH(ext);
                            sftp = ssh.openFileMode();
                        } catch (GradleException e) {
                            throw e
                        } catch (Exception e) {
                            throw new GradleException("SSH connection failed", e)
                        }

                        sftp.with {
                            try {
                                mkdir ext.paths.libraryDir
                            } catch (Exception e) {
                                throw new GradleException("Creation of on-brick directories failed.", e)
                            }

                            try {
                                put ext.localProgramPath(), ext.brickLibraryPath(), 0644
                            } catch (Exception e) {
                                throw new GradleException("Upload of program files failed.", e)
                            }

                            ext.build.uploads.call(proj).each {
                                Path src = it[0] as Path
                                if (!src.toFile().exists()) {
                                    throw new GradleException("Source file '${src.toString()}' does not exist.")
                                }
                                put it[0] as Path, it[1] as String, it[2] as int
                            }
                        }
                    } catch (GradleException e) {
                        throw e
                    } catch (Exception e) {
                        throw new GradleException("Program upload failed.", e)
                    } finally {
                        sftp?.close()
                        ssh?.close()
                    }
                })
            }

            task("deployRun").with {
                setGroup group
                setDescription "Deploy a new build of the program to the brick and then run it."
                dependsOn "deploy", "run"
            }
        }
    }
}
