package com.segment.analytics;


import static org.junit.Assert.fail;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import com.segment.analytics.runscope.MessageResponse;
import com.segment.analytics.runscope.MessagesResponse;
import com.segment.analytics.runscope.RunscopeService;
import com.segment.analytics.sample.MainActivity;
import com.segment.analytics.sample.test.BuildConfig;
import com.segment.backo.Backo;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * An end to end to test that sends events to a Segment source, and verifies that a webhook
 * connected to the source connected to the source (configured manually via the app) is able to
 * receive the data sent by this library.
 *
 * See https://paper.dropbox.com/doc/Libraries-End-to-End-Tests-ESEakc3LxFrqcHz69AmyN for details.
 */
@RunWith(AndroidJUnit4.class)
public class E2ETest {

  @Rule
  public final ActivityTestRule<MainActivity> activityActivityTestRule =
      new ActivityTestRule<>(MainActivity.class);

  @Rule
  public EndToEndTestsDisabledRule endToEndTestsDisabledRule = new EndToEndTestsDisabledRule();

  /**
   * Write key for the Segment project to send data to.
   * https://app.segment.com/segment-libraries/sources/analytics_android_e2e_test/overview
   */
  private static final String SEGMENT_WRITE_KEY = "OAtgAHjkAD5MP31srDe9wiBjpvcXC8De";
  /**
   * Runscope bucket that is connect to the Segment project.
   * https://www.runscope.com/radar/uy22axz4sdb8
   */
  private static final String RUNSCOPE_BUCKET = "uy22axz4sdb8";
  // Token to read data from the Runscope bucket.
  private static final String RUNSCOPE_TOKEN = BuildConfig.RUNSCOPE_TOKEN;

  private static final Backo BACKO = Backo.builder()
      .base(TimeUnit.SECONDS, 1)
      .cap(TimeUnit.SECONDS, 5)
      .build();

  private Analytics analytics;
  private RunscopeService runscopeService;

  @Before
  public void setup() {
    analytics = new Analytics.Builder(activityActivityTestRule.getActivity(), SEGMENT_WRITE_KEY)
        .build();

    runscopeService = new Retrofit.Builder()
        .baseUrl("https://api.runscope.com")
        .addConverterFactory(MoshiConverterFactory.create())
        .client(new OkHttpClient.Builder()
            .addNetworkInterceptor(new Interceptor() {
              @Override
              public okhttp3.Response intercept(Chain chain) throws IOException {
                return chain.proceed(chain.request()
                    .newBuilder()
                    .addHeader("Authorization", "Bearer " + RUNSCOPE_TOKEN)
                    .build());
              }
            })
            .build())
        .build()
        .create(RunscopeService.class);
  }

  @After
  public void tearDown() {
    analytics.shutdown();
  }

  @Test
  public void track() {
    final String uuid = UUID.randomUUID().toString();
    analytics.track("Simple Track", new Properties().putValue("id", uuid));
    analytics.flush();

    assertMessageReceivedByWebhook(uuid);
  }

  @Test
  public void screen() {
    final String uuid = UUID.randomUUID().toString();
    analytics.screen("Home", new Properties().putValue("id", uuid));
    analytics.flush();

    assertMessageReceivedByWebhook(uuid);
  }

  @Test
  public void group() {
    final String uuid = UUID.randomUUID().toString();
    analytics.group("segment", new Traits().putValue("id", uuid));
    analytics.flush();

    assertMessageReceivedByWebhook(uuid);
  }

  @Test
  public void identify() throws Exception {
    final String uuid = UUID.randomUUID().toString();
    analytics.group("prateek", new Traits().putValue("id", uuid));
    analytics.flush();

    assertMessageReceivedByWebhook(uuid);
  }

  private void assertMessageReceivedByWebhook(String id) {
    for (int i = 0; i < 10; i++) {
      try {
        BACKO.sleep(i);
        if (hasMatchingRequest(id)) {
          return;
        }
      } catch (Exception ignored) {
        // Catch and ignore so that we can retry.
      }
    }

    fail("did not find message with id: " + id);
  }

  /**
   * Returns {@code true} if a message with the provided ID is found in Runscope.
   */
  @SuppressWarnings("ConstantConditions")
  private boolean hasMatchingRequest(String id) throws IOException {
    Response<MessagesResponse> messagesResponse = runscopeService
        .messages(RUNSCOPE_BUCKET)
        .execute();

    for (MessagesResponse.Message message : messagesResponse.body().data) {
      Response<MessageResponse> messageResponse = runscopeService
          .message(RUNSCOPE_BUCKET, message.uuid)
          .execute();

      // TODO: Deserialize into Segment message and check against properties.
      if (messageResponse.body().data.request.body.contains(id)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Skips tests if they were supposed to be ignored.
   */
  static class EndToEndTestsDisabledRule implements MethodRule {

    @Override
    public Statement apply(final Statement base, FrameworkMethod method, Object target) {
      return new Statement() {
        @Override
        public void evaluate() throws Throwable {
          // Skip the test if End to End tests are disabled (e.g. contributor PRs).
          if (!BuildConfig.RUN_E2E_TESTS) {
            return;
          }
          base.evaluate();
        }
      };
    }
  }
}
