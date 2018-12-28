package com.github.elj.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class RemoteCommandTask extends DefaultTask {
    @Input
    def commands = []

    @TaskAction
    void runCommand() throws Exception {
        Extension ext = this.project.ev3

        for (String command : commands) {

            String msg = String.format(
                    "---> Running \"%s\" <---",
                    command.replace(
                            ext.pref.brickPassword,
                            "********"))

            String echoCmd = "echo \"" + msg.replaceAll("\"", "\\\\\"") + "\""

            SSH ssh = null
            try {
                ssh = new SSH(ext)
                ssh.runStdio echoCmd
                ssh.runStdio command
            } finally {
                ssh?.close();
            }
        }
    }
}
