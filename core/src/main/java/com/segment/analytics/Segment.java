package com.segment.analytics;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.Logger.OWNER_SEGMENT;
import static com.segment.analytics.Logger.VERB_ENQUEUE;
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
class Segment {
  private static final Charset UTF_8 = Charset.forName("UTF-8");
  private static final String SEGMENT_THREAD_NAME = Utils.THREAD_PREFIX + "Segment";
  private static final String PAYLOAD_QUEUE_FILE_SUFFIX = "-payload";
  private static final int MAX_FLUSH_BATCH_SIZE = 50;
  private static final int MAX_QUEUE_SIZE = 200;

  final Context context;
  final QueueFile payloadQueueFile;
  final SegmentHTTPApi segmentHTTPApi;
  final int flushQueueSize;
  final int flushInterval;
  final Stats stats;
  final Handler handler;
  final HandlerThread segmentThread;
  final Logger logger;
  final Map<String, Boolean> integrations;

  static synchronized Segment create(Context context, int flushQueueSize, int flushInterval,
      SegmentHTTPApi segmentHTTPApi, Map<String, Boolean> integrations, String tag, Stats stats,
      Logger logger) {
    File parent = context.getFilesDir();
    String filePrefix = tag.replaceAll("[^A-Za-z0-9]", ""); // sanitize input
    QueueFile payloadQueueFile;
    try {
      if (parent.exists() || parent.mkdirs() || parent.isDirectory()) {
        payloadQueueFile = new QueueFile(new File(parent, filePrefix + PAYLOAD_QUEUE_FILE_SUFFIX));
      } else {
        throw new IOException();
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not create disk queue " + filePrefix + " in " + parent, e);
    }
    flushQueueSize = Math.min(flushQueueSize, MAX_FLUSH_BATCH_SIZE);
    return new Segment(context, flushQueueSize, flushInterval, segmentHTTPApi, payloadQueueFile,
        integrations, stats, logger);
  }

  Segment(Context context, int flushQueueSize, int flushInterval, SegmentHTTPApi segmentHTTPApi,
      QueueFile payloadQueueFile, Map<String, Boolean> integrations, Stats stats, Logger logger) {
    this.context = context;
    this.flushQueueSize = flushQueueSize;
    this.segmentHTTPApi = segmentHTTPApi;
    this.payloadQueueFile = payloadQueueFile;
    this.stats = stats;
    this.logger = logger;
    this.integrations = integrations;
    this.flushInterval = flushInterval * 1000;
    segmentThread = new HandlerThread(SEGMENT_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    segmentThread.start();
    handler = new SegmentHandler(segmentThread.getLooper(), this);
    dispatchFlush(flushInterval);
  }

  void dispatchEnqueue(final BasePayload payload) {
    handler.sendMessage(handler.obtainMessage(SegmentHandler.REQUEST_ENQUEUE, payload));
  }

  void performEnqueue(BasePayload payload) {
    final int queueSize = payloadQueueFile.size();
    logger.debug(OWNER_SEGMENT, VERB_ENQUEUE, payload.id(), "queueSize: %s", queueSize);
    if (queueSize > MAX_QUEUE_SIZE) {
      // In normal conditions, we wouldn't have more than flushQueueSize events, which is
      // MAX_FLUSH_BATCH_SIZE = 50 at most. But if the user has been offline for a while, then
      // the number of events could start accumulating. Even though QueueFile has a limit of 2GB,
      // which is more than enough, we shouldn't be using more than a couple of megs here.
      logger.print(null, "queueSize limit reached. Dropping oldest event.");
      try {
        payloadQueueFile.remove();
      } catch (IOException e) {
        panic("could not remove payload from queue.");
      }
    }
    try {
      String json = payload.toString();
      if (isNullOrEmpty(json)) throw new IOException("could not serialize payload " + payload);
      payloadQueueFile.add(json.getBytes(UTF_8));
    } catch (IOException e) {
      logger.error(OWNER_SEGMENT, VERB_ENQUEUE, payload.id(), e, "payload: %s", payload);
      return;
    }
    // Check if we've reached the maximum queue size
    if (payloadQueueFile.size() >= flushQueueSize) {
      performFlush();
    }
  }

  void dispatchFlush(int delay) {
    handler.removeMessages(SegmentHandler.REQUEST_FLUSH);
    handler.sendMessageDelayed(handler.obtainMessage(SegmentHandler.REQUEST_FLUSH), delay);
  }

  void performFlush() {
    final int queueSize = Math.min(payloadQueueFile.size(), MAX_FLUSH_BATCH_SIZE);
    boolean error = false;

    if (queueSize != 0 && isConnected(context)) {
      final List<BasePayload> payloads = new ArrayList<BasePayload>(queueSize);
      for (int i = 0; i < queueSize; i++) {
        try {
          byte[] bytes = payloadQueueFile.peek();
          String json = new String(bytes, UTF_8);
          if (isNullOrEmpty(json)) throw new IOException("Cannot deserialize payload.");
          payloads.add(new BasePayload(json));
        } catch (IOException e) {
          logger.print(e, "Could not read payload. Skipping.");
        }
        try {
          payloadQueueFile.remove();
        } catch (IOException e) {
          panic("could not remove payload from queue.");
        }
      }
      try {
        segmentHTTPApi.upload(new BatchPayload(payloads, integrations));
        stats.dispatchFlush(queueSize);
      } catch (IOException e) {
        logger.print(e, "Unable to flush messages. Requeuing %s events.", queueSize);
        for (int i = 0; i < payloads.size(); i++) {
          performEnqueue(payloads.get(i));
        }
        error = true;
      }
    }

    if (!error && payloadQueueFile.size() > 0) {
      performFlush();
    } else {
      dispatchFlush(flushInterval);
    }
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
    static final int REQUEST_ENQUEUE = 0;
    static final int REQUEST_FLUSH = 1;
    private final Segment segment;

    SegmentHandler(Looper looper, Segment segment) {
      super(looper);
      this.segment = segment;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case REQUEST_ENQUEUE:
          BasePayload payload = (BasePayload) msg.obj;
          segment.performEnqueue(payload);
          break;
        case REQUEST_FLUSH:
          segment.performFlush();
          break;
        default:
          panic("Unknown dispatcher message." + msg.what);
      }
    }
  }
}
