0.3.1 / 2013-05-30
=================
* Fix NullPointerException in SettingsCache

0.3.0 / 2013-05-29
=================
* Removing the track(userId, ..) overrides such that only identify needs to provide a userId
* Adding SQL Db fix to avoid locking issue on count

0.2.3 / 2013-05-21
=================
* Adding parameter mapping to Mixpanel

0.2.2 / 2013-05-20
=================
* Added screen method to public API
* Added screen implementation for GA, Omniture, Localytics, Flurry, Mixpanel, Amplitude and Countly

0.2.1 / 2013-05-10
=================
* Fixed synchronous settings download at initialize
* Fixing Mixpanel provider to not identify on track
* Adding Mixpanel alias'd user test
* Added Omniture

0.2.0 / 2013-05-08
=================
* Adding ability to request integration settings from Segment.io
* Adding Amplitude, BugSnag, Countly, Crittercism, Flurry, Google Analytics, Localytics, and Mixpanel bundled providers and associated libraries
* Adding context.providers support for identifies, aliases, and tracks
* Added stopwatch timing for bundled provider operations

0.1.1 / 2013-04-23
=================
* Moving context.build out of context.device
* Fixing global context not propagating bug

0.1.0 / 2013-04-12
=================
* API stabilized
* Repo going public
