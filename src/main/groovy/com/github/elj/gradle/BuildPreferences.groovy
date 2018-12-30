package com.github.elj.gradle;

import org.gradle.api.Project

import java.nio.file.Paths;

class BuildPreferences {
    def splashPath = { Project proj, Extension ext ->
        return Paths.get(proj.projectDir.toString(), "gradle", "splash.txt")
    }

    def templateWrapper = { Project proj, Extension ext ->
        return """
#!/bin/sh
cat ${ext.brickSplashPath()}
exec ${ext.getJavaCommand(true)}
"""
    }

    def uploads = { Project proj, Extension ext ->
        return []
    }
}
