package com.segment.analytics;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.JsonWriter;
import com.segment.analytics.integrations.AliasPayload;
import com.segment.analytics.integrations.BasePayload;
import com.segment.analytics.integrations.GroupPayload;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;
import com.segment.analytics.internal.Private;
import com.segment.analytics.internal.Utils.AnalyticsThreadFactory;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.internal.Utils.THREAD_PREFIX;
import static com.segment.analytics.internal.Utils.closeQuietly;
import static com.segment.analytics.internal.Utils.createDirectory;
import static com.segment.analytics.internal.Utils.isConnected;
import static com.segment.analytics.internal.Utils.toISO8601Date;

/** Entity that queues payloads on disks and uploads them periodically. */
class SegmentIntegration extends Integration<Void> {
  static final Integration.Factory FACTORY = new Integration.Factory() {
    @Override public Integration<?> create(ValueMap settings, Analytics analytics) {
      return SegmentIntegration.create(analytics.getApplication(), analytics.client,
          analytics.cartographer, analytics.networkExecutor, analytics.stats,
          Collections.unmodifiableMap(analytics.bundledIntegrations), analytics.tag,
          analytics.flushIntervalInMillis, analytics.flushQueueSize, analytics.getLogger(),
          analytics.crypto);
    }

    @Override public String key() {
      return SEGMENT_KEY;
    }
  };

  /**
   * Drop old payloads if queue contains more than 1000 items. Since each item can be at most
   * 15KB, this bounds the queue size to ~15MB (ignoring headers), which also leaves room for
   * QueueFile's 2GB limit.
   */
  static final int MAX_QUEUE_SIZE = 1000;
  /** Our servers only accept payloads < 15KB. */
  static final int MAX_PAYLOAD_SIZE = 15000; // 15KB.
  /**
   * Our servers only accept batches < 500KB. This limit is 475KB to account for extra data
   * that is not present in payloads themselves, but is added later, such as {@code sentAt},
   * {@code integrations} and other json tokens.
   */
  @Private static final int MAX_BATCH_SIZE = 475000; // 475KB.
  @Private static final Charset UTF_8 = Charset.forName("UTF-8");
  private static final String SEGMENT_THREAD_NAME = THREAD_PREFIX + "SegmentDispatcher";
  static final String SEGMENT_KEY = "Segment.io";
  private final Context context;
  private final PayloadQueue payloadQueue;
  private final Client client;
  private final int flushQueueSize;
  private final Stats stats;
  private final Handler handler;
  private final HandlerThread segmentThread;
  private final Logger logger;
  private final Map<String, Boolean> bundledIntegrations;
  private final Cartographer cartographer;
  private final ExecutorService networkExecutor;
  private final ScheduledExecutorService flushScheduler;
  /**
   * We don't want to stop adding payloads to our disk queue when we're uploading payloads. So we
   * upload payloads on a network executor instead.
   * <p/>
   * Given:
   * 1. Peek returns the oldest elements
   * 2. Writes append to the tail of the queue
   * 3. Methods on QueueFile are synchronized (so only thread can access it at a time)
   * <p/>
   * We offload flushes to the network executor, read the QueueFile and remove entries on it, while
   * we continue to add payloads to the QueueFile on the default Dispatcher thread.
   * <p/>
   * We could end up in a case where (assuming MAX_QUEUE_SIZE is 10):
   * 1. Executor reads 10 payloads from the QueueFile
   * 2. Dispatcher is told to add an payloads (the 11th) to the queue.
   * 3. Dispatcher sees that the queue size is at it's limit (10).
   * 4. Dispatcher removes an payloads.
   * 5. Dispatcher adds a payload.
   * 6. Executor finishes uploading 10 payloads and proceeds to remove 10 elements from the file.
   * Since the dispatcher already removed the 10th element and added a 11th, this would actually
   * delete the 11th payload that will never get uploaded.
   * <p/>
   * This lock is used ensure that the Dispatcher thread doesn't remove payloads when we're
   * uploading.
   */
  @Private final Object flushLock = new Object();
  private final Crypto crypto;

