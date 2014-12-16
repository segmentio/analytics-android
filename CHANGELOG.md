Version 2.5.0 (In Progress)
===========================
* Improvement: Remove dependency on Tape. We won't add it back again for this major version.
* Fix: OOM when buffering too many messages in the queue
* Improvement: Stream payloads to URLConnection
* Fix: Each project will not have it's own ProjectSettings and Traits Cache
* Improvement: Reject payloads over 450kb
* Improvement: Automatically batch messages in 500kb
* Improvement: If the disk queue grows to over 1000, old messages will be dropped
* Improvement: Print important payload information in logs

Version 2.4.6 (December 5th, 2014)
==================================
* Fix: Catch serialization errors better
* Improvement: Better logging messages
* Fix: Add a default category for Google Analytics events
* Improvement: Follow App Links in Mixpanel

Version 2.4.5 (December 3rd, 2014)
==================================
* Improvement: Eagerly clear references to OnIntegrationReadyListener

Version 2.4.4 (December 2nd, 2014)
==================================
* Fix: Better error handling for unexpected deserialization
* Improvement: Update Play Services Dependencies
* Fix: Update Javadocs

Version 2.4.3 (November 25th, 2014)
===================================
* New: Kahuna Integration

Version 2.4.2 (November 20th, 2014)
===================================
* Improvement: Updating the Crittercism SDK version from 4.5.3 to 5.0.3
* Fix: Use `newId` parameter from `alias` method
* Fix: Value should be numeric type

Version 2.4.1 (November 11th, 2014)
===================================
* Fix: Check if Error Input Stream is null before trying to read it

Version 2.4.0 (November 10th, 2014)
====================================
* Breaking: Add explicit dependency on [Tape](https://github.com/square/tape)
* Breaking: `alias` method accepts `(previousId, newId)` instead of `(newId, previousId)` (arguments reversed).
* New: Deprecate `logging` method on builder for `debugging`
* New: `debugging` value will now be forwarded to integrations so they can enable it as we ll
* New: `Analytics.setSingletonInstance` allows setting the global Analytics instance returned from `Analytics.with`
* Improvement: If integrations fail to load (e.g. due to missing permissions), we'll now update internal flags so server can send the event instead
* Improvement: Explicitly specify UTF-8 charset for serializing/deserializing payloads
* Improvement: Fallback to memory queue if disk fails to load
* Improvement: Accept `httpFallback` parameter for AppsFlyer
* Fix: Don't call `Leanplum#forceContentUpdate` when flushing

Version 2.3.0 (N/A)
===================
* Accidentally skipped!

Version 2.2.0 (October 29th, 2014)
====================================
* Breaking: Updated Localytics integration (Localytics now returns LocalyticsAmpSession for listeners)
* New: `flushInterval` setting, that flushes any events in the queue at a specified interval
* New: Added AppsFlyer integration
* New: Collect advertisingId as per spec
* Fix: Verify C2DM permission for Leanplum
* Fix: Synchronize maps across threads
* Fix: Ignore `siteSpeedSamplingRate` for Google Analytics

Version 2.1.10 (October 26th, 2014)
====================================
* Fix: Update `context.device.id` and `context.referrer.id` to match spec

Version 2.1.9 (October 25th, 2014)
====================================
* Fix: Correctly use size of disk queue

Version 2.1.8 (October 24th, 2014)
====================================
* New: Added Leanplum Integration
* Improvement: Updated Google Analytics

Version 2.1.7 (October 23rd, 2014)
====================================
* Improvement: Make Tape classes non-public

Version 2.1.6 (October 21th, 2014)
====================================
* New: Added `context.setDeviceToken()`, for push notifications e.g. Outbound.io

Version 2.1.5 (October 21th, 2014)
====================================
* Fix: Cache settings each time we request it
* Fix: Avoid race condition when calling tracking methods on bundled integrations

Version 2.1.4 (October 20th, 2014)
====================================
* Fix: Initialize bundled integrations on main thread

Version 2.1.3 (October 19th, 2014)
====================================
* Improvement: Better logging for serialization errors
* Fix: Make `Product` class public
* Improvement: Add timezone in context

Version 2.1.2 (October 17th, 2014)
====================================
* Fix: Catch file system errors better

Version 2.1.1 (October 16th, 2014)
====================================
* API Change: Make logging state final
* Improvement: Explicitly create disk queue directory

Version 2.1.0 (October 16th, 2014)
====================================
* API Change: Hide lifecycle callbacks. We register this automatically so user's shouldn't use these.
* New: Added Integration Listener API
* Improvement: Better messages in logging
* Improvement: Integrations are now executed synchronously
* Fix: Forward `Activity Started` lifecycle event to integrations
* Fix: Convert seconds to milliseconds for Flurry

Version 2.0.4 (October 10th, 2014)
====================================
* Improvement: Reduce size of batched payloads
* New: Added convenience methods for special properties
* Fix: Forward `analytics.flush()` to Google Analytics

Version 2.0.3 (October 4th, 2014)
====================================
* Improvement: Better detection of ecommerce events for bundled integrations
* Fix: Correctly pass in activity for lifecycle events
* Fix: Clean up logs for integrations that are not available
* Fix: use label instead of `applicationInfo.name` for context

Version 2.0.0 (September 29th, 2014)
====================================

* New architecture distances client from main thread even more
* Improves logging with consistent format : `THREAD | VERB | ID | EXTRAS`
* Queue events for bundled integrations while initializing
* Use Tape for underlying disk queue
* Simpler and more consistent API
* Allow creation of multiple clients
