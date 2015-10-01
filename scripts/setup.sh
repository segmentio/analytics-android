#!/bin/bash

set -ex

export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$PATH"

DEPS="$ANDROID_HOME/installed-dependencies"

if [ ! -e $DEPS ]; then
  cp -r /usr/local/android-sdk-linux $ANDROID_HOME &&
  echo y | android update sdk --no-ui --all --filter tools,platform-tools,build-tools-23.0.1,android-23,extra-google-m2repository,extra-google-google_play_services,extra-android-support,extra-android-m2repository &&
  touch $DEPS
fi
