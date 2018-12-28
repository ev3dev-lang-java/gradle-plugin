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

        sftp.put sourceStream, destination, ChannelSftp.OVERWRITE
        sftp.chmod mode, destination
    }

    void mkdir(String destination) throws Exception {
        try {
            sftp.mkdir destination
        } catch (SftpException e) {
            if (e.id != ChannelSftp.SSH_FX_FAILURE || !sftp.stat(destination).isDir()) {
                throw e
            }
        }
    }
}
