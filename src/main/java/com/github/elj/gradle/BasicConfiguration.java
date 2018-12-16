package com.github.elj.gradle;

import java.util.ArrayList;

public class BasicConfiguration {
    private String mainClass = "please.specify.main.class";
    private String brickHost = "0.0.0.0";
    private String brickUser = "robot";
    private String brickPassword = "maker";
    private int    brickTimeout = 5000;

    private boolean libOpenCV = false;
    private boolean libRXTX = false;
    private ArrayList<String> libCustom = new ArrayList<>();

    private boolean useSudo = false;
    private boolean useTime = false;
    private boolean useBrickrun = false;

    private ArrayList<String> jvmFlags = new ArrayList<>();

    private boolean slimJar = true;
    private boolean useEmbeddedPaths = false;

    public String getBrickHost() {
        return brickHost;
    }

    public void setBrickHost(String brickHost) {
        this.brickHost = brickHost;
    }

    public String getBrickUser() {
        return brickUser;
    }

    public void setBrickUser(String brickUser) {
        this.brickUser = brickUser;
    }

    public String getBrickPassword() {
        return brickPassword;
    }

    public void setBrickPassword(String brickPassword) {
        this.brickPassword = brickPassword;
    }

    public int getBrickTimeout() {
        return brickTimeout;
    }

    public void setBrickTimeout(int brickTimeout) {
        this.brickTimeout = brickTimeout;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public boolean getLibOpenCV() {
        return libOpenCV;
    }

    public void setLibOpenCV(boolean libOpenCV) {
        this.libOpenCV = libOpenCV;
    }

    public boolean getLibRXTX() {
        return libRXTX;
    }

    public void setLibRXTX(boolean libRXTX) {
        this.libRXTX = libRXTX;
    }

    public boolean getUseSudo() {
        return useSudo;
    }

    public void setUseSudo(boolean useSudo) {
        this.useSudo = useSudo;
    }

    public boolean getUseTime() {
        return useTime;
    }

    public void setUseTime(boolean useTime) {
        this.useTime = useTime;
    }

    public boolean getUseBrickrun() {
        return useBrickrun;
    }

    public void setUseBrickrun(boolean useBrickrun) {
        this.useBrickrun = useBrickrun;
    }

    public ArrayList<String> getJvmFlags() {
        return jvmFlags;
    }

    public void setJvmFlags(ArrayList<String> jvmFlags) {
        this.jvmFlags = jvmFlags;
    }

    public boolean getSlimJar() {
        return slimJar;
    }

    public void setSlimJar(boolean slimJar) {
        this.slimJar = slimJar;
    }

    public boolean getUseEmbeddedPaths() {
        return useEmbeddedPaths;
    }

    public void setUseEmbeddedPaths(boolean useEmbeddedPaths) {
        this.useEmbeddedPaths = useEmbeddedPaths;
    }

    public ArrayList<String> getLibCustom() {
        return libCustom;
    }

    public void setLibCustom(ArrayList<String> libCustom) {
        this.libCustom = libCustom;
    }
}
