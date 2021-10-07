## Developer Feedback Requested: Analytics-Kotlin Beta

**A beta release of the new Analytics-Kotlin library is available at the [Analytics-Kotlin repository](https://github.com/segmentio/analytics-kotlin). This library is governed by [Segment’s First-Access and Beta terms](https://segment.com/legal/first-access-beta-preview/), and should not be used in production scenarios.**

During the beta phase, Segment wants your feedback, contributions, and ideas. If you have requirements or ideas for features for Analytics-Kotlin and Segment's integration with the Android platform, let us know.


analytics-android
=================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.segment.analytics.android/analytics/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.segment.analytics.android/analytics)
[![Javadocs](http://javadoc-badge.appspot.com/com.segment.analytics.android/analytics.svg?label=javadoc)](http://javadoc-badge.appspot.com/com.segment.analytics.android/analytics)
[![CircleCI](https://circleci.com/gh/segmentio/analytics-android.svg?style=svg)](https://circleci.com/gh/segmentio/analytics-android)
[![codecov](https://codecov.io/gh/segmentio/analytics-android/branch/master/graph/badge.svg)](https://codecov.io/gh/segmentio/analytics-android)

analytics-android is an Android client for [Segment](https://segment.com)


<div align="center">
  <img src="https://user-images.githubusercontent.com/16131737/53615894-00237380-3b95-11e9-946c-5539d9835c64.png"/>
  <p><b><i>You can't fix what you can't measure</i></b></p>
</div>

Analytics helps you measure your users, product, and business. It unlocks insights into your app's funnel, core business metrics, and whether you have product-market fit.

## How to get started
1. **Collect analytics data** from your app(s).
    - The top 200 Segment companies collect data from 5+ source types (web, mobile, server, CRM, etc.).
2. **Send the data to analytics tools** (for example, Google Analytics, Amplitude, Mixpanel).
    - Over 250+ Segment companies send data to eight categories of destinations such as analytics tools, warehouses, email marketing and remarketing systems, session recording, and more.
3. **Explore your data** by creating metrics (for example, new signups, retention cohorts, and revenue generation).
    - The best Segment companies use retention cohorts to measure product market fit. Netflix has 70% paid retention after 12 months, 30% after 7 years.

[Segment](https://segment.com) collects analytics data and allows you to send it to more than 250 apps (such as Google Analytics, Mixpanel, Optimizely, Facebook Ads, Slack, Sentry) just by flipping a switch. You only need one Segment code snippet, and you can turn integrations on and off at will, with no additional code. [Sign up with Segment today](https://app.segment.com/signup).

### Why?
1. **Power all your analytics apps with the same data**. Instead of writing code to integrate all of your tools individually, send data to Segment, once.

2. **Install tracking for the last time**. We're the last integration you'll ever need to write. You only need to instrument Segment once. Reduce all of your tracking code and advertising tags into a single set of API calls.

3. **Send data from anywhere**. Send Segment data from any device, and we'll transform and send it on to any tool.

4. **Query your data in SQL**. Slice, dice, and analyze your data in detail with Segment SQL. We'll transform and load your customer behavioral data directly from your apps into Amazon Redshift, Google BigQuery, or Postgres. Save weeks of engineering time by not having to invent your own data warehouse and ETL pipeline.

    For example, you can capture data on any app:
    ```js
    analytics.track('Order Completed', { price: 99.84 })
    ```
    Then, query the resulting data in SQL:
    ```sql
    select * from app.order_completed
    order by price desc
    ```

### 🚀 Startup Program
<div align="center">
  <a href="https://segment.com/startups"><img src="https://user-images.githubusercontent.com/16131737/53128952-08d3d400-351b-11e9-9730-7da35adda781.png" /></a>
</div>
If you are part of a new startup  (&lt;$5M raised, &lt;2 years since founding), we just launched a new startup program for you. You can get a Segment Team plan  (up to <b>$25,000 value</b> in Segment credits) for free up to 2 years — <a href="https://segment.com/startups/">apply here</a>!

## Documentation

You can find usage documentation at [https://segment.com/docs/sources/mobile/android/](https://segment.com/docs/sources/mobile/android/).

## Integrating with Segment

Interested in integrating your service with us? Check out our [Partners page](https://segment.com/partners/) for more details.

## License

```
WWWWWW||WWWWWW
 W W W||W W W
      ||
    ( OO )__________
     /  |           \
    /o o|    MIT     \
    \___/||_||__||_|| *
         || ||  || ||
        _||_|| _||_||
       (__|__|(__|__|

The MIT License (MIT)

Copyright (c) 2016 Segment, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
