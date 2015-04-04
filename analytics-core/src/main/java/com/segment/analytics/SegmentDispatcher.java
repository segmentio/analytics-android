package com.segment.analytics;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.JsonWriter;
import com.segment.analytics.internal.AbstractIntegration;
import com.segment.analytics.internal.model.payloads.AliasPayload;
import com.segment.analytics.internal.model.payloads.BasePayload;
import com.segment.analytics.internal.model.payloads.GroupPayload;
import com.segment.analytics.internal.model.payloads.IdentifyPayload;
import com.segment.analytics.internal.model.payloads.ScreenPayload;
import com.segment.analytics.internal.model.payloads.TrackPayload;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.internal.Utils.THREAD_PREFIX;
import static com.segment.analytics.internal.Utils.closeQuietly;
import static com.segment.analytics.internal.Utils.createDirectory;
import static com.segment.analytics.internal.Utils.debug;
import static com.segment.analytics.internal.Utils.error;
import static com.segment.analytics.internal.Utils.isConnected;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.toISO8601Date;

/** Entity that queues payloads on disks and uploads them periodically. */
class SegmentDispatcher extends AbstractIntegration {

  static final String SEGMENT_KEY = "Segment.io";

  /**
   * Drop old payloads if queue contains more than 1000 items. Since each item can be at most
   * 450KB, this bounds the queueFile size to ~450MB (ignoring headers), which also leaves room for
   * QueueFile's 2GB limit.
   */
  static final int MAX_QUEUE_SIZE = 1000;
  /** Our servers only accept payloads < 15kb. */
  static final int MAX_PAYLOAD_SIZE = 15000; // 15kb
  private static final Charset UTF_8 = Charset.forName("UTF-8");
  private static final String SEGMENT_THREAD_NAME = THREAD_PREFIX + "SegmentDispatcher";
  /**
   * Our servers only accept batches < 500KB. This limit is 475kb to account for extra data
   * that is not present in payloads themselves, but is added later, such as {@code sentAt},
   * {@code integrations} and json tokens.
   */
  private static final int MAX_BATCH_SIZE = 475000; // 475kb
  final Context context;
  final QueueFile queueFile;
  final Client client;
  final long flushIntervalInMillis;
  final int flushQueueSize;
  final Stats stats;
  final Handler handler;
  final HandlerThread segmentThread;
  final Analytics.LogLevel logLevel;
  final Map<String, Boolean> bundledIntegrations;
  final Cartographer cartographer;
  private final ExecutorService networkExecutor;

