package com.github.elj.gradle;

import com.jcraft.jsch.JSch;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class GradlePlugin implements Plugin<Project> {
    public static final String BASIC_EXT = "ev3config";
    public static final String PATHS_EXT = "ev3paths";
    public static final String HELPER_EXT = "ev3";
    private static JSch sshInstance;

    public static JSch getSSH() {
        synchronized (GradlePlugin.class) {
            if (sshInstance == null) {
                sshInstance = new JSch();
            }
            return sshInstance;
        }
    }

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(org.gradle.api.plugins.JavaPlugin.class);
        project.getPluginManager().apply(com.github.jengelman.gradle.plugins.shadow.ShadowPlugin.class);

        BasicConfiguration basic = project.getExtensions().create(BASIC_EXT, BasicConfiguration.class);
        PathConfiguration paths = project.getExtensions().create(PATHS_EXT, PathConfiguration.class);
        Helpers helpers = project.getExtensions().create(HELPER_EXT, Helpers.class, project);
        registerInstallerTasks(helpers);
        registerSystemTasks(helpers);
        registerDeploymentTasks(project, helpers, basic, paths);
    }

    private void registerInstallerTasks(Helpers hlp) {
        final String group = "ELJ-Installer";

        hlp.createCommandTask(group, "getInstaller",
                Arrays.asList(
                        () -> "mkdir -p /home/robot/java",
                        () -> "/bin/sh -c \"if grep -i jessie /etc/os-release; then wget https://raw.githubusercontent.com/ev3dev-lang-java/installer/master/installer-jessie.sh -O /home/robot/java/installer.sh; else wget https://raw.githubusercontent.com/ev3dev-lang-java/installer/master/installer.sh -O /home/robot/java/installer.sh; fi\"",
                        () -> "chmod +x /home/robot/java/installer.sh"),
                "Download component installer on the brick.");

        hlp.createSudoCommandTask(group, "updateAPT",
                () -> "/home/robot/java/installer.sh update",
                "Update APT repositories.");

        hlp.createCommandTask(group, "helpInstall",
                () -> "/home/robot/java/installer.sh help",
                "Print the installer help.");

        hlp.createSudoCommandTask(group, "installJava",
                () -> "/home/robot/java/installer.sh java",
                "Install Java on the brick.");

        hlp.createSudoCommandTask(group, "installOpenCV",
                () -> "/home/robot/java/installer.sh opencv",
                "Install OpenCV libraries on the brick.");

        hlp.createSudoCommandTask(group, "installRXTX",
                () -> "/home/robot/java/installer.sh rxtx",
                "Install RXTX library on the brick.");

        hlp.createSudoCommandTask(group, "installJavaLibraries",
                () -> "/home/robot/java/installer.sh javaLibs",
                "Install Java libraries on the brick.");

        hlp.createCommandTask(group, "javaVersion",
                () -> "java -version",
                "Print Java version which is present on the brick.");
    }


    private void registerSystemTasks(Helpers hlp) {
        String group = "ELJ-System";

        hlp.createServiceTasks(group, "bluetooth");
        hlp.createServiceTasks(group, "ntp");
        hlp.createServiceTasks(group, "nmbd");
        hlp.createServiceTasks(group, "brickman");
        hlp.createCommandTask(group, "getDebianDistro",
                () -> "cat /etc/os-release", "Get the /etc/os-release file from the brick");
        hlp.createCommandTask(group, "free",
                () -> "free", "Print free memory summary.");
        hlp.createCommandTask(group, "ps",
                () -> "ps aux | sort -n -k 4", "Print list of running processes.");
        hlp.createSudoCommandTask(group, "shutdown",
                () -> "shutdown -h now", "Shutdown the brick.");
        hlp.createCommandTask(group, "ev3devInfo",
                () -> "ev3dev-sysinfo -m", "Get output of ev3dev-sysinfo -m.");
    }

    private void registerDeploymentTasks(Project proj, Helpers hlp, BasicConfiguration basic, PathConfiguration paths) {
        String group = "ELJ-Deployment";

        hlp.createCommandTask(group, "testConnection", () -> "ls", "Test connection to the brick.");
        hlp.createCommandTask(group, "pkillJava", () -> "pkill java", "Kill running Java instances.");
        hlp.createCommandTask(group, "undeploy",
                () -> "rm -f " + hlp.brickProgramPath() + " " + hlp.brickWrapperPath() + " " + hlp.brickSplashPath(),
                "Remove previously uploaded JAR.");

        Task wrap = proj.task("templateWrapper");
        wrap.setGroup(group);
        wrap.setDescription("Generate shell script wrapper for the program.");
        wrap.doLast(task -> {
            String contents = String.format(
                    "#!/bin/sh\ncat %s\nexec %s",
                    hlp.brickSplashPath(), hlp.getJavaCommand(true));
            try {
                Files.write(Paths.get(proj.getBuildDir().toString(), "launcher.sh"),
                        contents.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new GradleException("Error writing file", e);
            }
        });


        Task deploy = proj.task("deploy");
        deploy.setGroup(group);
        deploy.setDescription("Deploy a new build of the program to the brick.");
        deploy.dependsOn("clean", "templateWrapper", basic.getSlimJar() ? "jar" : "shadowJar");
        deploy.doLast(task -> {
            try (SSH ssh = SSH.create(proj)) {
                ssh.connect();
                ssh.openSftp();
                ssh.mkdir(paths.getWrapperDir());
                ssh.mkdir(paths.getProgramDir());
                ssh.mkdir(paths.getSplashDir());
                ssh.put(hlp.localProgramPath(), hlp.brickProgramPath(), 0644);
                ssh.put(hlp.localSplashPath(), hlp.brickSplashPath(), 0644);
                ssh.put(hlp.localWrapperPath(), hlp.brickWrapperPath(), 0755);
                ssh.closeSftp();
            } catch (Exception e) {
                throw new GradleException("Program upload failed.", e);
            }
        });

        hlp.createCommandTask(group, "run", () -> hlp.getJavaCommand(false),
                "Run the program that is currently loaded on the brick.");

        Task deployRun = proj.task("deployRun");
        deployRun.setGroup(group);
        deployRun.setDescription("Deploy a new build of the program to the brick and then run it.");
        deployRun.dependsOn("deploy", "run");
    }
}