  /**
   * Create a {@link QueueFile} in the given folder with the given name. If the underlying file is
   * somehow corrupted, we'll delete it, and try to recreate the file. This method will throw an
   * {@link IOException} if the directory doesn't exist and could not be created.
   */
  private static QueueFile createQueueFile(File folder, String name) throws IOException {
    createDirectory(folder);
    File file = new File(folder, name);
    try {
      return new QueueFile(file);
    } catch (IOException e) {
      //noinspection ResultOfMethodCallIgnored
      if (file.delete()) {
        return new QueueFile(file);
      } else {
        throw new IOException("Could not create queue file (" + name + ") in " + folder + ".");
      }
    }
  }

  static synchronized SegmentIntegration create(Context context, Client client,
      Cartographer cartographer, ExecutorService networkExecutor, Stats stats,
      Map<String, Boolean> bundledIntegrations, String tag, long flushIntervalInMillis,
      int flushQueueSize, Logger logger, Crypto crypto) {
    PayloadQueue payloadQueue;
    try {
      File folder = context.getDir("segment-disk-queue", Context.MODE_PRIVATE);
      QueueFile queueFile = createQueueFile(folder, tag);
      payloadQueue = new PayloadQueue.PersistentQueue(queueFile);
    } catch (IOException e) {
      logger.error(e, "Falling back to memory queue.");
      payloadQueue = new PayloadQueue.MemoryQueue(new ArrayList<byte[]>());
    }
    return new SegmentIntegration(context, client, cartographer, networkExecutor, payloadQueue,
        stats, bundledIntegrations, flushIntervalInMillis, flushQueueSize, logger, crypto);
  }

  SegmentIntegration(Context context, Client client, Cartographer cartographer,
      ExecutorService networkExecutor, PayloadQueue payloadQueue, Stats stats,
      Map<String, Boolean> bundledIntegrations, long flushIntervalInMillis, int flushQueueSize,
      Logger logger, Crypto crypto) {
    this.context = context;
    this.client = client;
    this.networkExecutor = networkExecutor;
    this.payloadQueue = payloadQueue;
    this.stats = stats;
    this.logger = logger;
    this.bundledIntegrations = bundledIntegrations;
    this.cartographer = cartographer;
    this.flushQueueSize = flushQueueSize;
    this.flushScheduler = Executors.newScheduledThreadPool(1, new AnalyticsThreadFactory());
    this.crypto = crypto;

    segmentThread = new HandlerThread(SEGMENT_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    segmentThread.start();
    handler = new SegmentDispatcherHandler(segmentThread.getLooper(), this);

    long initialDelay = payloadQueue.size() >= flushQueueSize ? 0L : flushIntervalInMillis;
    flushScheduler.scheduleAtFixedRate(new Runnable() {
      @Override public void run() {
        flush();
      }
    }, initialDelay, flushIntervalInMillis, TimeUnit.MILLISECONDS);
  }

  @Override public void identify(IdentifyPayload identify) {
    dispatchEnqueue(identify);
  }

  @Override public void group(GroupPayload group) {
    dispatchEnqueue(group);
  }

  @Override public void track(TrackPayload track) {
    dispatchEnqueue(track);
  }

  @Override public void alias(AliasPayload alias) {
    dispatchEnqueue(alias);
  }

  @Override public void screen(ScreenPayload screen) {
    dispatchEnqueue(screen);
  }

  private void dispatchEnqueue(BasePayload payload) {
    handler.sendMessage(handler.obtainMessage(SegmentDispatcherHandler.REQUEST_ENQUEUE, payload));
  }

  void performEnqueue(BasePayload original) {
    // Override any user provided values with anything that was bundled.
    // e.g. If user did Mixpanel: true and it was bundled, this would correctly override it with
    // false so that the server doesn't send that event as well.
    ValueMap providedIntegrations = original.integrations();
    LinkedHashMap<String, Object> combinedIntegrations =
        new LinkedHashMap<>(providedIntegrations.size() + bundledIntegrations.size());
    combinedIntegrations.putAll(providedIntegrations);
    combinedIntegrations.putAll(bundledIntegrations);
    combinedIntegrations.remove("Segment.io"); // don't include the Segment integration.
    // Make a copy of the payload so we don't mutate the original.
    ValueMap payload = new ValueMap();
    payload.putAll(original);
    payload.put("integrations", combinedIntegrations);

    if (payloadQueue.size() >= MAX_QUEUE_SIZE) {
      synchronized (flushLock) {
        // Double checked locking, the network executor could have removed payload from the queue
        // to bring it below our capacity while we were waiting.
        if (payloadQueue.size() >= MAX_QUEUE_SIZE) {
          logger.info("Queue is at max capacity (%s), removing oldest payload.",
              payloadQueue.size());
          try {
            payloadQueue.remove(1);
          } catch (IOException e) {
            logger.error(e, "Unable to remove oldest payload from queue.");
            return;
          }
        }
      }
    }

    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      OutputStream cos = crypto.encrypt(bos);
      cartographer.toJson(payload, new OutputStreamWriter(cos));
      byte[] bytes = bos.toByteArray();
      if (bytes == null || bytes.length == 0 || bytes.length > MAX_PAYLOAD_SIZE) {
        throw new IOException("Could not serialize payload " + payload);
      }
      payloadQueue.add(bytes);
    } catch (IOException e) {
      logger.error(e, "Could not add payload %s to queue: %s.", payload, payloadQueue);
      return;
    }

    logger.verbose("Enqueued %s payload. %s elements in the queue.", payload, payloadQueue.size());
    if (payloadQueue.size() >= flushQueueSize) {
      submitFlush();
    }
  }

