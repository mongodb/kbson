#!/bin/bash

############################################
#            Main Program                  #
############################################

set -o errexit  # Exit the script with error if any of the commands fail

OS=$(uname -s | tr '[:upper:]' '[:lower:]')
if [ "$OS" == "darwin" ]; then
   sudo xcode-select -s /Applications/Xcode13.2.1.app
else
   echo "Publishing requires a Mac machine." 1>&2
   exit 1
fi


export JAVA_HOME=/opt/java/jdk11
RELEASE=${RELEASE:false}

export ORG_GRADLE_PROJECT_nexusUsername=${NEXUS_USERNAME}
export ORG_GRADLE_PROJECT_nexusPassword=${NEXUS_PASSWORD}
export ORG_GRADLE_PROJECT_signingKey="${SIGNING_KEY}"
export ORG_GRADLE_PROJECT_signingPassword=${SIGNING_PASSWORD}

if [ "$RELEASE" == "true" ]; then
  TASK="publishArchives"
else
  TASK="publishSnapshots"
fi

SYSTEM_PROPERTIES="-Dorg.gradle.internal.publish.checksums.insecure=true -Dorg.gradle.internal.http.connectionTimeout=120000 -Dorg.gradle.internal.http.socketTimeout=120000"

./gradlew -version
./gradlew ${SYSTEM_PROPERTIES} --stacktrace --info  ${TASK}
