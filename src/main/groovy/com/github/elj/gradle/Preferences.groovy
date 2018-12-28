package com.github.elj.gradle

class Preferences {
    String mainClass = "please.specify.main.class"
    String brickHost = "0.0.0.0"
    String brickUser = "robot"
    String brickPassword = "maker"
    int brickTimeout = 5000

    boolean libOpenCV = false
    boolean libRXTX = false
    def libCustom = []

    boolean useSudo = false
    boolean useTime = false
    boolean useBrickrun = false

    def jvmFlags = []

    boolean slimJar = true
    boolean useEmbeddedPaths = false
}
