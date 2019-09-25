package com.github.elj.gradle

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException

import java.nio.file.Path

class SFTP implements AutoCloseable {
    private ChannelSftp sftp = null

    SFTP(Session session) {
        sftp = (ChannelSftp) session.openChannel("sftp")
        sftp.connect()
    }

    @Override
    void close() throws Exception {
        sftp?.exit()
    }

    void put(Path source, String destination, int mode) throws Exception {
        System.out.println("Uploading file: " + source.getFileName())
        def sourceStream  = new FileInputStream(source.toFile())

        sftp.with {
            put sourceStream, destination, OVERWRITE
            chmod mode, destination
        }
    }

    void putIfNonexistent(Path source, String destination, int mode) throws Exception {
        try {
            sftp.stat(destination)
        } catch (SftpException ex) {
            if (ex.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                put(source, destination, mode)
            } else {
                throw ex
            }
        }
    }

    void putStream(InputStream stream, String name, String destination, int mode) throws Exception {
        System.out.println("Uploading file from stream: " + name);

        sftp.with {
            put stream, destination, OVERWRITE
            chmod mode, destination
        }
    }

    void mkdir(String destination) throws Exception {
        try {
            sftp.with {
                mkdir destination
            }
        } catch (SftpException e) {
            if (e.id != ChannelSftp.SSH_FX_FAILURE || !sftp.stat(destination).isDir()) {
                throw e
            }
        }
    }
}
