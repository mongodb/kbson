#!/bin/bash

set -o xtrace   # Write all commands first to stderr
set -o errexit  # Exit the script with error if any of the commands fail

export JAVA_HOME=/opt/java/jdk11

############################################
#            Main Program                  #
############################################

echo "Compiling and running checks"

./gradlew -version
./gradlew --info clean check -x allTests
