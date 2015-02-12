analytics-android
=================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.segment.analytics.android/core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.segment.analytics.android/core)
[![Circle CI](https://circleci.com/gh/segmentio/analytics-android.svg?style=shield)](https://circleci.com/gh/segmentio/analytics-android)
[![Javadocs](http://javadoc-badge.appspot.com/com.segment.analytics.android/core.svg?label=javadoc)](http://javadoc-badge.appspot.com/com.segment.analytics.android/core)

analytics-android is an Android client for [Segment](https://segment.com)

## Integrating with Segment

Interested in integrating your service with us? Check out our [Partners page](https://segment.com/partners/) for more details.

## Documentation

You can find usage documentation at [https://segment.com/libraries/android](https://segment.com/libraries/android).

#### Building with Gradle

To build the project with [Gradle](http://tools.android.com/tech-docs/new-build-system/user-guide), you will need to have Java 8 installed, and export a variable `$JAVA_HOME` that points to the installation directory.
If you have an existing Android SDK installed, make sure to export a variable `$ANDROID_HOME` that points to it. Once you've cloned the repo, simply run `./gradlew clean build`, which will automatically download all dependencies, build the project, and run all tests. Run `./gradlew tasks --all` to see all available tasks.

Check out how to contribute to the library, or add providers here: [https://segment.com/libraries/android#contributing](https://segment.com/libraries/android#contributing).

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

Copyright (c) 2014 Segment, Inc.

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
