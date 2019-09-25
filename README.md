# Gradle plugin for projects using ev3dev-lang-java

## Why

This plugin is intended to provide a simple and unified user interface for various Gradle projects using ev3dev-lang-java.

## Deployment concept

This plugin uploads the program JAR to a central directory on the brick.
It then creates a shell script as a *launcher/wrapper* through which it is possible to start the JAR from the brick menu.
The wrapper will also display a textual *splash* - it will be displayed on the brick display when the program is loading.

## Using the plugin in a project

1. Create a Gradle project.
2. Insert the following snippet at the beginning of `settings.gradle`:

    ```gradle
    pluginManagement {
        resolutionStrategy {
            eachPlugin {
                if (requested.id.namespace == "com.github.ev3dev-lang-java") {
                    useModule("com.github.ev3dev-lang-java:${requested.id.name}:${requested.version}")
                }
            }
        }
        repositories {
            gradlePluginPortal()
            maven { url "https://jitpack.io" }
            jcenter()
            mavenCentral()
        }
    }
    ```

    This code is necessary for Gradle to be able to find this plugin.

3. Add a plugin declaration at the top of your `build.gradle` file:
    ```gradle
    plugins {
        id 'com.github.ev3dev-lang-java.gradle-plugin' version '<plugin version>'
    }
    ```
    You can find the latest version of this plugin here: https://jitpack.io/#ev3dev-lang-java/gradle-plugin

4. Create a new file named `config.gradle` in your project root directory.
    ```gradle
    brick.pref {
        // Main class //
        mainClass = "<main class of your program>"

        // Brick connection parameters //
        sshHost = "<ip address of your brick>"
        sshUser = "robot"
        sshPassword = "maker"
    }
    ```
    For more configuration options, see the following sections.
5. Insert the following snippet after the configurtion block in `build.gradle`:
    ```gradle

    apply from: './config.gradle'

    jar {
        manifest {
            attributes("Implementation-Title": project.name,
                       "Implementation-Version": version,
                       "Main-Class": brick.pref.mainClass,
                       "Class-Path": brick.getClassPath(true) )
        }
    }
    ```
   This includes the project configuration and it also pushes the information from this plugin to the JAR manifest.

## What's next

Now you can write your program. Before you upload/deploy the program, you may need to run the task `setupEverything`.
This will be needed in these circumstances:
* You are using a non-EV3 platform (BrickPi, PiStorms) without having previously installed Java.
* You want to use OpenCV or RXTX system libraries.

To upload the program, simly run the task `deploy`. This task also takes care of uploading dependencies and creating
a wrapper shell script for running programs from the brick menu.

## Using system OpenCV & RXTX

1. Make sure you have run the `setupEverything` task or that you have installed OpenCV/RXTX manually on the brick.
2. Add a corresponding version of the library to your project's `compileOnly` dependencies.
3. Enable `libOpenCV` or `libRXTX` flag in the plugin configuration block.

