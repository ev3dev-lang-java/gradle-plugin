package com.github.elj.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class DeploymentTask extends DefaultTask {

    @Input
    boolean isLibrary = false

    @TaskAction
    void deploy() throws Exception {
        SSH ssh = null
        SFTP sftp = null
        try {
            System.out.println("/ Connecting...")
            try {
                ssh = new SSH(project.brick)
                sftp = ssh.openFileMode()
            } catch (GradleException e) {
                throw e
            } catch (Exception e) {
                throw new GradleException("SSH connection failed", e)
            }

            deployMkdir(sftp)
            deployDependencies(sftp)
            deployJar(sftp)
            deployLauncher(sftp)
            deployExtras(sftp)

        } catch (GradleException e) {
            throw e
        } catch (Exception e) {
            throw new GradleException("Program upload failed.", e)
        } finally {
            sftp?.close()
            ssh?.close()
        }
    }

    private void deployMkdir(SFTP sftp) throws Exception {
        System.out.println("/ Ensuring on-brick directory structure...")
        try {
            Extension ext = project.brick
            sftp.mkdir ext.paths.wrapperDir
            sftp.mkdir ext.paths.javaDir
            sftp.mkdir ext.paths.splashDir
            sftp.mkdir ext.paths.programDir
            sftp.mkdir ext.paths.libraryDir
        } catch (Exception e) {
            throw new GradleException("Creation of on-brick directories failed.", e)
        }
    }

    private void deployDependencies(SFTP sftp) throws Exception {
        System.out.println("/ Checking on-brick Java libraries...")
        try {
            Extension ext = project.brick
            project.configurations.runtimeClasspath.each {
                Path src = it.toPath()
                String dst = ext.paths.libraryDir + "/" + src.fileName.toString()
                int mode = 0644

                sftp.putIfNonexistent src, dst, mode
            }
        } catch (Exception e) {
            throw new GradleException("Upload of Gradle libraries failed.", e)
        }
    }

    private void deployJar(SFTP sftp) throws Exception {
        Extension ext = project.brick

        String remotePath
        if (isLibrary) {
            System.out.println("/ Uploading library JAR...")
            remotePath = ext.brickLibraryPath()
        } else {
            System.out.println("/ Uploading program JAR...")
            remotePath = ext.brickProgramPath()
        }
        sftp.with {
            try {
                put ext.localProgramPath(), remotePath, 0644
            } catch (Exception e) {
                throw new GradleException("Upload of program files failed.", e)
            }
        }
    }

    private void deployLauncher(SFTP sftp) throws Exception {
        if (!isLibrary) {
            System.out.println("/ Uploading program launcher...")
            try {
                Extension ext = project.brick
                sftp.put ext.localWrapperPath(), ext.brickWrapperPath(), 0755

                // provide a default splash
                Path splash = ext.localSplashPath()
                if (!Files.exists(splash)) {
                    InputStream str = null
                    try {
                        str = GradlePlugin.class.getResourceAsStream("/splash.txt")
                        sftp.putStream str, "[default splash]", ext.brickSplashPath(), 0644
                    } finally {
                        str?.close()
                    }
                } else {
                    sftp.put splash, ext.brickSplashPath(), 0644
                }

            } catch (Exception e) {
                throw new GradleException("Upload of program files failed.", e)
            }
        }
    }

    private void deployExtras(SFTP sftp) throws Exception {
        try {
            Extension ext = project.brick
            List uploads = ext.build.uploads.call(project)

            if (!uploads.empty) {
                System.out.println("/ Uploading additional files...")
                uploads.each { it ->
                    Path src = Paths.get(it[0] as String)
                    String dst = it[1] as String
                    int mode = it[2] as Integer

                    if (!src.toFile().exists()) {
                        throw new GradleException("Source file '${src.toString()}' does not exist.")
                    } else {
                        sftp.put src, dst, mode
                    }
                }
            }
        } catch (Exception e) {
            throw new GradleException("Upload of additional files failed.", e)
        }
    }
}
