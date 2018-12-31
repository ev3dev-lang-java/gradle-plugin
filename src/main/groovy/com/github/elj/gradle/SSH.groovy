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
        this(ext.pref.sshHost, ext.pref.sshUser, ext.pref.sshPassword, ext.pref.sshTimeout)
    }

    SSH(String host, String user, String password, int timeout) {
        sess = ssh.getSession(user, host).with {
            setPassword(password)
            setConfig("StrictHostKeyChecking", "no")
            try {
                connect(timeout)
            } catch (Exception e) {
                throw new GradleException("Cannot connect to the brick.", e)
            }
            return it
        }
    }

    void runStdio(String command) throws JSchException {
        int status = 0;
        try {
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
                status = getExitStatus()
            }
        } catch (Exception e) {
            throw new GradleException("Remote execution over SSH failed.", e)
        } finally {
            if (status != 0) {
                throw new GradleException("Remote command returned failure: $status")
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
