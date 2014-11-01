Version 2.3.0 (In progress)
====================================
* Breaking: Add explicit dependency on [Tape](https://github.com/square/tape)
* New: Deprecate `logging` for `debugging`
* Fix: If integrations fail to load (e.g. missing permissions), update flags so server can send the event
* New: (WIP) Debugging will now be forwarded to integrations so they can enable it too

Version 2.2.0 (October 29th, 2014)
====================================
* Breaking: Updated Localytics integration (Breaking, Localytics now returns LocalyticsAmpSession)
* New: `flushInterval` setting, that flushes any events in the queue at a specified interval
* Fix: Synchronize maps across threads
* New: Added AppsFlyer integration
* Fix: More permissions verification for Leanplum
* Fix: Ignore `siteSpeedSamplingRate` for Google Analytics
* New: Collect advertisingId as per spec

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