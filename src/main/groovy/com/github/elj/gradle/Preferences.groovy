package com.github.elj.gradle

class Preferences {
    String mainClass = "please.specify.main.class"
    String sshHost = "0.0.0.0"
    String sshUser = "robot"
    String sshPassword = "maker"
    int sshTimeout = 5000

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
