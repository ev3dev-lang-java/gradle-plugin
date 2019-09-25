package com.github.elj.gradle

import com.jcraft.jsch.JSchException
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class SetupTask extends DefaultTask {
    @Input
    def command = ""

    @TaskAction
    void runSetup() throws Exception {
        Extension ext = this.project.brick

        SSH ssh = null
        SFTP sftp = null
        try {
            ssh = new SSH(ext)
            sftp = ssh.openFileMode()

            InputStream setupStream = null
            try {
                setupStream = SetupTask.class.getResourceAsStream("/setup.sh")
                sftp.putStream setupStream, "setup helper", "/tmp/setup.sh", 0777
            } finally {
                setupStream?.close()
            }

            ssh.runStdio "echo -e \"${-> ext.pref.sshPassword}\" | sudo -S /tmp/setup.sh ${command}"
        } catch(JSchException ex) {
            if (ex.getMessage() == "inputstream is closed") {
                throw new GradleException("Cannot connect to the brick!")
            }
        } finally {
            sftp?.close()
            ssh?.close()
        }
    }
}
