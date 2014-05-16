
0.6.13 / May 16, 2014
=================
* updating to Countly SDK 13.10, fixes #31

0.6.12 / May 8, 2014
=================
* updating Mixpanel to 4.2.0

0.6.11 / May 6, 2014
=================
* fixing bug that prevented Flurry from being ready because `ready(..)` was called in `integration.onActivityStart`

0.6.10 / April 27, 2014
=================
* improving IntegrationManager logging
* update Localytics to `2.19.0`

0.6.9 / April 27, 2014
=================
* updating Countly SDK to fix single quote SQL bug, fixes #32

0.6.8 / April 27, 2014
=================
* integration.onActivityX can only be called after integration.onActivityCreate has been called. Fixes #33
* removing `Omniture` bundled SDK in favor of the server-side integration

0.6.7 / April 27, 2014
=================
* separating flush and request into two threads, one for processing flushes one at a time, and one for processing requests one at a time. This eliminates the race condition between getting items from the database, sending requests, and then removing the items on the same thread. Now a flush operation encompasses all three actions, and only one flush happens at any point in time.
* improving logging around the flush process
* adding a `DuplicateTest`
* renaming occurences of `provider` to `integration`
* turning `TestCase` generators into methods instead of static objects. This allows each generated test case to have a unique `requestId`.

0.6.6 / March 26, 2014
=================
* rolling back to older Tapstream, fixes #29

0.6.5 / March 25, 2014
=================
* updating to Count.ly [commit](https://github.com/Countly/countly-sdk-android/commit/6a2f6b4e92faf80acd7c1a6ce3ec99c5336135b5)
* guards against unfinalized db statements when multiple payload database are created and are unsynchronized, fixes #25
* updating to Viewed X Screen new spec, fixes #28
* race condition fix: updated flushing thread to remove from the db first, try sending, and re-queue only if it didn't work, fixes #24

0.6.4 / March 25, 2014
=================
* updating to Crittercism [4.4.0](https://app.crittercism.com/downloads/release_notes/android/4.4.0)
* updating to Amplitude version to [commit](https://github.com/amplitude/Amplitude-Android/commit/51a69da7a8ff49848985db511e72814359e1c9f0)
* updating to Flurry version [3.4.0](http://support.flurry.com/index.php?title=Analytics/Code/ReleaseNotes/Android)
* updating to Bugsnag version [2.1.1](https://bugsnag.com/docs/notifiers/android)
* updating to Google Analytics SDK version [3.01](https://developers.google.com/analytics/devguides/collection/android/changelog#changelog)
* updating to Quantcast version [1.2.0](https://github.com/quantcast/android-measurement/blob/master/CHANGELOG.md)
* updating to Tapstream version [2.6.2](https://tapstream.com/sdk/android/)

0.6.3 / March 25, 2014
=================
* updating to Mixpanel SDK [4.0.1](https://github.com/mixpanel/mixpanel-android/commit/6830db80a7ef3735c087c2c30772a99c65b28d14)

0.6.2 / February 21, 2014
=================
* set sessionId initially
* add `analytics_send_location` option

0.6.1 / February 20, 2014
=================
* adding `device.model`
* force settings `fetch` on application start

0.6.0 / February 19, 2014
=================
* renaming `EventProperties` to `Props`
* deprecating `EventProperties`
* adding `sessionId` to each message
* `screen` is now sent to the server-side API
* added `group` method call to the API

0.5.0 / February 17, 2014
=================
* renaming `Provider` to `Integration`
* pulling in https://github.com/segmentio/analytics-android/pull/16
* updating `LocalyticsIntegration`
* adding `activityPause` and `activityResume` to the API

0.4.5 / January 8, 2014
=================
* downgrading to Mixpanel 3.3.4 SDK due to v4.0.0 being [marked beta post release](https://github.com/mixpanel/mixpanel-android/commit/98e30e414634df80a90650d183a5f7a131a17c74)

0.4.4 / January 8, 2014
=================
* upgrading to Mixpanel 4.0 SDK

0.4.3 / January 8, 2014
=================
* Updated initialized state to be `volatile` to prevent two activities initializing the Analytics client simultaneously
* Added `requestId` to payloads
* Added logging statements on enqueue and sending
* Rename `secret` to `writeKey`
* Added `requestTimestamp` to batch
* Added `libraryVersion`

0.4.2 / December 17, 2013
=================
* Updated Localytics bundled SDK

0.4.1 / November 25, 2013
=================
* Settings fetching moved to its own thread

0.4.0 / November 20, 2013
=================
* Location info now only read from network
* Providers check whether they have permission to run
* Added `optOut`
* Added Quantcast and tests
* Added Tapsteam and tests
* Updated Amplitude, Bugsnag, Crittercism, Google Analytics, Flurry, and Mixpanel bundled SDKs

0.3.3 / November 11, 2013
=================
* `analytics.reset` is now static

0.3.2 / August 15, 2013
=================
* Updated Count.ly bundled SDK
* Updated Localytics bundled SDK
* Updated Amplitude bundled SDK
* Updated bugsnag bundled SDK
* Updated Crittercism bundled SDK
* Updated Flurry bundled SDK
* Updated Google Analytics bundled SDK
* Updated Mixpanel bundled SDK

0.3.2 / July 7, 2013
=================
* Started sending entity as bytes in BasicRequester to fix UTF8 issues

0.3.1 / May 30, 2013
=================
* Fix NullPointerException in SettingsCache

0.3.0 / May 29, 2013
=================
* Removing the track(userId, ..) overrides such that only identify needs to provide a userId
* Adding SQL Db fix to avoid locking issue on count

0.2.3 / May 21, 2013
=================
* Adding parameter mapping to Mixpanel

0.2.2 / May 20, 2013
=================
* Added screen method to public API
* Added screen implementation for GA, Omniture, Localytics, Flurry, Mixpanel, Amplitude and Countly

0.2.1 / May 10, 2013
=================
* Fixed synchronous settings download at initialize
* Fixing Mixpanel provider to not identify on track
* Adding Mixpanel alias'd user test
* Added Omniture

0.2.0 / May 8, 2013
=================
* Adding ability to request integration settings from Segment.io
* Adding Amplitude, BugSnag, Countly, Crittercism, Flurry, Google Analytics, Localytics, and Mixpanel bundled providers and associated libraries
* Adding context.providers support for identifies, aliases, and tracks
* Added stopwatch timing for bundled provider operations

0.1.1 / April 23, 2013
=================
* Moving context.build out of context.device
* Fixing global context not propagating bug

0.1.0 / April 12, 2013
=================
* API stabilized
* Repo going public
