
Version 3.2.1 (Sep 23rd, 2015)
==================

  * updating Amplitude SDK to v2.0.4
  * update localytics version to 3.4.2
  * Updating MoEngage SDK version
  * Handle case when context.device is null

Version 3.2.0 (Sep 21st, 2015)
==============================

  * Use US ISO 8601 timestamps
  * Add 'collectDeviceId' Builder option.
    AnonymousId is used in place of device identifiers if enabled.
  * UXCam lib updated to v2.0.6

Version 3.1.8 (Sep 11th, 2015)
==============================

  * Add aliases for revenue keys
  * Send revenue to AppsFlyer if one is set
  * Improve GA integration, most notably - add support for custom dimensions and metrics
  * Fix toString implementation

Version 3.1.7 (Aug 27th, 2015)
===============================
* Fix: Edge case when using `Options.setIntegration(name, true)` for a bundled integration would cause events to be sent twice, once server side and once client side.
* Enhancement: Fail early if multiple conflicting Analytics instances are created. Two analytics instances will conflict if they have the same tag (which is automatically generated from the write key if it is not explicilty provided).
* Enhancement: Update Amplitude SDK to 2.0.2
* Enhancement: Update Kahuna SDK to 2.0.3
* Enhancement: Update MoEngage SDK to 5.2.21
* Enhancement: Fail early if messages are enqueued after shutting down an Analytics instance.
* Fix: Race where activitiy lifecycle events would be delivered before initialization is complete.
* Fix: Properly handle `null` in `JSONObject`.
* Enhancement: Dump more output to address issues #263, #321, #309.

Version 3.1.6 (July 20th, 2015)
===============================
* Feature: Add MoEngage integration

Version 3.1.5 (July 13th, 2015)
===============================
* Feature: Add Apptimize Root integration
* Enhancement: Track LTV with Localytics
* Fix: Localytics ready callback
* Enhancement: Add API to presize Properties and Traits

Version 3.1.4 (June 22, 2015)
=============================
* Enhancement: Update Amplitude SDK
* Enhancement: Update Apptimize SDK
* Enhancement: Update Bugsnag SDK
* Enhancement: Update Crittercism SDK
* Enhancement: Update Flurry SDK
* Enhancement: Update Google Analytics SDK
* Enhancement: Update Leanplum SDK
* Enhancement: Update Quantcast SDK
* Enhancement: Update Tapstream SDK

Version 3.1.3 (May 27th, 2015)
==============================
* Enhancement: Update Localytics SDK and integration with support for custom dimensions.

Version 3.1.2 (May 13th, 2015)
==============================
* Fix: Track increment events for Mixpanel
* Enhancement: Improved flushing behaviour. If you were manually setting a `    flushQueueSize`, make sure it is under 250.
* Enhancement: Update Leanplum SDK

Version 3.1.1 (May 11th, 2015)
===============================
* Feature: Updated Kahuna Integration with E-Commerce Spec
* Enhancement: Update Apptimize SDK
* Enhancement: Update Amplitude SDK
* Enhancement: Print full error messages for 400 Responses
* Fix: NPE in Google Analytics Integration

Version 3.1.0 (April 21st, 2015)
===============================
* Feature: Add reset method
* Feature: Add Apptimize Integration
* Feature: Add APIs to send custom options to integrations
* Feature: Add ability to set a custom connection factory (Beta)
* Fix: Attach JAR for core artifact
* Enhancement: Update Crittercism Integration
* Enhancement: Update Amplitude Integration
* Fix: Send Kahuna quantity and revenue for track calls
* Fix: Guard against negative file length in QueueFile. This is a potential fix for [#172](https://github.com/segmentio/analytics-android/issues/172)
* Enhancement: Log when an event is created
* Enhancement: Log when no integrations are enabled

Version 3.0.2 (March 20th, 2015)
===============================
* Enhancement: Update Kahuna integration
* Enhancement: Update payload limits
* Enhancement: Fix some context fields

Version 3.0.1 (March 5th, 2015)
===============================
* Fix: AAR packaging for error 'module depends on libraries but is not a library itself'
* Fix: Potential NPE for disabled integrations
* Fix: Correctly forward `createdAt` to Mixpanel
* Fix: Callback for Quantcast integration
* Fix: Update Taplytics dependency
* Enhancement: Collect context data as per our spec
* Enhancement: Update Flurry dependency
* Enhancement: Support `increment` events for Mixpanel

Version 3.0.0 (Feb 24th, 2015)
==================================
* Feature: Add support for Tracking Plan
* Feature: Add API's to pass in executors for network calls
* Feature: New type safe integration callback API
* Feature: Add API's to control logging behaviour for bundled integrations
* Enhancement: Remove integration adapters from `analytics-core` module
* Enhancement: Update Leanplum dependency
* Enhancement: Update Mixpanel dependency
* Fix: Correctly convert special traits for Mixpanel 
* Fix: Alias method implementation and docs
