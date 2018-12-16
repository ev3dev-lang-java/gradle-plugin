package com.github.elj.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RemoteCommandTask extends DefaultTask {
    @Input
    private List<? extends Supplier<String>> commands = new ArrayList<>();

    public List<? extends Supplier<String>> getCommands() {
        return commands;
    }

    public void setCommands(List<? extends Supplier<String>> commands) {
        this.commands = commands;
    }

    private String getPassword() {
        return ((BasicConfiguration) getProject().getExtensions().getByName(GradlePlugin.BASIC_EXT)).getBrickPassword();
    }

    @TaskAction
    public void runCommand() throws Exception {
        for (Object command : commands.stream().map(Supplier::get).collect(Collectors.toList())) {
            String msg = String.format("Running \"%s\"", ((String) command).replace(getPassword(), ""));
            try (SSH ssh = SSH.create(getProject())) {
                ssh.connect();
                ssh.runStdio("echo \"" + msg.replaceAll("\"", "\\\\\"") + "\"");
                ssh.runStdio((String) command);
            }
        }
    }
}