  /** Enqueues a flush message to the handler. */
  @Override public void flush() {
    handler.sendMessage(handler.obtainMessage(SegmentDispatcherHandler.REQUEST_FLUSH));
  }

  /** Submits a flush message to the network executor. */
  void submitFlush() {
    if (!shouldFlush()) {
      return;
    }

    networkExecutor.submit(new Runnable() {
      @Override public void run() {
        synchronized (flushLock) {
          performFlush();
        }
      }
    });
  }

  private boolean shouldFlush() {
    return payloadQueue.size() > 0 && isConnected(context);
  }

  /** Upload payloads to our servers and remove them from the queue file. */
  @Private void performFlush() {
    // Conditions could have changed between enqueuing the task and when it is run.
    if (!shouldFlush()) {
      return;
    }

    logger.verbose("Uploading payloads in queue to Segment.");
    int payloadsUploaded = 0;
    Client.Connection connection = null;
    try {
      // Open a connection.
      connection = client.upload();

      // Write the payloads into the OutputStream.
      BatchPayloadWriter writer = new BatchPayloadWriter(connection.os) //
          .beginObject() //
          .beginBatchArray();
      PayloadWriter payloadWriter = new PayloadWriter(writer, crypto);
      payloadQueue.forEach(payloadWriter);
      writer.endBatchArray().endObject().close();
      // Don't use the result of QueueFiles#forEach, since we may not upload the last element.
      payloadsUploaded = payloadWriter.payloadCount;

      // Upload the payloads.
      connection.close();
    } catch (Client.HTTPException e) {
      if (e.responseCode >= 400 && e.responseCode < 500) {
        // Simply log and proceed to remove the rejected payloads from the queue.
        logger.error(e, "Payloads were rejected by server. Marked for removal.");
      } else {
        logger.error(e, "Error while uploading payloads");
        return;
      }
    } catch (IOException e) {
      logger.error(e, "Error while uploading payloads");
      return;
    } finally {
      closeQuietly(connection);
    }

    try {
      payloadQueue.remove(payloadsUploaded);
    } catch (IOException e) {
      logger.error(e, "Unable to remove " + payloadsUploaded + " payload(s) from queue.");
      return;
    }

    logger.verbose("Uploaded %s payloads. %s remain in the queue.", payloadsUploaded,
        payloadQueue.size());
    stats.dispatchFlush(payloadsUploaded);
    if (payloadQueue.size() > 0) {
      performFlush(); // Flush any remaining items.
    }
  }

