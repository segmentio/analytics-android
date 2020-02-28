3.0.2 / 2019-09-16
==================

  * [DEST-929] Update Android Ampltiude SDK
    * Update Amplitude SDK to 2.23.1

3.0.1 / 2019-01-10
==================

  * [FCD-207] Implement groupIdentify to track group properties (#50)
    * Update Amplitude SDK to 2.21.0
    * Implement groupIdentify

3.0.0 / 2018-09-27
==================

  * [PLATFORM-3118] Update all (#49)
    * Update Amplitude's SDK
    * Update Analytics
    * Update test dependencies
    * Update target SDK to 28

2.0.2 / 2018-09-20
==================

  * Add signature to CI

2.0.2-beta / 2018-09-18
=======================

  * Update README with links to the repos
  * Add publish plugin (#48)

2.0.1 / 2018-09-18
==================

  * Add tag verification
  * Persist the workspace
  * Remove old configuration
  * Add publish steps
  * Add CircleCI 2.0 base config
  * Update Tests (#43)
  * Remove checkstyle.xml (#42)
  * Adjust test formatting
  * Simplify tests for revenue and total
  * Minimal formatting changes
  * Update tests to use isEqualToComparingFieldByFieldRecursively
  * Prepare next development version.

Version 2.0.0 (November 1, 2017)
================================
*(Supports analytics-android 4.0.+ and Amplitude 2.15.+)*

  * New: Adds support for `add` and `setOnce` for traits
  * New: `trackAllPagesV2` setting; triggers `Loaded a Screen`
  * New: Supports `enableLocationListening`
  * New: Supports `useAdvertisingIdForDeviceId`
  * New: Supports `outOfSession` events
  * New: Supports specifying `groupName` and `groupValue` properties
  * Fix: Sets userId to `null` on `.reset()`

Version 1.3.0 (October 16, 2017)
==============================
*(Supports analytics-android 4.2.4+ and Amplitude 2.15.+)*

  * New: Falls back on `total` when `revenue` is not present
  * Updating Amplitude dependency to 2.15.0.

Version 1.2.1 (May 25, 2017)
==============================
*(Supports analytics-android 4.0.+ and Amplitude 2.13.4+)*

  * Fix: Support lists for Amplitude groups.

Version 1.2.0 (May 22, 2017)
==============================
*(Supports analytics-android 4.0.+ and Amplitude 2.13.4+)*

  * New: Support Amplitude groups.

Version 1.1.2 (April 27, 2017)
==============================
*(Supports analytics-android 4.0.+ and Amplitude 2.13.3+)*

  * [Fix](https://github.com/segment-integrations/analytics-android-integration-amplitude/pull/15): `reset` method implementation so that it calls `regenerateDeviceId` instead of `clearUserProperties`.

Version 1.1.1 (August 8th, 2016)
==============================
*(Supports analytics-android 4.0.+ and Amplitude 2.9.2+)*

  * Fix handling revenue correctly with `useLogRevenueV2` option.

Version 1.1.0 (July 21st, 2016)
==============================
*(Supports analytics-android 4.0.+ and Amplitude 2.9.2+)*

  * Updating Amplitude dependency to 2.9.2.
  * Support `useLogRevenueV2` option.

Version 1.0.2 (May 2nd, 2016)
==============================
*(Supports analytics-android 4.0.+ and Amplitude 2.7.1+)*

  * Updating Amplitude dependency to 2.7.1.
  * Map reset method to call `clearUserProperties`.
  * Map group method.


Version 1.0.1 (March 15th, 2016)
==============================
*(Supports analytics-android 4.0.+ and Amplitude 2.5.1+)*

  * Updating Amplitude dependency to 2.5.1.


Version 1.0.0 (November 26th, 2015)
==============================
*(Supports analytics-android 4.0.+ and Amplitude 2.2.+)*

  * Initial Release
