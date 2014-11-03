package com.segment.analytics;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.squareup.tape.FileObjectQueue;
import com.squareup.tape.InMemoryObjectQueue;
import com.squareup.tape.ObjectQueue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.Utils.OWNER_SEGMENT;
import static com.segment.analytics.Utils.VERB_ENQUEUE;
import static com.segment.analytics.Utils.VERB_FLUSH;
import static com.segment.analytics.Utils.debug;
import static com.segment.analytics.Utils.error;
import static com.segment.analytics.Utils.isConnected;
import static com.segment.analytics.Utils.panic;
import static com.segment.analytics.Utils.quitThread;
import static com.segment.analytics.Utils.toISO8601Date;

/**
 * The actual service that posts data to Segment's servers.
 *
 * @since 2.4
 */
class SegmentIntegration extends AbstractIntegration<Void> {

  static final int REQUEST_ENQUEUE = 0;
  static final int REQUEST_FLUSH = 1;

  private static final String SEGMENT_THREAD_NAME = Utils.THREAD_PREFIX + "Segment";
  private static final String TASK_QUEUE_FILE_NAME = "payload-task-queue-";

  final Context context;
  final ObjectQueue<BasePayload> queue;
  final SegmentHTTPApi segmentHTTPApi;
  final int queueSize;
  final int flushInterval;
  final Stats stats;
  final Handler handler;
  final HandlerThread segmentThread;
  final boolean debuggingEnabled;
  final Map<String, Boolean> integrations;

  static SegmentIntegration create(Context context, int queueSize, int flushInterval,
      SegmentHTTPApi segmentHTTPApi, Map<String, Boolean> integrations, String tag, Stats stats,
      boolean debuggingEnabled) {
    FileObjectQueue.Converter<BasePayload> converter = new PayloadConverter();
    ObjectQueue<BasePayload> queue;
    try {
      File parent = context.getFilesDir();
      if (!parent.exists()) parent.mkdirs();
      File queueFile = new File(parent, TASK_QUEUE_FILE_NAME + tag);
      queue = new FileObjectQueue<BasePayload>(queueFile, converter);
    } catch (IOException e) {
      queue = new InMemoryObjectQueue<BasePayload>();
    }
    return new SegmentIntegration(context, queueSize, flushInterval, segmentHTTPApi, queue,
        integrations, stats, debuggingEnabled);
  }

