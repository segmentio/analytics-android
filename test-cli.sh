#!/bin/bash
#
# Simple script to test Java-based execution of Spoon. You must have assembled
# the jar prior to running this script (i.e., mvn clean verify).

set -e

./gradlew assemble assembleDebug assembleDebugTest build

APK=`\ls core/build/outputs/apk/*.apk`
java -jar spoon-*-jar-with-dependencies.jar --apk "$APK" --test-apk "$APK" --output core/build/spoon

open core/build/spoon/index.html