  /**
   * Create a {@link QueueFile} in the given folder with the given name. This method will throw an
   * {@link IOException} if the directory doesn't exist and could not be created. If the underlying
   * file is somehow corrupted, we'll delete it, and try to recreate the file.
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

  static synchronized SegmentDispatcher create(Context context, Client client,
      Cartographer cartographer, ExecutorService networkExecutor, Stats stats,
      Map<String, Boolean> bundledIntegrations, String tag, long flushIntervalInMillis,
      int flushQueueSize, Analytics.LogLevel logLevel) {
    QueueFile queueFile;
    try {
      File folder = context.getDir("segment-disk-queue", Context.MODE_PRIVATE);
      queueFile = createQueueFile(folder, tag);
    } catch (IOException e) {
      throw new IOError(e);
    }
    return new SegmentDispatcher(context, client, cartographer, networkExecutor, queueFile, stats,
        bundledIntegrations, flushIntervalInMillis, flushQueueSize, logLevel);
  }

  SegmentDispatcher(Context context, Client client, Cartographer cartographer,
      ExecutorService networkExecutor, QueueFile queueFile, Stats stats,
      Map<String, Boolean> bundledIntegrations, long flushIntervalInMillis, int flushQueueSize,
      Analytics.LogLevel logLevel) {
    this.context = context;
    this.client = client;
    this.networkExecutor = networkExecutor;
    this.queueFile = queueFile;
    this.stats = stats;
    this.logLevel = logLevel;
    this.bundledIntegrations = bundledIntegrations;
    this.cartographer = cartographer;
    this.flushIntervalInMillis = flushIntervalInMillis;
    this.flushQueueSize = flushQueueSize;

    segmentThread = new HandlerThread(SEGMENT_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    segmentThread.start();
    handler = new SegmentDispatcherHandler(segmentThread.getLooper(), this);

    if (queueFile.size() >= flushQueueSize) {
      flush();
    } else {
      scheduleFlush();
    }
  }

  @Override public void initialize(Context context, ValueMap settings, Analytics.LogLevel logLevel)
      throws IllegalStateException {
    // no-op
  }

  @Override public String key() {
    return SEGMENT_KEY;
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

  void performEnqueue(BasePayload payload) {
    if (queueFile.size() >= MAX_QUEUE_SIZE) {
      if (logLevel.log()) {
        debug("Queue is at max capacity (%s), removing oldest event.", queueFile.size());
      }
      try {
        queueFile.remove();
      } catch (IOException e) {
        throw new IOError(e);
      }
    }

    if (logLevel.log()) {
      debug("Enqueuing %s payload. Queue size is : %s.", payload, queueFile.size());
    }

    try {
      String payloadJson = cartographer.toJson(payload);
      if (isNullOrEmpty(payloadJson) || payloadJson.length() > MAX_PAYLOAD_SIZE) {
        throw new IOException("Could not serialize payload " + payload);
      }
      queueFile.add(payloadJson.getBytes(UTF_8));
    } catch (IOException e) {
      if (logLevel.log()) {
        error(e, "Could not add payload %s to queue: %s.", payload, queueFile);
      }
    }

    if (queueFile.size() >= flushQueueSize) {
      if (logLevel.log()) {
        debug("Queue size (%s) has triggered flush.", payload, queueFile.size());
      }
      performFlush();
    }
  }

  @Override public void flush() {
    handler.sendMessage(handler.obtainMessage(SegmentDispatcherHandler.REQUEST_FLUSH));
  }

  void scheduleFlush() {
    handler.sendMessageDelayed(handler.obtainMessage(SegmentDispatcherHandler.REQUEST_FLUSH),
        flushIntervalInMillis);
  }

  private int upload() throws IOException {
    Client.Connection connection = null;
    try {
      // Open a connection.
      connection = client.upload();

      // Write the payloads into the OutputStream.
      BatchPayloadWriter writer = new BatchPayloadWriter(connection.os).beginObject()
          .integrations(bundledIntegrations)
          .beginBatchArray();
      PayloadWriter payloadWriter = new PayloadWriter(writer);
      queueFile.forEach(payloadWriter);
      writer.endBatchArray().endObject().close();
      // Don't use the result of QueueFiles#forEach, since we may not read the last element.
      int payloadsUploaded = payloadWriter.payloadCount;

      try {
        // Upload the payloads.
        connection.close();
      } catch (Client.UploadException e) {
        // Simply log and proceed to remove the rejected payloads from the queue
        if (logLevel.log()) {
          error(e, "Payloads were rejected by server.");
        }
      }
      return payloadsUploaded;
    } finally {
      closeQuietly(connection);
    }
  }

  void performFlush() {
    if (queueFile.size() < 1 || !isConnected(context)) {
      scheduleFlush();
      return;
    }

    try {
      int payloadsUploaded = networkExecutor.submit(new Callable<Integer>() {
        @Override public Integer call() throws Exception {
          return upload();
        }
      }).get();

      try {
        queueFile.remove(payloadsUploaded);
      } catch (IOException e) {
        IOException ioException = new IOException("Unable to remove " //
            + payloadsUploaded + " payload(s) from queueFile: " + queueFile, e);
        throw new IOError(ioException);
      }

      if (queueFile.size() > 0) {
        performFlush(); // Flush any remaining items.
      } else {
        scheduleFlush();
      }
    } catch (InterruptedException e) {
      if (logLevel.log()) {
        error(e, "Thread interrupted while waiting for flush.");
      }
    } catch (ExecutionException e) {
      if (logLevel.log()) {
        error(e, "Could not upload payloads.");
      }
      scheduleFlush();
    }
  }

  void shutdown() {
    segmentThread.quit();
    closeQuietly(queueFile);
  }

  static class PayloadWriter implements QueueFile.ElementVisitor {

    final BatchPayloadWriter writer;
    int size;
    int payloadCount;

    PayloadWriter(BatchPayloadWriter writer) {
      this.writer = writer;
    }

    @Override public boolean read(InputStream in, int length) throws IOException {
      final int newSize = size + length;
      if (newSize > MAX_BATCH_SIZE) return false;
      size = newSize;
      byte[] data = new byte[length];
      //noinspection ResultOfMethodCallIgnored
      in.read(data, 0, length);
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

    BatchPayloadWriter integrations(Map<String, Boolean> integrations) throws IOException {
      if (!isNullOrEmpty(integrations)) {
        jsonWriter.name("integrations").beginObject();
        for (Map.Entry<String, Boolean> entry : integrations.entrySet()) {
          jsonWriter.name(entry.getKey()).value(entry.getValue());
        }
        jsonWriter.endObject();
      }
      return this;
    }

    BatchPayloadWriter beginBatchArray() throws IOException {
      jsonWriter.name("batch").beginArray();
      needsComma = false;
      return this;
    }

    BatchPayloadWriter emitPayloadObject(String payload) throws IOException {
      // Payloads already serialized into json when storing on disk. No need to waste cycles
      // deserializing them
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

    public void close() throws IOException {
      jsonWriter.close();
    }
  }

  static class SegmentDispatcherHandler extends Handler {

    static final int REQUEST_FLUSH = 1;
    private static final int REQUEST_ENQUEUE = 0;
    private final SegmentDispatcher segmentDispatcher;

    SegmentDispatcherHandler(Looper looper, SegmentDispatcher segmentDispatcher) {
      super(looper);
      this.segmentDispatcher = segmentDispatcher;
    }

    @Override public void handleMessage(final Message msg) {
      switch (msg.what) {
        case REQUEST_ENQUEUE:
          BasePayload payload = (BasePayload) msg.obj;
          segmentDispatcher.performEnqueue(payload);
          break;
        case REQUEST_FLUSH:
          segmentDispatcher.performFlush();
          break;
        default:
          throw new AssertionError("Unknown dispatcher message: " + msg.what);
      }
    }
  }
}