  void shutdown() {
    flushScheduler.shutdownNow();
    segmentThread.quit();
    closeQuietly(payloadQueue);
  }

  static class PayloadWriter implements PayloadQueue.ElementVisitor {

    final BatchPayloadWriter writer;
    final Crypto crypto;
    int size;
    int payloadCount;

    PayloadWriter(BatchPayloadWriter writer, Crypto crypto) {
      this.writer = writer;
      this.crypto = crypto;
    }

    @Override public boolean read(InputStream in, int length) throws IOException {
      InputStream is = crypto.decrypt(in);
      final int newSize = size + length;
      if (newSize > MAX_BATCH_SIZE) return false;
      size = newSize;
      byte[] data = new byte[length];
      //noinspection ResultOfMethodCallIgnored
      is.read(data, 0, length);
      writer.emitPayloadObject(new String(data, UTF_8));
      payloadCount++;
      return true;
    }
  }

  /** A wrapper that emits a JSON formatted batch payload to the underlying writer. */
  static class BatchPayloadWriter implements Closeable {

    private final JsonWriter jsonWriter;
    /** Keep around for writing payloads as Strings. */
    private final BufferedWriter bufferedWriter;
    private boolean needsComma = false;

    BatchPayloadWriter(OutputStream stream) {
      bufferedWriter = new BufferedWriter(new OutputStreamWriter(stream));
      jsonWriter = new JsonWriter(bufferedWriter);
    }

    BatchPayloadWriter beginObject() throws IOException {
      jsonWriter.beginObject();
      return this;
    }

    BatchPayloadWriter beginBatchArray() throws IOException {
      jsonWriter.name("batch").beginArray();
      needsComma = false;
      return this;
    }

    BatchPayloadWriter emitPayloadObject(String payload) throws IOException {
      // Payloads already serialized into json when storing on disk. No need to waste cycles
      // deserializing them.
      if (needsComma) {
        bufferedWriter.write(',');
      } else {
        needsComma = true;
      }
      bufferedWriter.write(payload);
      return this;
    }

    BatchPayloadWriter endBatchArray() throws IOException {
      if (!needsComma) {
        throw new IOException("At least one payload must be provided.");
      }
      jsonWriter.endArray();
      return this;
    }

    BatchPayloadWriter endObject() throws IOException {
      /**
       * The sent timestamp is an ISO-8601-formatted string that, if present on a message, can be
       * used to correct the original timestamp in situations where the local clock cannot be
       * trusted, for example in our mobile libraries. The sentAt and receivedAt timestamps will be
       * assumed to have occurred at the same time, and therefore the difference is the local clock
       * skew.
       */
      jsonWriter.name("sentAt").value(toISO8601Date(new Date())).endObject();
      return this;
    }

    @Override public void close() throws IOException {
      jsonWriter.close();
    }
  }

  static class SegmentDispatcherHandler extends Handler {

    static final int REQUEST_FLUSH = 1;
    @Private static final int REQUEST_ENQUEUE = 0;
    private final SegmentIntegration segmentIntegration;

    SegmentDispatcherHandler(Looper looper, SegmentIntegration segmentIntegration) {
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
          segmentIntegration.submitFlush();
          break;
        default:
          throw new AssertionError("Unknown dispatcher message: " + msg.what);
      }
    }
  }
}
