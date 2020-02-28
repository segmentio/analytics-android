analytics-android-integration-amplitude
=======================================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.segment.analytics.android.integrations/amplitude/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.segment.analytics.android.integrations/amplitude)
[![Javadocs](http://javadoc-badge.appspot.com/com.segment.analytics.android.integrations/amplitude.svg?label=javadoc)](http://javadoc-badge.appspot.com/com.segment.analytics.android.integrations/amplitude)

Amplitude integration for [analytics-android](https://github.com/segmentio/analytics-android).

## Installation

To install the Segment-Amplitude integration, simply add this line to your gradle file:

```
compile 'com.segment.analytics.android.integrations:amplitude:+'
```

## Usage

After adding the dependency, you must register the integration with our SDK.  To do this, import the Amplitude integration:


```
import com.segment.analytics.android.integrations.amplitude.AmplitudeIntegration;

```

And add the following line:

```
analytics = new Analytics.Builder(this, "write_key")
                .use(AmplitudeIntegration.FACTORY)
                .build();
```

Please see [our documentation](https://segment.com/docs/integrations/amplitude/) for more information.

## Repository
- [Snapshots](https://oss.sonatype.org/content/repositories/snapshots/com/segment/analytics/android/integrations/amplitude/)
- [Releases](https://oss.sonatype.org/content/repositories/releases/com/segment/analytics/android/integrations/amplitude/)

## Releasing
CircleCI is configured to release the artifacts when a new tag is created. Snapshot builds are created and uploaded
for each commit in master.

When you are working in a new release, change the version to `<new-version>-SNAPSHOT` in `gradle.properties`. After you
are done, push your changes to master (it will upload the SNAPSHOT version) and then create a tag with the version (it
will release and promote the new version).

### Errors promoting
Go to [Sonatype](https://oss.sonatype.org/#stagingRepositories) to check if the previous staging was not closed
properly, or if the signature was invalid. Segment's staging repositories are `comsegment-xxxxx`.

### Verify signature
You can get Segment's public key from:
- [GnuPG](http://keys.gnupg.net:11371/pks/lookup?search=tools%2Bandroid%40segment.com&fingerprint=on&op=index)
- [PGP MIT](http://pgp.mit.edu/pks/lookup?search=tools%2Bandroid%40segment.com&op=index)

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
