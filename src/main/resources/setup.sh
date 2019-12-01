#!/bin/bash
# ev3dev-lang-java setup script

####################
### PRINT BANNER ###

echo
echo "###################################"
echo "# EV3Dev-lang-java microInstaller #"
echo "###################################"
echo


##################
### PARSE ARGS ###

if [ "$1" == "setupSmall" ]; then
    echo "Running setup for JRI/JRE..."
    MODE="small"
elif [ "$1" == "setupBig" ]; then
    echo "Running setup for JDK..."
    MODE="full"
else
    echo "Unknown argument!" >&2
    exit 1
fi

##########################
### PLATFORM DETECTION ###

# detect platform
PLATFORM="unknown"
if [ -d "/sys/class/power_supply/lego-ev3-battery" ] \
|| [ -d "/sys/class/power_supply/legoev3-battery" ]; then PLATFORM="ev3";
elif [ -d "/sys/class/power_supply/brickpi-battery"   ]; then PLATFORM="brickpi";
elif [ -d "/sys/class/power_supply/brickpi3-battery"  ]; then PLATFORM="brickpi3";
elif [ -d "/sys/class/power_supply/pistorms-battery"  ]; then PLATFORM="pistorms";
fi
if [ -n "$INSTALLER_OVERRIDE_PLATFORM" ]; then
    PLATFORM="$INSTALLER_OVERRIDE_PLATFORM"
fi

# detect OS
DEBIAN="unknown"
if   grep stretch /etc/os-release >/dev/null; then DEBIAN="stretch";
elif grep buster  /etc/os-release >/dev/null; then DEBIAN="buster";
fi
if [ -n "$INSTALLER_OVERRIDE_DEBIAN" ]; then
    DEBIAN="$INSTALLER_OVERRIDE_DEBIAN"
fi

# print
echo "Platform detected: $PLATFORM on ev3dev-$DEBIAN"
echo

if [ "$PLATFORM" = "unknown" ]; then
    echo "Sorry, this platform is not recognized by the installer."
    echo "This installer was designed for EV3Dev hardware."
    echo
    echo "Open a issue if the problem continues:"
    echo "https://github.com/ev3dev-lang-java/ev3dev-lang-java/issues"
    echo
    exit 1
fi

if [ "$DEBIAN" = "unknown" ]; then
    echo "Sorry, this OS is not recognized by the installer."
    echo
    echo "Open a issue if the problem continues:"
    echo "https://github.com/ev3dev-lang-java/ev3dev-lang-java/issues"
    echo
    exit 1
fi


################
### LET'S GO ###

if [ "$PLATFORM" = "ev3" ]; then
    if [ "$MODE" == "small" ]; then
        # too slow
        #echo "Running APT update"
        #apt-get update || exit $?
        #echo

        echo "Installing JRI from ev3dev repository"
        apt-get install --yes --no-install-recommends jri-11-ev3 || exit $?
        echo

    elif [ "$MODE" == "full" ]; then
        # too slow
        #echo "Running APT update"
        #apt-get update || exit $?
        #echo

        echo "Installing full JDK manually from jenkins"

        echo "Cleaning up..."
        for i in /opt/jdk-11-ev3/bin/*; do
            update-alternatives --remove "$i" "/opt/jdk-11-ev3/bin/$i" || true
        done
        rm -rf "/opt/jdk-11-ev3" /tmp/java-extract || true
        mkdir -p /tmp/java-extract
        echo

        echo "Downloading Java..."
        wget -nv "https://ci.adoptopenjdk.net/view/ev3dev/job/eljbuild/job/$DEBIAN-11/lastSuccessfulBuild/artifact/build/jri-ev3.tar.gz" -O /tmp/java-extract/pkg.tar.gz  || return $?
        echo

        echo "Unpacking Java..."
        tar -C /tmp/java-extract -xf /tmp/java-extract/pkg.tar.gz
        mv "/tmp/java-extract/jdk" "/opt/jdk-11-ev3"
        echo

        echo "Setting up symlinks..."
        for i in /opt/jdk-11-ev3/bin/*; do
            update-alternatives --install "/usr/bin/$i" "$i" "/opt/jdk-11-ev3/bin/$i" 1199
        done
        echo
    fi
elif [ "$PLATFORM" = "brickpi"  ] ||
     [ "$PLATFORM" = "brickpi3" ] ||
     [ "$PLATFORM" = "pistorms" ]; then

    if [ "$DEBIAN" = "stretch" ]; then
        echo "Adding backports repository for JRE11"
        rm -f /etc/apt/preferences.d/jdk
        echo "deb http://ftp.debian.org/debian stretch-backports main" | tee "/etc/apt/sources.list.d/jdk.list"
    fi

    echo "Running APT update"
    apt-get update || exit $?
    echo

    echo "Installing JRE from Debian repo"
    if [ "$DEBIAN" = "stretch" ]; then
        TARGET_ARG="-t stretch-backports"
    elif [ "$DEBIAN" = "buster" ]; then
        TARGET_ARG=""
    fi
    if [ "$1" == "small" ]; then
        apt-get install --yes --no-install-recommends $TARGET_ARG openjdk-11-jre-headless || exit $?
    elif [ "$1" == "full" ]; then
        apt-get install --yes --no-install-recommends $TARGET_ARG openjdk-11-jdk-headless || exit $?
    fi
fi

echo "Installing OpenCV & RXTX libraries"
if [ "$DEBIAN" = "stretch" ]; then
  LIB_PKGS="libopencv2.4-java libopenmpt0 librxtx-java"
elif [ "$DEBIAN" = "buster" ]; then
  LIB_PKGS="libopencv3.2-java libopenmpt0=0.4.3-1 librxtx-java"
fi
apt-get install --yes --no-install-recommends $LIB_PKGS || exit $?
echo

########################
### CDS OPTIMIZATION ###

hash -r # remove cached java path

echo "Optimizing (creating CDS cache) ..."
java -Xshare:dump || exit $?
echo

echo "-> Java version:"
java -version || exit $?
echo


#########################
### PERMISSION FIXUP ###

echo "Fixing permissions on /home/robot..."
chown robot:robot -R /home/robot
