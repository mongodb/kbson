#!/bin/sh

set -o verbose
set -o errexit

SDK_ROOT=$1
if [ -z $SDK_ROOT ]; then
    echo "usage: $0 <sdk-root>"
    exit 1
fi
shift

export JAVA_HOME=/opt/java/jdk11

# The releases of the sdk tools are published at
# https://developer.android.com/studio#cmdline-tools
SDK_PACKAGE=commandlinetools-linux-8512546_latest.zip # 7.0

if [ ! -e  $SDK_ROOT ]; then
    mkdir $SDK_ROOT
    (
        cd $SDK_ROOT

        # https://dl.google.com/android/repository/commandlinetools-linux-8512546_latest.zip
        # 1. Download the latest "command line tools only" package from the Android Studio downloads page and unzip the package.
        # 2. Move the unzipped cmdline-tools directory into a new directory of your choice, such as android_sdk.
        # 3. This new directory is your Android SDK directory.
        # 4. In the unzipped cmdline-tools directory, create a sub-directory called latest.
        # 5. Move the original cmdline-tools directory contents, into the newly created latest directory.
        # You can now use the command line tools from this location.

        test -e $SDK_PACKAGE || curl -O https://dl.google.com/android/repository/$SDK_PACKAGE
        unzip -q ./$SDK_PACKAGE
        mkdir -p cmdline-tools/latest
        mv ./cmdline-tools/* ./cmdline-tools/latest || true
        yes | ./cmdline-tools/latest/bin/sdkmanager --channel=0 \
            "build-tools;33.0.0" \
            "platform-tools"  \
            "platforms;android-33"  \
        | grep -v Unzipping
    )
fi
