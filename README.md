analytics-android
=================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.segment.analytics.android/core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.segment.analytics.android/core)
[![Build Status](https://travis-ci.org/segmentio/analytics-android.svg?branch=f2prateek%2Fgradle)](https://travis-ci.org/segmentio/analytics-android)

analytics-android is an Android client for [Segment.io](https://segment.io)

## Documentation

You can find usage documentation at [https://segment.io/libraries/android](https://segment.io/libraries/android).

#### Build via Gradle

To build the project with [Gradle](http://tools.android.com/tech-docs/new-build-system/user-guide), you will need to have Java installed, and export a variable `$JAVA_HOME` that points to the installlation directory.

The following steps are automated but, but if you run into any issues, make sure these up. You can install the Android SDK and set a variable `$ANDROID_HOME`, and open the SDK Manager tool and install the right SDK versions of Android (API 19) and the right version of the build tools (19.1.0).

Once you have set all this up, run `./gradlew tasks --all` to see all available tasks.
Runing `./gradlew clean build connectedTest` will run all the tests. Note that you'll need to have a connected device or emulator for tests.

Check out how to contribute to the library, or add providers here: [https://segment.io/libraries/android#contributing](https://segment.io/libraries/android#contributing).

## License

```
WWWWWW||WWWWWW
 W W W||W W W
      ||
    ( OO )__________
     /  |           \
    /o o|    MIT     \
    \___/||_||__||_|| *
         || ||  || ||
        _||_|| _||_||
       (__|__|(__|__|

The MIT License (MIT)

Copyright (c) 2014 Segment.io, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
