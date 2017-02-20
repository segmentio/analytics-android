package com.segment.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.annotation.Config.NONE;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;
import com.segment.analytics.core.BuildConfig;
import com.segment.analytics.integrations.Logger;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = NONE)
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
}
