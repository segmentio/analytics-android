package com.segment.analytics;

import android.content.Context;
import java.io.IOException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static com.segment.analytics.TestUtils.mockApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.Mock;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class) @Config(emulateSdk = 18, manifest = Config.NONE)
public class DispatcherTest {
  @Mock SegmentHTTPApi segmentHTTPApi;
  @Mock Stats stats;
  Context context;
  ObjectQueue<BasePayload> queue;
  Dispatcher dispatcher;

  @Before public void setUp() {
    initMocks(this);
    context = mockApplication();
    when(context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)).thenReturn(PERMISSION_DENIED);
    queue = new InMemoryObjectQueue<BasePayload>();
    dispatcher = createDispatcher(20);
  }

  Dispatcher createDispatcher(int maxQueueSize) {
    return new Dispatcher(context, maxQueueSize, segmentHTTPApi, queue,
        Collections.<String, Boolean>emptyMap(), stats, true);
  }

  @Test public void addsToQueueCorrectly() {
    dispatcher = createDispatcher(20);
    assertThat(queue.size()).isEqualTo(0);
    dispatcher.performEnqueue(mock(BasePayload.class));
    assertThat(queue.size()).isEqualTo(1);
    dispatcher.performEnqueue(mock(BasePayload.class));
    assertThat(queue.size()).isEqualTo(2);
  }

  @Test public void flushesQueueCorrectly() {
    dispatcher = createDispatcher(20);
    dispatcher.performEnqueue(mock(BasePayload.class));
    dispatcher.performEnqueue(mock(BasePayload.class));
    dispatcher.performEnqueue(mock(BasePayload.class));
    dispatcher.performEnqueue(mock(BasePayload.class));

    dispatcher.performFlush();
    try {
      verify(segmentHTTPApi).upload(Matchers.<Dispatcher.BatchPayload>any());
    } catch (IOException e) {
      fail("should not throw exception");
    }
    verify(stats).dispatchFlush(4);
    assertThat(queue.size()).isEqualTo(0);
  }

  @Test public void flushesWhenQueueHitsMax() {
    dispatcher = createDispatcher(3);
    assertThat(queue.size()).isEqualTo(0);
    dispatcher.performEnqueue(mock(BasePayload.class));
    dispatcher.performEnqueue(mock(BasePayload.class));
    dispatcher.performEnqueue(mock(BasePayload.class));

    try {
      verify(segmentHTTPApi).upload(Matchers.<Dispatcher.BatchPayload>any());
    } catch (IOException e) {
      fail("should not throw exception");
    }
    verify(stats).dispatchFlush(3);
    assertThat(queue.size()).isEqualTo(0);
  }
}
