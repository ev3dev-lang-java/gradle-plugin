package com.github.elj.gradle;

public class PathConfiguration {
    private String wrapperDir = "/home/robot";
    private String libraryDir = "/home/robot/java/libraries";
    private String programDir = "/home/robot/java/programs";
    private String splashDir  = "/home/robot/java/splashes";
    private String opencvJar  = "/usr/share/java/opencv.jar";
    private String rxtxJar    = "/usr/share/java/RXTXcomm.jar";

    public String getWrapperDir() {
        return wrapperDir;
    }

    public void setWrapperDir(String wrapperDir) {
        this.wrapperDir = wrapperDir;
    }

    public String getLibraryDir() {
        return libraryDir;
    }

    public void setLibraryDir(String libraryDir) {
        this.libraryDir = libraryDir;
    }

    public String getProgramDir() {
        return programDir;
    }

    public void setProgramDir(String programDir) {
        this.programDir = programDir;
    }

    public String getSplashDir() {
        return splashDir;
    }

    public void setSplashDir(String splashDir) {
        this.splashDir = splashDir;
    }

    public String getOpencvJar() {
        return opencvJar;
    }

    public void setOpencvJar(String opencvJar) {
        this.opencvJar = opencvJar;
    }

    public String getRxtxJar() {
        return rxtxJar;
    }

    public void setRxtxJar(String rxtxJar) {
        this.rxtxJar = rxtxJar;
    }
}
