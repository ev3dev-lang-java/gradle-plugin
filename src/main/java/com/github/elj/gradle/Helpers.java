package com.github.elj.gradle;

import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Helpers {
    private Project proj;
    private BasicConfiguration basic;
    private PathConfiguration paths;

    public Helpers(Project proj) {
        this.proj = proj;
        this.basic = (BasicConfiguration) proj.getExtensions().getByName(GradlePlugin.BASIC_EXT);
        this.paths = (PathConfiguration) proj.getExtensions().getByName(GradlePlugin.PATHS_EXT);
    }

    public String basename() {
        String suffix = basic.getSlimJar() ? "" : "-all";
        return proj.getName() + "-" + proj.getVersion().toString() + suffix;
    }

    public String brickProgramPath() {
        return paths.getProgramDir() + "/" + basename() + ".jar";
    }

    public String brickWrapperPath() {
        return paths.getWrapperDir() + "/" + basename() + ".sh";
    }

    public String brickSplashPath() {
        return paths.getSplashDir() + "/" + basename() + ".txt";
    }

    public Path localProgramPath() {
        return Paths.get(proj.getBuildDir().toString(), "libs", basename() + ".jar");
    }

    public Path localWrapperPath() {
        return Paths.get(proj.getBuildDir().toString(), "launcher.sh");
    }

    public Path localSplashPath() {
        return Paths.get(proj.getProjectDir().toString(), "gradle", "splash.txt");
    }

    private String filterClassPath(String url) {
        return url.replaceAll("^file://", "");
    }

    public String getJavaCommand(boolean wrapper) {
        ArrayList<String> javaArr = new ArrayList<>();
        javaArr.add("java");
        javaArr.addAll(basic.getJvmFlags());

        if (basic.getUseEmbeddedPaths()) {
            javaArr.add("-jar");
            javaArr.add(brickProgramPath());
        } else {
            javaArr.add("-cp \"" + getClassPath(false) + "\"");
            javaArr.add(basic.getMainClass());
        }

        ArrayList<String> prefixArr = new ArrayList<>();
        if (!wrapper) {
            if (basic.getUseTime()) {
                prefixArr.add("time");
            }
            if (basic.getUseBrickrun()) {
                prefixArr.add("brickrun --");
            }
        }

        if (basic.getUseSudo()) {
            String javaCmd = String.join(" ", javaArr);
            String shLine = ("echo \"" + basic.getBrickPassword() + "\" | sudo -S " + javaCmd).replaceAll("\"", "\\\\\"");
            prefixArr.add("/bin/sh -c \"" + shLine + "\"");
        } else {
            prefixArr.addAll(javaArr);
        }
        return String.join(" ", prefixArr);
    }

    public String getClassPath(boolean forJar) {
        final ArrayList<String> jarList = new ArrayList<>();

        if (basic.getSlimJar()) {
            proj.getConfigurations().getByName("runtime").forEach((File path) -> {
                jarList.add("file://" + paths.getLibraryDir() + "/" + path.getName());
            });
        }

        if (basic.getLibOpenCV()) {
            jarList.add("file://" + paths.getOpencvJar());
        }

        if (basic.getLibRXTX()) {
            jarList.add("file://" + paths.getRxtxJar());
        }

        jarList.addAll(basic.getLibCustom());

        if (forJar) {
            return jarList
                    .stream()
                    .collect(Collectors.joining(" "));
        } else {
            jarList.add(brickProgramPath());

            return jarList
                    .stream()
                    .map(url -> filterClassPath(url))
                    .collect(Collectors.joining(":"));
        }
    }

    public RemoteCommandTask createCommandTask(String grpName, String name, List<? extends Supplier<String>> commands, String desc) {
        RemoteCommandTask tsk = proj.getTasks()
                .create(name, RemoteCommandTask.class,
                        action -> action.setCommands(commands));
        tsk.setGroup(grpName);
        tsk.setDescription(desc);
        return tsk;
    }

    public RemoteCommandTask createCommandTask(String grpName, String name, Supplier<String> command, String desc) {
        return createCommandTask(grpName, name, Collections.singletonList(command), desc);
    }

    public RemoteCommandTask createSudoCommandTask(String grpName, String name, Supplier<String> command, String desc) {
        return createCommandTask(grpName, name, () -> "echo -e \"${project.brickPassword}\" | sudo -S " + command.get(), desc);
    }

    public void createServiceTasks(String grpName, String serviceName) {
        createServiceTask(grpName, serviceName, "stop");
        createServiceTask(grpName, serviceName, "restart");
    }

    private String capitalize(String str) {
        if (str.length() == 0) {
            return str;
        } else if (str.length() == 1) {
            return str.toUpperCase();
        } else {
            return Character.toUpperCase(str.charAt(0)) + str.substring(1);
        }
    }

    public RemoteCommandTask createServiceTask(String grpName, String serviceName, String action) {
        String taskName = action + capitalize(serviceName);
        String taskDesc = capitalize(action) + " the " + serviceName + " service.";

        return createSudoCommandTask(grpName, taskName, () -> "systemctl " + action + " " + serviceName, taskDesc);
    }
}
