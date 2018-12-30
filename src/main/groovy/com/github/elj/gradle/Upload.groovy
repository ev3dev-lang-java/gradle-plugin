package com.github.elj.gradle

import java.nio.file.Path

class Upload {
    Upload(Path source, String destination, int mode) {
        this.source = source
        this.destination = destination
        this.mode = mode
    }

    Path source
    String destination
    int mode
}