  SegmentIntegration(Context context, int queueSize, int flushInterval,
      SegmentHTTPApi segmentHTTPApi, ObjectQueue<BasePayload> queue,
      Map<String, Boolean> integrations, Stats stats, boolean debuggingEnabled) {
    super(debuggingEnabled);
    this.context = context;
    this.queueSize = queueSize;
    this.segmentHTTPApi = segmentHTTPApi;
    this.queue = queue;
    this.stats = stats;
    this.debuggingEnabled = debuggingEnabled;
    this.integrations = integrations;
    this.flushInterval = flushInterval * 1000;
    segmentThread = new HandlerThread(SEGMENT_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    segmentThread.start();
    handler = new SegmentHandler(segmentThread.getLooper(), this);
    rescheduleFlush();
  }

  @Override void initialize(Context context, JsonMap settings)
      throws InvalidConfigurationException {

  }

  @Override String key() {
    return "Segment";
  }

  @Override void identify(IdentifyPayload identify) {
    super.identify(identify);
    dispatchEnqueue(identify);
  }

  @Override void group(GroupPayload group) {
    super.group(group);
    dispatchEnqueue(group);
  }

  @Override void track(TrackPayload track) {
    super.track(track);
    dispatchEnqueue(track);
  }

  @Override void alias(AliasPayload alias) {
    super.alias(alias);
    dispatchEnqueue(alias);
  }

  @Override void screen(ScreenPayload screen) {
    super.screen(screen);
    dispatchEnqueue(screen);
  }

  @Override void flush() {
    super.flush();
    dispatchFlush();
  }

  void dispatchEnqueue(final BasePayload payload) {
    handler.sendMessage(handler.obtainMessage(REQUEST_ENQUEUE, payload));
  }

  void performEnqueue(BasePayload payload) {
    try {
      queue.add(payload);
    } catch (Exception e) {
      if (debuggingEnabled) {
        error(OWNER_SEGMENT, VERB_ENQUEUE, payload.id(), e,
            String.format("payload: %s", payload));
      }
    }

    if (debuggingEnabled) {
      debug(OWNER_SEGMENT, VERB_ENQUEUE, payload.id(),
          String.format("queueSize: %s", queue.size()));
    }
    // Check if we've reached the maximum queue size
    if (queue.size() >= queueSize) {
      performFlush();
    }
  }

  void dispatchFlush() {
    handler.sendMessage(handler.obtainMessage(REQUEST_FLUSH));
  }

  void performFlush() {
    if (queue.size() == 0 || !isConnected(context)) {
      rescheduleFlush();
      return;
    }

    final List<BasePayload> payloads = new ArrayList<BasePayload>();
    try {
      queue.setListener(new ObjectQueue.Listener<BasePayload>() {
        @Override public void onAdd(ObjectQueue<BasePayload> queue, BasePayload entry) {
          if (debuggingEnabled) {
            debug(OWNER_SEGMENT, VERB_FLUSH, entry.id(), null);
          }
          payloads.add(entry);
        }

        @Override public void onRemove(ObjectQueue<BasePayload> queue) {

        }
      });
      queue.setListener(null);
    } catch (Exception e) {
      if (debuggingEnabled) {
        error(OWNER_SEGMENT, VERB_FLUSH, "could not read queue", e,
            String.format("queue: %s", queue));
      }
      return;
    }

    int count = payloads.size();
    try {
      segmentHTTPApi.upload(new BatchPayload(payloads, integrations));
      stats.dispatchFlush(count);
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0; i < count; i++) {
        queue.remove();
      }
    } catch (IOException e) {
      if (debuggingEnabled) {
        error(OWNER_SEGMENT, VERB_FLUSH, "unable to clear queue", e, "events: " + count);
      }
    }
    rescheduleFlush();
  }

  private void rescheduleFlush() {
    handler.removeMessages(REQUEST_FLUSH);
    handler.sendMessageDelayed(handler.obtainMessage(REQUEST_FLUSH), flushInterval);
  }

  static class BatchPayload extends JsonMap {
    /**
     * The sent timestamp is an ISO-8601-formatted string that, if present on a message, can be
     * used to correct the original timestamp in situations where the local clock cannot be
     * trusted, for example in our mobile libraries. The sentAt and receivedAt timestamps will be
     * assumed to have occurred at the same time, and therefore the difference is the local clock
     * skew.
     */
    private static final String SENT_AT_KEY = "sentAt";

    /**
     * A dictionary of integration names that the message should be proxied to. 'All' is a special
     * name that applies when no key for a specific integration is found, and is case-insensitive.
     */
    private static final String INTEGRATIONS_KEY = "integrations";

    BatchPayload(List<BasePayload> batch, Map<String, Boolean> integrations) {
      put("batch", batch);
      put(INTEGRATIONS_KEY, integrations);
      put(SENT_AT_KEY, toISO8601Date(new Date()));
    }
  }

  void shutdown() {
    quitThread(segmentThread);
  }

  private static class SegmentHandler extends Handler {
    private final SegmentIntegration segmentIntegration;

    SegmentHandler(Looper looper, SegmentIntegration segmentIntegration) {
      super(looper);
      this.segmentIntegration = segmentIntegration;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case REQUEST_ENQUEUE:
          BasePayload payload = (BasePayload) msg.obj;
          segmentIntegration.performEnqueue(payload);
          break;
        case REQUEST_FLUSH:
          segmentIntegration.performFlush();
          break;
        default:
          panic("Unknown dispatcher message." + msg.what);
      }
    }
  }
}
