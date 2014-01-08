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
