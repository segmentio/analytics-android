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

import static com.segment.analytics.TestUtils.grantPermission;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import android.Manifest;
import androidx.annotation.NonNull;
import com.google.common.util.concurrent.MoreExecutors;
import com.segment.analytics.integrations.BasePayload;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.TrackPayload;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DestinationMiddlewareTest {

  Analytics.Builder builder;

  @Mock Integration<Void> integrationFoo;
  @Mock Integration<Void> integrationBar;

  @Before
  public void setUp() {
    initMocks(this);
    Analytics.INSTANCES.clear();
    grantPermission(RuntimeEnvironment.application, Manifest.permission.INTERNET);
    ValueMap projectSettings =
        new ValueMap()
            .putValue(
                "integrations",
                new ValueMap()
                    .putValue("foo", new ValueMap().putValue("appToken", "foo_token"))
                    .putValue(
                        "bar",
                        new ValueMap()
                            .putValue("appToken", "bar_token")
                            .putValue("trackAttributionData", true)));
    builder =
        new Analytics.Builder(RuntimeEnvironment.application, "write_key")
            .defaultProjectSettings(projectSettings)
            .use(
                new Integration.Factory() {
                  @Override
                  public Integration<?> create(ValueMap settings, Analytics analytics) {
                    return integrationFoo;
                  }

                  @NonNull
                  @Override
                  public String key() {
                    return "foo";
                  }
                })
            .use(
                new Integration.Factory() {
                  @Override
                  public Integration<?> create(ValueMap settings, Analytics analytics) {
                    return integrationBar;
                  }

                  @NonNull
                  @Override
                  public String key() {
                    return "bar";
                  }
                })
            .executor(MoreExecutors.newDirectExecutorService());
  }

  @Test
  public void middlewareRuns() throws Exception {
    final AtomicReference<TrackPayload> payloadRef = new AtomicReference<>();
    Analytics analytics =
        builder
            .useDestinationMiddleware(
                "foo",
                new Middleware() {
                  @Override
                  public void intercept(Chain chain) {
                    payloadRef.set((TrackPayload) chain.payload());
                    chain.proceed(chain.payload());
                  }
                })
            .build();

    analytics.track("foo");
    assertThat(payloadRef.get().event()).isEqualTo("foo");
    verify(integrationFoo).track(payloadRef.get());
  }

  @Test
  public void middlewareDoesNotRunForOtherIntegration() throws Exception {
    final AtomicReference<TrackPayload> payloadRefOriginal = new AtomicReference<>();
    final AtomicReference<TrackPayload> payloadRefDestMiddleware = new AtomicReference<>();
    Analytics analytics =
        builder
            .useSourceMiddleware(
                new Middleware() {
                  @Override
                  public void intercept(Chain chain) {
                    payloadRefOriginal.set((TrackPayload) chain.payload());
                    chain.proceed(chain.payload());
                  }
                })
            .useDestinationMiddleware(
                "foo",
                new Middleware() {
                  @Override
                  public void intercept(Chain chain) {
                    // modify reference and add new property
                    TrackPayload payload = (TrackPayload) chain.payload();
                    ValueMap properties = new ValueMap();
                    properties.putAll(payload.properties());

                    TrackPayload newPayload =
                        payload
                            .toBuilder()
                            .properties(properties.putValue("middleware_key", "middleware_value"))
                            .build();
                    payloadRefDestMiddleware.set(newPayload);
                    chain.proceed(newPayload);
                  }
                })
            .build();

    analytics.track("foo");

    assertThat(payloadRefOriginal.get().event()).isEqualTo("foo");
    verify(integrationBar).track(payloadRefOriginal.get());

    assertThat(payloadRefDestMiddleware.get().event()).isEqualTo("foo");
    assertThat(payloadRefDestMiddleware.get().properties()).containsKey("middleware_key");
    assertThat(payloadRefDestMiddleware.get().properties().get("middleware_key"))
        .isEqualTo("middleware_value");
    verify(integrationFoo).track(payloadRefDestMiddleware.get());
  }

  @Test
  public void middlewareWillRunForMultipleIntegrations() throws Exception {
    final AtomicReference<TrackPayload> payloadRefOriginal = new AtomicReference<>();
    final AtomicReference<TrackPayload> payloadRefDestMiddleware = new AtomicReference<>();
    final AtomicInteger destCounter = new AtomicInteger(0);
    Middleware middleware =
        new Middleware() {
          @Override
          public void intercept(Chain chain) {
            TrackPayload newPayload = (TrackPayload) chain.payload().toBuilder().build();
            payloadRefDestMiddleware.set(newPayload);
            destCounter.incrementAndGet();
            chain.proceed(newPayload);
          }
        };
    Analytics analytics =
        builder
            .useSourceMiddleware(
                new Middleware() {
                  @Override
                  public void intercept(Chain chain) {
                    payloadRefOriginal.set((TrackPayload) chain.payload());
                    chain.proceed(chain.payload());
                  }
                })
            .useDestinationMiddleware("foo", middleware)
            .useDestinationMiddleware("Segment.io", middleware)
            .build();

    analytics.track("foo");

    assertThat(destCounter.get()).isEqualTo(2); // should only be called for 2 integrations
    verify(integrationBar).track(payloadRefOriginal.get());
    verify(integrationFoo).track(payloadRefDestMiddleware.get());
  }

  @Test
  public void middlewareCanShortCircuit() throws Exception {
    final AtomicReference<TrackPayload> payloadRef = new AtomicReference<>();
    Analytics analytics =
        builder
            .useSourceMiddleware(
                new Middleware() {
                  @Override
                  public void intercept(Chain chain) {
                    payloadRef.set((TrackPayload) chain.payload());
                    chain.proceed(chain.payload());
                  }
                })
            .useDestinationMiddleware(
                "foo",
                new Middleware() {
                  @Override
                  public void intercept(Chain chain) {
                    // drop event for `foo` integration
                  }
                })
            .build();

    analytics.track("foo");

    assertThat(payloadRef.get().event()).isEqualTo("foo");
    verify(integrationFoo, never()).track(anyObject()); // validate event does not go through
    verify(integrationBar).track(payloadRef.get()); // validate event goes through
  }

  @Test
  public void middlewareCanChain() throws Exception {
    final AtomicReference<TrackPayload> payloadRef = new AtomicReference<>();
    Analytics analytics =
        builder
            .useDestinationMiddleware(
                "foo",
                new Middleware() {
                  @Override
                  public void intercept(Chain chain) {
                    TrackPayload payload = (TrackPayload) chain.payload();
                    ValueMap properties = new ValueMap();
                    properties.putAll(payload.properties());

                    TrackPayload newPayload =
                        payload.toBuilder().properties(properties.putValue("key1", "val1")).build();
                    chain.proceed(newPayload);
                  }
                })
            .useDestinationMiddleware(
                "foo",
                new Middleware() {
                  @Override
                  public void intercept(Chain chain) {
                    TrackPayload payload = (TrackPayload) chain.payload();
                    ValueMap properties = new ValueMap();
                    properties.putAll(payload.properties());

                    TrackPayload newPayload =
                        payload.toBuilder().properties(properties.putValue("key2", "val2")).build();
                    payloadRef.set(newPayload);
                    chain.proceed(newPayload);
                  }
                })
            .build();

    analytics.track("foo");
    assertThat(payloadRef.get().properties()).containsKey("key1");
    assertThat(payloadRef.get().properties().get("key1")).isEqualTo("val1");
    assertThat(payloadRef.get().properties()).containsKey("key2");
    assertThat(payloadRef.get().properties().get("key2")).isEqualTo("val2");
  }

  @Test
  public void middlewareCanTransform() throws Exception {
    final AtomicReference<IdentifyPayload> payloadRefOriginal = new AtomicReference<>();
    final AtomicReference<IdentifyPayload> payloadRefDestMiddleware = new AtomicReference<>();
    Analytics analytics =
        builder
            .useSourceMiddleware(
                new Middleware() {
                  @Override
                  public void intercept(Chain chain) {
                    payloadRefOriginal.set((IdentifyPayload) chain.payload());
                    chain.proceed(chain.payload());
                  }
                })
            .useDestinationMiddleware(
                "foo",
                new Middleware() {
                  @Override
                  public void intercept(Chain chain) {
                    chain.proceed(chain.payload().toBuilder().messageId("override").build());
                  }
                })
            .useDestinationMiddleware(
                "foo",
                new Middleware() {
                  @Override
                  public void intercept(Chain chain) {
                    BasePayload payload = chain.payload();
                    payloadRefDestMiddleware.set((IdentifyPayload) payload);
                    chain.proceed(payload);
                  }
                })
            .build();

    analytics.identify("prateek");
    assertThat(payloadRefDestMiddleware.get().messageId()).isEqualTo("override");
    verify(integrationFoo).identify(payloadRefDestMiddleware.get());
    verify(integrationBar).identify(payloadRefOriginal.get());
  }
}
