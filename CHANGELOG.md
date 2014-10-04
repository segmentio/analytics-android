Version 2.0.3 (September 29th, 2014)
====================================
* Improvement: Better detection of ecommerce events for bundled integrations
* Fix: Correctly pass in activity for lifecycle events
* Fix: Clean up logs for integrations that are not available
* Fix: use label instead of `applicationInfo.name` for context

Version 2.0.0 (September 29th, 2014)
====================================

* New architecture distances client from main thread even more
* Improves logging with consistent format `THREAD | VERB | ID | EXTRAS`
* Queue events for bundled integrations while initializing
* Use Tape for undelying disk queue
* Simpler and more consistent API
* Allow creation of multiple clients