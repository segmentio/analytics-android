/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.segment.android.test;

import android.util.Log;
import com.segment.android.Analytics;
import com.segment.android.models.Options;
import com.segment.android.models.Props;
import com.segment.android.models.Traits;
import com.segment.android.stats.AnalyticsStatistics;
import java.util.Random;
import junit.framework.Assert;
import org.junit.Test;

public class BasicAnalyticsTest extends BaseTest {

  private static String userId = "android_user_" + (new Random()).nextInt(999999);
  private static String groupId = "group_id_" + (new Random()).nextInt(999999);

  @Override
  protected void setUp() {
    super.setUp();

    Log.w("AnalyticsTest", "Analytics Test using userId: " +
        userId + " and groupId: " + groupId);
  }

  @Test
  public void testIdentify() {

    AnalyticsStatistics statistics = Analytics.getStatistics();

    int insertAttempts = statistics.getInsertAttempts().getCount();
    int identifyAttempts = statistics.getIdentifies().getCount();
    int flushAttempts = statistics.getFlushAttempts().getCount();
    int successful = statistics.getSuccessful().getCount();

    Traits traits = new Traits("username", userId, "baller", true);

    Analytics.identify(userId, traits);

    Analytics.identify(userId);

    Analytics.identify(new Traits("username", userId, "baller", true, "just_user_id", true));

    Analytics.identify(traits, new Options().setTimestamp(TestCases.calendar()));

    Analytics.identify(traits, new Options().setTimestamp(TestCases.calendar())
            .setIntegration("Mixpanel", true)
            .setIntegration("KISSMetrics", true)
    );

    Assert.assertEquals(identifyAttempts + 5, statistics.getIdentifies().getCount());
    Assert.assertEquals(insertAttempts + 5, statistics.getInsertAttempts().getCount());

    Analytics.flush(false);

    Assert.assertEquals(flushAttempts + 1, statistics.getFlushAttempts().getCount());

    Assert.assertEquals(successful + 5, statistics.getSuccessful().getCount());
  }

  @Test
  public void testGroup() {

    AnalyticsStatistics statistics = Analytics.getStatistics();

    int insertAttempts = statistics.getInsertAttempts().getCount();
    int groupAttempts = statistics.getGroups().getCount();
    int flushAttempts = statistics.getFlushAttempts().getCount();
    int successful = statistics.getSuccessful().getCount();

    Traits traits = new Traits("name", "segment.io", "plan", "Pro");

    Analytics.group(groupId, traits);

    Analytics.group(groupId);

    Analytics.group(new Traits("username", userId, "baller", true, "just_user_id", true));

    Analytics.group(traits, new Options().setTimestamp(TestCases.calendar()));

    Analytics.group(traits, new Options().setTimestamp(TestCases.calendar())
            .setIntegration("Mixpanel", true)
            .setIntegration("KISSMetrics", true)
    );

    Assert.assertEquals(groupAttempts + 5, statistics.getGroups().getCount());
    Assert.assertEquals(insertAttempts + 5, statistics.getInsertAttempts().getCount());

    Analytics.flush(false);

    Assert.assertEquals(flushAttempts + 1, statistics.getFlushAttempts().getCount());

    Assert.assertEquals(successful + 5, statistics.getSuccessful().getCount());
  }

  @Test
  public void testTrack() {

    AnalyticsStatistics statistics = Analytics.getStatistics();

    int insertAttempts = statistics.getInsertAttempts().getCount();
    int trackAttempts = statistics.getTracks().getCount();
    int flushAttempts = statistics.getFlushAttempts().getCount();
    int successful = statistics.getSuccessful().getCount();

    Analytics.track("Android: UserId Saved Action");

    Analytics.track("Android: UserId Not Saved Action");

    Analytics.track("Android: First Event Properties Event",
        new Props("Mickey Mouse", 4, "Donnie", "Darko"));

    Analytics.track("Android: With Calendar", new Props(),
        new Options().setTimestamp(TestCases.calendar()));

    Analytics.track("Android: With Context", new Props(),
        new Options().setTimestamp(TestCases.calendar())
            .setIntegration("Mixpanel", true)
            .setIntegration("KISSMetrics", true)
    );

    Assert.assertEquals(trackAttempts + 5, statistics.getTracks().getCount());
    Assert.assertEquals(insertAttempts + 5, statistics.getInsertAttempts().getCount());

    Analytics.flush(false);

    Assert.assertEquals(flushAttempts + 1, statistics.getFlushAttempts().getCount());

    Assert.assertEquals(successful + 5, statistics.getSuccessful().getCount());
  }

  @Test
  public void testScreen() {

    AnalyticsStatistics statistics = Analytics.getStatistics();

    int insertAttempts = statistics.getInsertAttempts().getCount();
    int screenAttempts = statistics.getScreens().getCount();
    int flushAttempts = statistics.getFlushAttempts().getCount();
    int successful = statistics.getSuccessful().getCount();

    Analytics.screen("Android: Test Screen");

    Analytics.screen("Android: Another Screen Action");

    Analytics.screen("Android: First Event Screen Properties Event",
        new Props("Mickey Mouse", 4, "Donnie", "Darko"));

    Analytics.screen("Android: Screen With Calendar", new Props(),
        new Options().setTimestamp(TestCases.calendar())
    );

    Analytics.screen("Android: Screen With Context", new Props(),
        new Options().setTimestamp(TestCases.calendar())
            .setIntegration("Mixpanel", true)
            .setIntegration("KISSMetrics", true)
    );

    Assert.assertEquals(screenAttempts + 5, statistics.getScreens().getCount());
    Assert.assertEquals(insertAttempts + 5, statistics.getInsertAttempts().getCount());

    Analytics.flush(false);

    Assert.assertEquals(flushAttempts + 1, statistics.getFlushAttempts().getCount());

    Assert.assertEquals(successful + 5, statistics.getSuccessful().getCount());
  }

  @Test
  public void testAlias() {

    AnalyticsStatistics statistics = Analytics.getStatistics();

    int insertAttempts = statistics.getInsertAttempts().getCount();
    int aliasAttempts = statistics.getAlias().getCount();
    int flushAttempts = statistics.getFlushAttempts().getCount();
    int successful = statistics.getSuccessful().getCount();

    String from = Analytics.getAnonymousId();
    String to = "android_user_" + (new Random()).nextInt(999999);

    Analytics.setAnonymousId(from);

    Log.w("AnalyticsTest", "Aliasing : " + from + " => " + to);

    Analytics.track("Anonymous Event");

    Analytics.alias(from, to);

    Analytics.identify(to, new Traits("Crazay", "Duh"));

    Analytics.track("Identified Event");

    Assert.assertEquals(aliasAttempts + 1, statistics.getAlias().getCount());
    Assert.assertEquals(insertAttempts + 4, statistics.getInsertAttempts().getCount());

    Analytics.flush(false);

    Assert.assertEquals(flushAttempts + 1, statistics.getFlushAttempts().getCount());

    Assert.assertEquals(successful + 4, statistics.getSuccessful().getCount());
  }
}
