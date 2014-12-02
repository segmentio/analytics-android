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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.Logger.OWNER_SEGMENT_INTEGRATION;
import static com.segment.analytics.Logger.VERB_ENQUEUE;
import static com.segment.analytics.Logger.VERB_FLUSH;
import static com.segment.analytics.Utils.isConnected;
import static com.segment.analytics.Utils.isNullOrEmpty;
import static com.segment.analytics.Utils.panic;
import static com.segment.analytics.Utils.quitThread;
import static com.segment.analytics.Utils.toISO8601Date;

/**
 * The actual service that posts data to Segment's servers.
 *
 * @since 2.3
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
  final Logger logger;
  final Map<String, Boolean> integrations;

  SegmentIntegration(Context context, int queueSize, int flushInterval,
      SegmentHTTPApi segmentHTTPApi, ObjectQueue<BasePayload> queue,
      Map<String, Boolean> integrations, Stats stats, Logger logger) {
    this.context = context;
    this.queueSize = queueSize;
    this.segmentHTTPApi = segmentHTTPApi;
    this.queue = queue;
    this.stats = stats;
    this.logger = logger;
    this.integrations = integrations;
    this.flushInterval = flushInterval * 1000;
    segmentThread = new HandlerThread(SEGMENT_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    segmentThread.start();
    handler = new SegmentHandler(segmentThread.getLooper(), this);
    rescheduleFlush();
  }

  static synchronized SegmentIntegration create(Context context, int queueSize, int flushInterval,
      SegmentHTTPApi segmentHTTPApi, Map<String, Boolean> integrations, String tag, Stats stats,
      Logger logger) {
    File parent = context.getFilesDir();
    ObjectQueue<BasePayload> queue;
    try {
      if (!parent.exists()) parent.mkdirs();
      File queueFile = new File(parent, TASK_QUEUE_FILE_NAME + tag);
      queue = new FileObjectQueue<BasePayload>(queueFile, new PayloadConverter());
    } catch (IOException e) {
      logger.error(OWNER_SEGMENT_INTEGRATION, Logger.VERB_INITIALIZE, null, e,
          "Unable to initialize disk queue with tag %s in directory %s,"
              + "falling back to memory queue.", tag, parent.getAbsolutePath());
      queue = new InMemoryObjectQueue<BasePayload>();
    }
    return new SegmentIntegration(context, queueSize, flushInterval, segmentHTTPApi, queue,
        integrations, stats, logger);
  }

  @Override void initialize(Context context, JsonMap settings, boolean debuggingEnabled)
      throws IllegalStateException {
    // ignored
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
    logger.debug(OWNER_SEGMENT_INTEGRATION, VERB_ENQUEUE, payload.id(), "queueSize: %s",
        queue.size());
    try {
      queue.add(payload);
    } catch (Exception e) {
      logger.error(OWNER_SEGMENT_INTEGRATION, VERB_ENQUEUE, payload.id(), e, "payload: %s",
          payload);
      return;
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
    if (queue.size() != 0 && isConnected(context)) {
      boolean batch = true;
      final List<BasePayload> payloads = new ArrayList<BasePayload>(queue.size());
      try {
        queue.setListener(new ObjectQueue.Listener<BasePayload>() {
          @Override public void onAdd(ObjectQueue<BasePayload> queue, BasePayload entry) {
            logger.debug(OWNER_SEGMENT_INTEGRATION, VERB_FLUSH, entry.id(), null);
            payloads.add(entry);
          }

          @Override public void onRemove(ObjectQueue<BasePayload> queue) {

          }
        });
        queue.setListener(null);
      } catch (Exception e) {
        logger.error(OWNER_SEGMENT_INTEGRATION, VERB_FLUSH,
            "Could not read queue. Flushing messages individually.", e,
            String.format("queue: %s", queue));
        batch = false;
      }

      if (batch) {
        int count = payloads.size();
        try {
          segmentHTTPApi.upload(new BatchPayload(payloads, integrations));
          stats.dispatchFlush(count);
          //noinspection ForLoopReplaceableByForEach
          for (int i = 0; i < count; i++) {
            queue.remove();
          }
        } catch (IOException e) {
          logger.error(OWNER_SEGMENT_INTEGRATION, VERB_FLUSH, "Unable to flush messages.", e,
              "events: %s", count);
        }
      } else {
        // There was an error reading the queue, so we'll try to flush the payloads one at a time
        while (queue.size() > 0) {
          try {
            BasePayload payload = queue.peek();
            logger.debug(OWNER_SEGMENT_INTEGRATION, VERB_FLUSH, payload.id(), null);
            try {
              segmentHTTPApi.upload(
                  new BatchPayload(Collections.singletonList(payload), integrations));
              stats.dispatchFlush(1);
              queue.remove();
            } catch (IOException e) {
              logger.error(OWNER_SEGMENT_INTEGRATION, VERB_FLUSH, "Unable to flush payload.", e,
                  "payload: %s", payload);
            }
          } catch (Exception e) {
            // This is an unrecoverable error, we can't read the entry from disk
            logger.error(OWNER_SEGMENT_INTEGRATION, VERB_FLUSH, "Unable to read payload.", e,
                "queue: %s", queue);
            queue.remove();
          }
        }
      }
    }
    rescheduleFlush();
  }

  private void rescheduleFlush() {
    handler.removeMessages(REQUEST_FLUSH);
    handler.sendMessageDelayed(handler.obtainMessage(REQUEST_FLUSH), flushInterval);
  }

  void shutdown() {
    quitThread(segmentThread);
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

  static class PayloadConverter implements FileObjectQueue.Converter<BasePayload> {
    static final Charset UTF_8 = Charset.forName("UTF-8");

    @Override public BasePayload from(byte[] bytes) throws IOException {
      String json = new String(bytes, UTF_8);
      if (isNullOrEmpty(json)) {
        throw new IOException("Cannot deserialize payload from empty byte array.");
      }
      return new BasePayload(json);
    }

    @Override public void toStream(BasePayload payload, OutputStream bytes) throws IOException {
      String json = payload.toString();
      if (isNullOrEmpty(json)) {
        throw new IOException("Cannot serialize payload : " + payload);
      }
      OutputStreamWriter outputStreamWriter = new OutputStreamWriter(bytes, UTF_8);
      outputStreamWriter.write(json);
      outputStreamWriter.close();
    }
  }
}
