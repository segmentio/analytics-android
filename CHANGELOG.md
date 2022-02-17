Changelog
=========

Version 4.10.4 (Feb 17, 2022)
============================
* [Fix](https://github.com/segmentio/analytics-android/pull/791): fix ANR issue caused by MediaDRM api

Version 4.10.3 (Jan 25, 2022)
============================
* [New](https://github.com/segmentio/analytics-android/pull/786): deprecate jcenter and bintray

Version 4.10.2 (Jan 18, 2022)
============================
* [New](https://github.com/segmentio/analytics-android/pull/784): fix get unique id NPE

Version 4.10.1 (Jan 11, 2022)
============================
* [New](https://github.com/segmentio/analytics-android/pull/781): Address violation of retrieving unsettable device
* [New](https://github.com/segmentio/analytics-android/pull/774): update kotlin docs to beta

Version 4.10.0 (July 8, 2021)
============================
* [New](https://github.com/segmentio/analytics-android/pull/769): Add referrer property when tracking Deep Link Opened

Version 4.9.4 (May 3, 2021)
============================
* [Fix](https://github.com/segmentio/analytics-android/pull/761): Map product_id for a Product
* [Fix](https://github.com/segmentio/analytics-android/pull/767): Safely handle bad URIs when tracking deep links

Version 4.9.3 (Mar 29, 2021)
============================
* [Fix](https://github.com/segmentio/analytics-android/pull/755): Check Float NaN and Infinity
* [Fix](https://github.com/segmentio/analytics-android/pull/758): Fix Incorrect Timestamps

Version 4.9.2 (Mar 22, 2021)
============================
* [New](https://github.com/segmentio/analytics-android/pull/734): Make new lifecycle methods opt-out
* [New](https://github.com/segmentio/analytics-android/pull/731): Kotlin Demo App
* [New](https://github.com/segmentio/analytics-android/pull/739): Custom apiHost for Segment.IO and support multi-regional Segment
* [Fix](https://github.com/segmentio/analytics-android/pull/750): Add NonNull annotations to params for analytics constructor
* [Fix](https://github.com/segmentio/analytics-android/pull/749): Retry hasPermission in case of failure due to package manager being inactive
* [Fix](https://github.com/segmentio/analytics-android/pull/752): Marshal NaN and other special numbers to 0.0 and not produce malformed JSON

Version 4.9.0 (Oct 15, 2020)
============================
* [New](https://github.com/segmentio/analytics-android/pull/707): Remove the ability to natively report attribution information via Segment integrations
* [New](https://github.com/segmentio/analytics-android/pull/727): Edge Function Support
* [New](https://github.com/segmentio/analytics-android/pull/715): Add a Webhook Integration for viewing payloads
* [New](https://github.com/segmentio/analytics-android/pull/728): Enable faster project settings refresh when debugging enabled

Version 4.8.2 (Jul 30, 2020)
============================
* [Fix](https://github.com/segmentio/analytics-android/pull/686): Generate timestamps at call site rather than asynchronously

Version 4.8.0 (Jul 13, 2020)
============================

* [Fix](https://github.com/segmentio/analytics-android/pull/668): Fix: Proguard/R8 configuration for createValueMap
* [Fix](https://github.com/segmentio/analytics-android/pull/670): handle RuntimeException when calling `getActivityInfo`
* [New](https://github.com/segmentio/analytics-android/pull/671): Experimental Nanosecond timestamps

Version 4.7.1 (Jun 8, 2020)
============================

* [Fix](https://github.com/segmentio/analytics-android/pull/667): Use AndroidX lifecycle functions for App Fore/Backgrounded
* [Fix](https://github.com/segmentio/analytics-android/pull/666): Fix alias not overridden by cached value
* [Fix](https://github.com/segmentio/analytics-android/pull/663): Catch null intent data

Version 4.7.0 (Jun 1, 2020)
============================

* [New](https://github.com/segmentio/analytics-android/pull/655): Add type to device context
* [Fix](https://github.com/segmentio/analytics-android/pull/665): Prevent Global Context modification for one-time op
* [New](https://github.com/segmentio/analytics-android/pull/664): Add support for Destination Middleware

Version 4.6.0 (May 21, 2020)
============================

* [New](https://github.com/segmentio/analytics-android/pull/662): Add DefaultProjectSettings in Analytics.Builder
* [Fix](https://github.com/segmentio/analytics-android/pull/659): Test coverage fix after previous merge to master
* [New](https://github.com/segmentio/analytics-android/pull/658): Update project to AndroidX
* [Fix](https://github.com/segmentio/analytics-android/pull/656): Set build property to String

Version 4.5.0 (Apr 22, 2020)
============================

* Promoting 4.5.x to stable release.  This includes all changes since 4.3.1.
* [Fix](https://github.com/fubotv/segment-analytics-android/pull/1): Check integration settings for null

Version 4.5.0-beta.2 (Mar 23, 2020)
===================================

 * [New](https://github.com/segmentio/analytics-android/pull/632): Make crypto abstract methods public
 * [Fix](https://github.com/segmentio/analytics-android/pull/648): Preserve BUILD and VERSION keys on reset() calls
 * [Fix](https://github.com/segmentio/analytics-android/pull/646): Resolves ArrayIndexOutOfBoundsException crash (#586)
 * [Fix](https://github.com/segmentio/analytics-android/pull/644): Updated documentation on deep copy and threading
 * [Fix](https://github.com/segmentio/analytics-android/pull/642): Double check that input stream is closed

Version 4.5.0-beta.1 (Dec 16, 2019)
===================================

 * [New](https://github.com/segmentio/analytics-android/pull/637): Track "Deep Link Opened" on activity creation
 
Version 4.5.0-beta.0 (July 25, 2019)
===================================

 * [New](https://github.com/segmentio/analytics-android/pull/620): Add Application Open and Application Backgrounded Events

Version 4.4.0-beta1 (Nov 28, 2018)
==================================

 * [Fix](https://github.com/segmentio/analytics-android/pull/574): Send application build number as a string to match our documentation and other libraries. If you were relying on this field being a number, you may need to update reports that rely on the now deprecated behaviour. You can also override this behaviour and keep the deprecated behaviour by supplying the `build` field manually.

 ```java
 PackageManager packageManager = context.getPackageManager();
 PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
 Map<String, Object> app = new LinkedHashMap<>();

 // Put the build as an integer instead of string.
 app.put("build", packageInfo.versionCode);

 // Put other application context fields that the library collects. Not shown in this example, but you should filter out any null fields.
 app.put("name", packageInfo.applicationInfo.loadLabel(packageManager));
 app.put("version", packageInfo.versionName);
 app.put("namespace", packageInfo.packageName);

 analytics.getAnalyticsContext().put("app", app);
 ```

 * [New](https://github.com/segmentio/analytics-android/pull/595): Add `Options#putContext` method. This allows you to send context fields for a single event. This can also override default context fields for a single event.

 ```java
 analytics.track("My Event", properties, new Options().putContext("custom_context_field", true));
 ```

 * [New](https://github.com/segmentio/analytics-android/pull/582): Send a custom user agent on HTTP requests originating the library. The new user-agent will be formatted as `analytics-android/{version}`.

 * [Fix](https://github.com/segmentio/analytics-android/pull/578): Retry network requests when server responds with a HTTP 429 response code.

 * [Fix](https://github.com/segmentio/analytics-android/pull/575): Update maximum message limit to be 32kb to match our API limits.

 * [Fix](https://github.com/segmentio/analytics-android/pull/573): Fix race condition that could cause events to pick up user IDs after the event was originally recorded.

Version 4.3.1 (Nov 28, 2017)
=============================

 * [Fix](https://github.com/segmentio/analytics-android/pull/547): Rename `Properties#putSubtotal` helper to `Properties#subtotal`.
 * [New](https://github.com/segmentio/analytics-android/pull/550): Send disabled events to Segment's API so that it can be surfaced in the debugger. These events will not be sent to any destination.
 * [New](https://github.com/segmentio/analytics-android/pull/552): Add support for schema defaults.
 * [Fix](https://github.com/segmentio/analytics-android/pull/537): Show better errors for unexpected 3xx response codes.


Version 4.3.0 (Oct 2nd, 2017)
=============================

 * Promoting RC to stable release. This includes all the improvements from 4.3.0-RC1 and 4.3.0-RC2.


Version 4.3.0-RC2 (July 25th, 2017)
===================================

 * [New](https://github.com/segmentio/analytics-android/pull/536): Look up Advertising ID for Amazon Fire devices.
 * [Fix](https://github.com/segmentio/analytics-android/pull/534): Attribution tracking using mobile service should be false by default.


Version 4.3.0-RC1 (May 10th, 2017)
==================================
 * [New](https://github.com/segmentio/analytics-android/pull/515): Add Middlewares.
 * [Fix](https://github.com/segmentio/analytics-android/pull/524): Unregister Application lifecycle callbacks on shutdown.
 * [Fix](https://github.com/segmentio/analytics-android/pull/499): Record `ms` precision in timestamps.
 * [Fix](https://github.com/segmentio/analytics-android/pull/508): Support serialization of Primitive arrays.


Version 4.2.6 (January 31st, 2017)
==================================

 * [Fix](https://github.com/segmentio/analytics-android/pull/495): Update Cartographer. This fixes an issue where sending custom values sent as arrays would not be serialized correctly.
 * [Fix](https://github.com/segmentio/analytics-android/pull/494): Make DateFormat access thread safe. This fixes an issue where generated timestamps could be sometimes be malformed and not conform to the ISO 8601 standard.


Version 4.2.5 (January 2nd, 2017)
==================================

 * [Fix](https://github.com/segmentio/analytics-android/pull/487/commits/8649050b4b7b74be17fc7b7e4ec0add7362325fd): Using `Properties#putProducts` was stored as an array instead of a list, and not serialized correctly. This caused it to be usable by Segment and server side integrations. If you're stuck on a previous version for some reason, you can manually store it as a list:

 ```java
 List<Product> products = new ArrayList<>();
 products.add(new Product("foo", "bar", 10));
 // add other products to this list.

 Properties properties = new Properties();
 properties.put("products", products);
 ```


Version 4.2.4 (November 14th, 2016)
==================================

 * [Fix](https://github.com/segmentio/analytics-android/pull/484): Version 4.2.2 introduced a change where a default application wide cache was being installed for HTTPURLConnection requests. If you were not using an HTTP cache already or relying on this behaviour, this may have resulted in unintended caching behaviour for your application. This fix returns to the behaviour before 4.2.2, where this library does not install a cache. You may continue to choose to install an application level cache for HTTPURLConnection if you wish.


Version 4.2.3 (November 4th, 2016)
==================================

  * [Improvement]: Update CDN hostname from `cdn.segment.com` to `cdn-settings.segment.com`. This endpoint has been added to improve performance for mobile clients.


Version 4.2.2 (October 13th, 2016)
====================================

  * [Fix](https://github.com/segmentio/analytics-android/pull/479): Rely on HTTP cache for caching settings responses. This fixes a regression introduced in [version 4.1.4](https://github.com/segmentio/analytics-android/pull/448), where cached settings responses were not being used, and would always be fetched from the network.


Version 4.2.1 (October 7th, 2016)
====================================

  * [Fix](https://github.com/segmentio/analytics-android/pull/476): Use Application Opened instead of Application Started.
  * [Improvement](https://github.com/segmentio/analytics-android/pull/475): Update Google Play Services for Android wear module.

Version 4.2.0 (September 19th, 2016)
====================================

  * [Improvement](https://github.com/segmentio/analytics-android/pull/464): Reduce synthetic accessor methods.
  * [Fix](https://github.com/segmentio/analytics-android/pull/466): Handle `null` values in maps.
  * [New](https://github.com/segmentio/analytics-android/pull/467): Add the ability for the SDK to natively report attribution information via Segment integrations enabled for your project, without needing to bundle their SDKs. Attribution information is sent as a track call as documented in the [mobile lifecycle spec](https://segment.com/docs/spec/mobile/#install-attributed).

```java
Analytics analytics = new Analytics.Builder(context, writeKey)
    .trackAttributionInformation()
    .build();
```

Version 4.1.6 (August 9th, 2016)
================================

  * Improvement: Add more logging when collecting advertising ID.

Version 4.1.5 (July 10th, 2016)
===============================

  * Improvement: Add more protection against growing disk queue to over 2GB.

Version 4.1.4 (Jun 6th, 2016)
=============================

  * New: Add opt out method in the library. This will stop sending any events to all integrations for the device.

```java
analytics.optOut(true);
```

  * Fix: Use Application Opened instead of Application Started.
  * Improvement: gzip HTTP request body.
  * Fix: Guard against possible ArrayIndexOutOfBoundsException.

Version 4.1.3 (May 31st, 2016)
==============================

  * Fix: Early Return when queue error occurs
  * Fix: Don't block user thread while waiting for advertising ID
  * Fix: Add Memory Fallback and be lenient about Queue Errors.

Version 4.1.2 (May 24th, 2016)
==============================

  * Fix: Wait for advertising ID to be ready before enqueuing events.
  * New: Instrument automatic screen tracking. Enable this during initialization.

```java
Analytics analytics = new Analytics.Builder(context, writeKey)
    .recordScreenViews()
    .build();
```

Version 4.1.1 (May 10th, 2016)
==============================

  * Fix: Handling how advertising information is collected. Previously, if `isLimitAdTracking` was true, the library would incorrectly record it as `adTrackingEnabled: true`, when it should have been `adTrackingEnabled: false`. A server side fix has been deployed to automatically correct this. However, we still recommend customers update to the latest version.

Version 4.1.0 (May 9th, 2016)
==============================

  * New: Instrument automatic application lifecycle event tracking. Enable this during initialization.

```java
Analytics analytics = new Analytics.Builder(context, writeKey)
    .trackApplicationLifecycleEvents()
    .build();
```

  * Fix: Dump raw QueueFile data to track #321.

Version 4.0.9 (Mar 29th, 2016)
==============================

  * Fix: Fix for possible race condition when mutating integration options on multiple threads.

Version 4.0.8 (Mar 15th, 2016)
==============================

  * Fix: Another update to `Options` handling, so that it can *not* override the tracking plan if an event is disabled.

Version 4.0.7 (Mar 14th, 2016)
==============================

  * Fix: Handle possible NPE introduced in 4.0.6.

Version 4.0.6 (Mar 14th, 2016)
==============================

  * Fix: Update `Options` handling so that it overrides the tracking plan.

Version 4.0.5 (Mar 10th, 2016)
==============================

  * Fix: Correctly disable events from being sent server side for bundled integrations.

Version 4.0.4 (Feb 4th, 2016)
==============================

  * New: Improve logging information for QueueFile.

Version 4.0.3 (Dec 29th, 2015)
==============================

  * Fix: Collect advertising ID based on whether the `AdvertisingIdClient` is available rather than `GoogleAnalytics`.

Version 4.0.2 (Dec 11th, 2015)
==============================

  * Fix: Actually fix NPE when device is offline duNring initialization.

Version 4.0.1 (Dec 7th, 2015)
==============================

  * Fix: NPE when device is offline during initialization.

Version 4.0.0 (Nov 24th, 2015)
==============================

  * Deprecates `analytics-core` artifact. This is now renamed into the `analytics` artifact.
  * Bundling integrations is now more explicit. It takes 2 steps:

Add the integration dependencies.
```
  compile('com.segment.analytics.android.integrations:google-analytics:1.0.0') {
    transitive = true
  }
  compile('io.branch.segment.analytics.android.integrations:library:1.0.0-RELEASE') {
    transitive = true
  }
  ...
```

Register them in your builder when you initialize the SDK.
```
Analytics analytics = new Analytics.Builder(context, writeKey)
  .use(GoogleAnalyticsIntegration.FACTORY)
  .use(BranchIntegration.FACTORY)
  ...
  .build();
```

Version 3.4.0 (Oct 20th, 2015)
==============================

  * Updating Amplitude SDK to 2.2.0
  * New Logging API for integrations
     * Integrations will log the exact method call made by them, which makes it easier to see
       exactly how a Segment call is translated for the end tool.
       Currently only done for a few tools (Mixpanel, Google Analytics, Flurry, Localytics),
       but will be added for more tools.
     * LogLevel.BASIC is now deprecated. Use LogLevel.DEBUG instead.
     * Logging behaviour with regards to bundled integrations has changed. See the JavaDocs
       for more details.

  * Update MoEngage SDK to 5.3.10
  * Fix bug with MoEngage integration when trying to track events outside of an activity.

Version 3.3.3 (Oct 10th, 2015)
=============================

  * Update Tapstream

Version 3.3.2 (Oct 7th, 2015)
============================

  * Attribute that events are made from the Segment SDK to Kahuna.

Version 3.3.1 (Oct 6th, 2015)
============================

  * Update Mixpanel SDK to 4.6.4
  * Update Amplitude SDK to 2.1.0

Version 3.3.0 (Oct 4th, 2015)
=============================

  * Send transaction data correctly to Google Analytics
  * Update Play Services dependencies to 8.1.0
  * Update Support Library dependency to 23.0.1

Version 3.2.1 (Sep 23rd, 2015)
=============================

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
