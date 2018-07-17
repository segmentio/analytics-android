package com.segment.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.annotation.Config.NONE;

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.provider.Settings.Secure;
import android.util.Pair;
import com.segment.analytics.integrations.Logger;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class GetAdvertisingIdTaskTest {
  @Test
  public void getAdvertisingId() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    Traits traits = Traits.create();
    AnalyticsContext context =
        AnalyticsContext.create(RuntimeEnvironment.application, traits, true);
    AsyncTask<Context, Void, Pair<String, Boolean>> task =
        new GetAdvertisingIdTask(context, latch, Logger.with(Analytics.LogLevel.VERBOSE));
    task.execute(RuntimeEnvironment.application);
    latch.await();
    assertThat(context.device()).doesNotContainKey("advertisingId");
  }

  @Test
  public void getAdvertisingIdAmazonFireOSLimitAdTracking1() throws Exception {
    Context context = RuntimeEnvironment.application;
    ContentResolver contentResolver = context.getContentResolver();
    Secure.putInt(contentResolver, "limit_ad_tracking", 1);
    CountDownLatch latch = new CountDownLatch(1);

    Traits traits = Traits.create();
    AnalyticsContext analyticsContext =
        AnalyticsContext.create(RuntimeEnvironment.application, traits, true);

    AsyncTask<Context, Void, Pair<String, Boolean>> task =
        new GetAdvertisingIdTask(analyticsContext, latch, Logger.with(Analytics.LogLevel.VERBOSE));
    task.execute(context);
    latch.await();

    assertThat(analyticsContext.device()).doesNotContainKey("advertisingId");
    assertThat(analyticsContext.device()).containsEntry("adTrackingEnabled", false);
  }

  @Test
  public void getAdvertisingIdAmazonFireOSLimitAdTracking0() throws Exception {
    Context context = RuntimeEnvironment.application;
    ContentResolver contentResolver = context.getContentResolver();
    Secure.putInt(contentResolver, "limit_ad_tracking", 0);
    Secure.putString(contentResolver, "advertising_id", "df07c7dc-cea7-4a89-b328-810ff5acb15d");

    CountDownLatch latch = new CountDownLatch(1);

    Traits traits = Traits.create();
    AnalyticsContext analyticsContext =
        AnalyticsContext.create(RuntimeEnvironment.application, traits, true);

    AsyncTask<Context, Void, Pair<String, Boolean>> task =
        new GetAdvertisingIdTask(analyticsContext, latch, Logger.with(Analytics.LogLevel.VERBOSE));
    task.execute(context);
    latch.await();

    assertThat(analyticsContext.device())
        .containsEntry("advertisingId", "df07c7dc-cea7-4a89-b328-810ff5acb15d");
    assertThat(analyticsContext.device()).containsEntry("adTrackingEnabled", true);
  }
}
