package com.segment.analytics;

import android.content.Context;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static com.segment.analytics.TestUtils.createLogger;
import static com.segment.analytics.TestUtils.mockApplication;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.any;
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
  Logger logger;
  Queue<BasePayload> queue;
  Dispatcher dispatcher;

  @Before public void setUp() {
    initMocks(this);
    context = mockApplication();
    when(context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE)).thenReturn(PERMISSION_DENIED);
    logger = createLogger();
    queue = new ArrayDeque<BasePayload>();
    dispatcher = createDispatcher(20);
  }

  Dispatcher createDispatcher(int maxQueueSize) {
    return new Dispatcher(context, maxQueueSize, segmentHTTPApi, queue, stats, logger);
  }

  @Test public void addsToQueueCorrectly() {
    dispatcher = createDispatcher(20);
    assertThat(queue).hasSize(0);
    dispatcher.performEnqueue(mock(BasePayload.class));
    assertThat(queue).hasSize(1);
    dispatcher.performEnqueue(mock(BasePayload.class));
    assertThat(queue).hasSize(2);
  }

  @Test public void flushesQueueCorrectly() {
    dispatcher = createDispatcher(20);
    dispatcher.performEnqueue(mock(BasePayload.class));
    dispatcher.performEnqueue(mock(BasePayload.class));
    dispatcher.performEnqueue(mock(BasePayload.class));
    dispatcher.performEnqueue(mock(BasePayload.class));

    dispatcher.performFlush();
    try {
      verify(segmentHTTPApi).upload((java.util.List<BasePayload>) any());
    } catch (IOException e) {
      fail("should not throw exception");
    }
    verify(stats).dispatchFlush(4);
    assertThat(queue).hasSize(0);
  }

  @Test public void flushesWhenQueueHitsMax() {
    dispatcher = createDispatcher(3);
    assertThat(queue).hasSize(0);
    dispatcher.performEnqueue(mock(BasePayload.class));
    dispatcher.performEnqueue(mock(BasePayload.class));
    dispatcher.performEnqueue(mock(BasePayload.class));

    try {
      verify(segmentHTTPApi).upload((java.util.List<BasePayload>) any());
    } catch (IOException e) {
      fail("should not throw exception");
    }
    verify(stats).dispatchFlush(3);
    assertThat(queue).hasSize(0);
  }
}