## All configuration options
### Main configuration
```gradle
// Main plugin configuration
brick.pref {
    // This lets you specify main class that will be used when running the program from the brick menu.
    mainClass = "please.specify.main.class"

    // Brick connection parameters
    sshHost = "10.0.0.2"
    sshUser = "robot"
    sshPassword = "maker"
    sshTimeout = 5000

    // Whether to include system OpenCV in classpath.
    libOpenCV = false

    // Whether to include system RXTX in classpath.
    libRXTX = false

    // Additional runtime classpath entries (paths to JAR libraries)
    libCustom += []

    // If true, the program will be run as root (using sudo with the password provided above)
    useSudo = false

    // If true, the program will be run wrapped in time command, which then prints the total execution time
    useTime = true

    // If true, the program will be run wrapped in brickrun command. Programs launched from Gradle
    // or from commandline will then appear like they have been started from the program menu.
    useBrickrun = false

    // Flags which will be passed to the on-brick JVM.
    jvmFlags = ["-Xms64m", "-Xmx64m", "-XX:+UseSerialGC"]
    // alternatively, just append some flags
    jvmFlags += []
    // some ideas:
    // - `-Xlog:class+load=info,class+unload=info ` - Display the debugging info for class loading.
    // - `-Xshare:on ` - Enable Class Data Sharing (recommended); often enabled automatically.
    // - `-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=7091 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false ` - Enable JMX agent.
    // - `-XX:TieredStopAtLevel=1 ` - Do not perform many optimizations. This can be used to speed up startup time.
    // - `-Djava.security.egd=file:/dev/./urandom ` - This can be used to speed up random number generation.

    // If true, dependencies will not be put into one giant JAR, rather they will be uploaded to
    // a Java library directory and then linked via manifest/runtime classpath.
    slimJar = true

    // If true, main class from JAR manifest will be used for the "run" Gradle task and the on-brick launcher.
    // If false, the main class will be directly passed to Java. It is then possible to change the main class for "run" without reuploading the program.
    useEmbeddedPaths = true
}
```
### Build configuration
```gradle
brick.build {
    // Lambda returning path to a text-mode "splash" for a program.
    splashPath = { Project proj ->
        return Paths.get(proj.projectDir.toString(), "gradle", "splash.txt")
    }

    // Lambda returning contents of the launcher script 
    templateWrapper = { Project proj ->
        return """#!/bin/sh
cat ${proj.brick.brickSplashPath()}
exec ${proj.brick.getJavaCommand(true)}
"""
    }

    // Lambda returning list of upload specifications
    // Upload specification is a three element list consisting of these parts:
    // - source path on the developer's computer
    // - destination path on the brick
    // - Unix permissions for the file
    uploads = { Project proj ->
        return [
            ['/bin/true', '/tmp/true', 0755]
        ]
    }
}
```

### Paths configuration
```gradle
brick.paths {
    // Configuration of where various things are stored.
    wrapperDir = "/home/robot"
    javaDir = "/home/robot/java"
    libraryDir = "/home/robot/java/libraries"
    programDir = "/home/robot/java/programs"
    splashDir = "/home/robot/java/splashes"
    opencvJar = "/usr/share/java/opencv.jar"
    rxtxJar = "/usr/share/java/RXTXcomm.jar"
}
```

## All provided Gradle tasks

The project has some tasks developed to interact in 3 areas:

- Deployment
- EV3Dev
- Installer


You can use the Java IDE to launch the tasks or you can execute them from the terminal:
```bash
./gradlew deployRun
```

### Setup tasks
These tasks need to be run once only when a fresh system is used.

- `setupEverything` - Installs a standard Java runtime (JRI/JRE) and OpenCV & RXTX libraries on the brick.
- `setupEverythingExpert` - Install an extended Java runtime (JDK) and OpenCV & RXTX libraries on the brick.

### Deployment tasks
- `deploy` - Upload the program and its dependencies to the brick.
- `deployLibrary` - Upload the program and its dependencies to the brick (into Java library directory).
- `undeploy` - Remove previously uploaded program.
- `undeployLibrary` - Remove previously uploaded program (from Java library directory).
- `run` - Run the program that is currently loaded on the brick.
- `deployRun` - Upload the program and its dependencies to the brick and then run it.
- `testConnection` - Test connection to the brick.
- `pkillJava` - Kill running Java instances.

### EV3Dev tasks
- `ev3devInfo` - Get system summary from `ev3dev-sysinfo -m`. Useful when sending bugreports to ev3dev-lang-java or ev3dev.
- `free` - Print free memory summary.
- `getDebianDistro` - Get Debian version information from `/etc/os-release`.
- `ps` - Print list of running processes.
- `stopBluetooth`/`restartBluetooth` - Stop/restart the Bluetooth service.
- `stopBrickman`/`restartBrickman` - Stop/restart the Brickman service.
- `stopNmbd`/`restartNmbd` - Stop/restart the NMBD service.
- `stopNtp`/`restartNtp` - Stop/restart the NTP service.
- `shutdown` - Shut down the brick.

## Examples

The primary consumer of this plugin is the template project:

https://github.com/ev3dev-lang-java/template-project-gradle/

## Issues

If you have any problem or doubt, use the main project.

https://github.com/ev3dev-lang-java/ev3dev-lang-java/issues
