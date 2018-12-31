package com.github.elj.gradle;

import org.gradle.api.Project

import java.nio.file.Paths;

class BuildPreferences {
    def splashPath = { Project proj ->
        return Paths.get(proj.projectDir.toString(), "gradle", "splash.txt")
    }

    def templateWrapper = { Project proj ->
        return """
#!/bin/sh
cat ${proj.ev3.brickSplashPath()}
exec ${proj.ev3.getJavaCommand(true)}
"""
    }

    def uploads = { Project proj ->
        return []
    }
}
