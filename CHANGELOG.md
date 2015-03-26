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
