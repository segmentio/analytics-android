/**
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
package com.segment.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import com.segment.analytics.sample.BuildConfig;
import com.segment.analytics.sample.MainActivity;
import com.segment.analytics.webhook.WebhookService;
import com.segment.backo.Backo;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.Credentials;
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
 * <p>See https://paper.dropbox.com/doc/Libraries-End-to-End-Tests-ESEakc3LxFrqcHz69AmyN for
 * details.
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
    /** Webhook bucket that is connected to the Segment project. */
    private static final String WEBHOOK_BUCKET = "android";
    /** Credentials to retrieve data from the webhook. */
    private static final String WEBHOOK_AUTH_USERNAME = BuildConfig.WEBHOOK_AUTH_USERNAME;

    private static final Backo BACKO =
            Backo.builder().base(TimeUnit.SECONDS, 1).cap(TimeUnit.MINUTES, 5).build();

    private Analytics analytics;
    private WebhookService webhookService;

    @Before
    public void setup() {
        analytics =
                new Analytics.Builder(activityActivityTestRule.getActivity(), SEGMENT_WRITE_KEY)
                        .build();

        webhookService =
                new Retrofit.Builder()
                        .baseUrl("https://webhook-e2e.segment.com")
                        .addConverterFactory(MoshiConverterFactory.create())
                        .client(
                                new OkHttpClient.Builder()
                                        .addNetworkInterceptor(
                                                new Interceptor() {
                                                    @Override
                                                    public okhttp3.Response intercept(Chain chain)
                                                            throws IOException {
                                                        return chain.proceed(
                                                                chain.request()
                                                                        .newBuilder()
                                                                        .addHeader(
                                                                                "Authorization",
                                                                                Credentials.basic(
                                                                                        WEBHOOK_AUTH_USERNAME,
                                                                                        ""))
                                                                        .build());
                                                    }
                                                })
                                        .build())
                        .build()
                        .create(WebhookService.class);
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

    /** Returns {@code true} if a message with the provided ID is found in the webhook. */
    @SuppressWarnings("ConstantConditions")
    private boolean hasMatchingRequest(String id) throws IOException {
        Response<List<String>> messagesResponse =
                webhookService.messages(WEBHOOK_BUCKET, 500).execute();

        assertThat(messagesResponse.code()).isEqualTo(200);

        for (String message : messagesResponse.body()) {
            // TODO: Deserialize into Segment message and check against properties.
            if (message.contains(id)) {
                return true;
            }
        }

        return false;
    }

    /** Skips tests if they were supposed to be ignored. */
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
