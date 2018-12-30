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
        sess = ssh.getSession(user, host).tap {
            setPassword(password)
            setConfig("StrictHostKeyChecking", "no")
            connect(timeout)
        }
    }

    void runStdio(String command) throws JSchException {
        ((ChannelExec) sess.openChannel("exec")).with {

            setInputStream(System.in, true)
            setOutputStream(System.out, true)
            setErrStream(System.err, true)

            setCommand(command)

            connect()

            while (!isClosed()) {
                try {
                    Thread.sleep(10)
                } catch (InterruptedException e) {
                    e.printStackTrace()
                }
            }
            if (getExitStatus() != 0) {
                throw new GradleException("Remote command returned failure: " + chan.getExitStatus())
            }
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
