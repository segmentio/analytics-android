package com.segment.analytics;

import static com.segment.analytics.TestUtils.grantPermission;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import android.Manifest;
import com.google.common.util.concurrent.MoreExecutors;
import com.segment.analytics.Analytics.Builder;
import com.segment.analytics.integrations.BasePayload;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MiddlewareTest {

  Analytics.Builder builder;

  @Before
  public void setUp() {
    initMocks(this);
    Analytics.INSTANCES.clear();
    grantPermission(RuntimeEnvironment.application, Manifest.permission.INTERNET);
    builder =
        new Builder(RuntimeEnvironment.application, "write_key")
            .executor(MoreExecutors.newDirectExecutorService());
  }

  @Test
  public void middlewareCanShortCircuit() throws Exception {
    final AtomicReference<TrackPayload> payloadRef = new AtomicReference<>();
    Analytics analytics =
        builder
            .middleware(
                new Middleware() {
                  @Override
                  public void intercept(Chain chain) {
                    payloadRef.set((TrackPayload) chain.payload());
                  }
                })
            .middleware(
                new Middleware() {
                  @Override
                  public void intercept(Chain chain) {
                    throw new AssertionError("should not be invoked");
                  }
                })
            .build();

    analytics.track("foo");
    assertThat(payloadRef.get().event()).isEqualTo("foo");
  }

  @Test
  public void middlewareCanProceed() throws Exception {
    final AtomicReference<ScreenPayload> payloadRef = new AtomicReference<>();
    Analytics analytics =
        builder
            .middleware(
                new Middleware() {
                  @Override
                  public void intercept(Chain chain) {
                    chain.proceed(chain.payload());
                  }
                })
            .middleware(
                new Middleware() {
                  @Override
                  public void intercept(Chain chain) {
                    BasePayload payload = chain.payload();
                    payloadRef.set((ScreenPayload) payload);
                    chain.proceed(payload);
                  }
                })
            .build();

    analytics.screen("foo");
    assertThat(payloadRef.get().name()).isEqualTo("foo");
  }

  @Test
  public void middlewareCanTransform() throws Exception {
    final AtomicReference<BasePayload> payloadRef = new AtomicReference<>();
    Analytics analytics =
        builder
            .middleware(
                new Middleware() {
                  @Override
                  public void intercept(Chain chain) {
                    chain.proceed(chain.payload().toBuilder().messageId("override").build());
                  }
                })
            .middleware(
                new Middleware() {
                  @Override
                  public void intercept(Chain chain) {
                    BasePayload payload = chain.payload();
                    payloadRef.set(payload);
                    chain.proceed(payload);
                  }
                })
            .build();

    analytics.identify("prateek");
    assertThat(payloadRef.get().messageId()).isEqualTo("override");
  }
}
