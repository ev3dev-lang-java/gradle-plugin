package com.github.elj.gradle;

import com.jcraft.jsch.*;
import org.gradle.api.GradleException;
import org.gradle.api.Project;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;

public class SSH implements AutoCloseable {
    private JSch ssh;
    private Session sess = null;
    private BasicConfiguration conf;
    private ChannelSftp sftp = null;

    private SSH(Project proj, BasicConfiguration conf) {
        this.ssh = GradlePlugin.getSSH();
        this.conf = conf;
    }

    public static SSH create(Project proj) {
        return new SSH(proj, (BasicConfiguration)proj.getExtensions().getByName(GradlePlugin.BASIC_EXT));
    }

    public void connect() throws JSchException {
        sess = ssh.getSession(conf.getBrickUser(), conf.getBrickHost());
        sess.setPassword(conf.getBrickPassword());
        sess.setConfig("StrictHostKeyChecking", "no");
        sess.connect(conf.getBrickTimeout());
    }

    public void runStdio(String command) throws JSchException {
        ChannelExec chan = (ChannelExec) sess.openChannel("exec");
        chan.setInputStream(System.in, true);
        chan.setOutputStream(System.out, true);
        chan.setErrStream(System.err, true);
        chan.setCommand(command);
        chan.connect();
        while (!chan.isClosed()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
        if (chan.getExitStatus() != 0) {
            throw new GradleException("Remote command returned failure: " + chan.getExitStatus());
        }
    }

    public void disconnect() {
        sess.disconnect();
    }

    @Override
    public void close() throws Exception {
        if (sess != null) {
            sess.disconnect();
        }
    }

    public void openSftp() throws Exception {
        sftp = (ChannelSftp) sess.openChannel("sftp");
        sftp.connect();
    }

    public void closeSftp() throws Exception {
        if (sftp != null) {
            sftp.exit();
        }
    }

    public void put(Path source, String destination, int mode) throws Exception {
        System.out.println("Uploading file: " + source.getFileName());
        sftp.put(new FileInputStream(source.toFile()), destination, ChannelSftp.OVERWRITE);
        sftp.chmod(mode, destination);
    }

    public void mkdir(String destination) throws Exception {
        try {
            sftp.mkdir(destination);
        } catch (Exception e) {

        }
    }
}
