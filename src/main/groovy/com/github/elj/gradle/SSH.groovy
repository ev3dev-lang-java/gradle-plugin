package com.github.elj.gradle

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import org.gradle.api.GradleException

class SSH implements AutoCloseable {
    private static JSch ssh = new JSch()

    private Session sess = null

    SSH(Extension ext) {
        this(ext.pref.brickHost, ext.pref.brickUser, ext.pref.brickPassword, ext.pref.brickTimeout)
    }

    SSH(String host, String user, String password, int timeout) {
        sess = ssh.getSession(user, host)
        sess.setPassword(password)
        sess.setConfig("StrictHostKeyChecking", "no")
        sess.connect(timeout)
    }

    void runStdio(String command) throws JSchException {
        ChannelExec chan = (ChannelExec) sess.openChannel("exec")
        chan.setInputStream(System.in, true)
        chan.setOutputStream(System.out, true)
        chan.setErrStream(System.err, true)
        chan.setCommand(command)
        chan.connect()
        while (!chan.isClosed()) {
            try {
                Thread.sleep(10)
            } catch (InterruptedException e) {
                e.printStackTrace()
            }
        }
        if (chan.getExitStatus() != 0) {
            throw new GradleException("Remote command returned failure: " + chan.getExitStatus())
        }
    }

    @Override
    void close() throws Exception {
        sess?.disconnect()
    }

    SFTP openFileMode() throws Exception {
        return new SFTP(sess)
    }
}
