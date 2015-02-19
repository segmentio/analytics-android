package com.segment.analytics;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.JsonWriter;
import com.segment.analytics.internal.model.payloads.BasePayload;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
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
import static com.segment.analytics.internal.Utils.OWNER_SEGMENT_DISPATCHER;
import static com.segment.analytics.internal.Utils.THREAD_PREFIX;
import static com.segment.analytics.internal.Utils.VERB_ENQUEUE;
import static com.segment.analytics.internal.Utils.VERB_FLUSH;
import static com.segment.analytics.internal.Utils.closeQuietly;
import static com.segment.analytics.internal.Utils.createDirectory;
import static com.segment.analytics.internal.Utils.debug;
import static com.segment.analytics.internal.Utils.error;
import static com.segment.analytics.internal.Utils.isConnected;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.panic;
import static com.segment.analytics.internal.Utils.toISO8601Date;

/** Entity that queues payloads on disks and uploads them periodically. */
class SegmentDispatcher {
  /**
   * Drop old payloads if queue contains more than 1000 items. Since each item can be at most
   * 450KB, this bounds the queueFile size to ~450MB (ignoring headers), which also leaves room for
   * QueueFile's 2GB limit.
   */
  static final int MAX_QUEUE_SIZE = 1000;
  private static final Charset UTF_8 = Charset.forName("UTF-8");
  private static final String SEGMENT_THREAD_NAME = THREAD_PREFIX + OWNER_SEGMENT_DISPATCHER;
  /**
   * Our servers only accept payloads < 500KB. This limit is 450kb to account for extra data
   * that is not present in payloads themselves, but is added later, such as {@code sentAt},
   * {@code integrations} and json tokens.
   */
  private static final int MAX_PAYLOAD_SIZE = 450000; // 450kb

  final Context context;
  final QueueFile queueFile;
  final Client client;
  final long flushIntervalInMillis;
  final int flushQueueSize;
  final Stats stats;
  final Handler handler;
  final HandlerThread segmentThread;
  final Analytics.LogLevel logLevel;
  final Map<String, Boolean> integrations;
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
      Map<String, Boolean> integrations, String tag, long flushIntervalInMillis, int flushQueueSize,
      Analytics.LogLevel logLevel) {
    QueueFile queueFile;
    try {
      File folder = context.getDir("segment-disk-queue", Context.MODE_PRIVATE);
      queueFile = createQueueFile(folder, tag);
    } catch (IOException e) {
      throw panic(e, "Could not create queue file.");
    }
    return new SegmentDispatcher(context, client, cartographer, networkExecutor, queueFile, stats,
        integrations, flushIntervalInMillis, flushQueueSize, logLevel);
  }

  SegmentDispatcher(Context context, Client client, Cartographer cartographer,
      ExecutorService networkExecutor, QueueFile queueFile, Stats stats,
      Map<String, Boolean> integrations, long flushIntervalInMillis, int flushQueueSize,
      Analytics.LogLevel logLevel) {
    this.context = context;
    this.client = client;
    this.networkExecutor = networkExecutor;
    this.queueFile = queueFile;
    this.stats = stats;
    this.logLevel = logLevel;
    this.integrations = integrations;
    this.cartographer = cartographer;
    this.flushIntervalInMillis = flushIntervalInMillis;
    this.flushQueueSize = flushQueueSize;

    segmentThread = new HandlerThread(SEGMENT_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    segmentThread.start();
    handler = new SegmentDispatcherHandler(segmentThread.getLooper(), this);

    if (queueFile.size() >= flushQueueSize) {
      dispatchFlush(0);
    } else {
      dispatchFlush(flushIntervalInMillis);
    }
  }

  void dispatchEnqueue(BasePayload payload) {
    handler.sendMessage(handler.obtainMessage(SegmentDispatcherHandler.REQUEST_ENQUEUE, payload));
  }

  void dispatchFlush(long delay) {
    handler.removeMessages(SegmentDispatcherHandler.REQUEST_FLUSH);
    handler.sendMessageDelayed(handler.obtainMessage(SegmentDispatcherHandler.REQUEST_FLUSH),
        delay);
  }

  void performEnqueue(BasePayload payload) {
    if (queueFile.size() >= MAX_QUEUE_SIZE) {
      try {
        queueFile.remove();
      } catch (IOException e) {
        throw panic(e, "Could not remove payload from queue.");
      }
    }

    try {
      if (logLevel.log()) {
        debug(OWNER_SEGMENT_DISPATCHER, VERB_ENQUEUE, payload.messageId(),
            "Queue Size: " + queueFile.size());
      }
      String payloadJson = cartographer.toJson(payload);
      if (isNullOrEmpty(payloadJson) || payloadJson.length() > MAX_PAYLOAD_SIZE) {
        throw new IOException("Could not serialize payload " + payload);
      }
      queueFile.add(payloadJson.getBytes(UTF_8));
    } catch (IOException e) {
      if (logLevel.log()) {
        error(OWNER_SEGMENT_DISPATCHER, VERB_ENQUEUE, payload.messageId(), e, payload, queueFile);
      }
    }

    if (queueFile.size() >= flushQueueSize) {
      performFlush();
    }
  }

  private int flush() throws IOException {
    Client.Connection connection;
    try {
      // Open a connection.
      connection = client.upload();
    } catch (IOException e) {
      if (logLevel.log()) {
        error(OWNER_SEGMENT_DISPATCHER, VERB_FLUSH, null, e, "Could not open connection");
      }
      throw e;
    }

    int payloadsUploaded;
    try {
      // Write the payloads into the OutputStream.
      BatchPayloadWriter writer = new BatchPayloadWriter(connection.os).beginObject()
          .integrations(integrations)
          .beginBatchArray();
      PayloadWriter payloadWriter = new PayloadWriter(writer);
      queueFile.forEach(payloadWriter);
      writer.endBatchArray().endObject().close();
      // Don't use the result of QueueFiles#forEach, since we may or may not read the last element.
      payloadsUploaded = payloadWriter.payloadCount;
    } catch (IOException e) {
      closeQuietly(connection);
      if (logLevel.log()) {
        error(OWNER_SEGMENT_DISPATCHER, VERB_FLUSH, null, e, queueFile);
      }
      throw e;
    }

    try {
      // Upload the payloads.
      connection.close();
    } catch (Client.UploadException e) {
      // simply log proceed to remove the rejected payloads from the queue
      if (logLevel.log()) {
        error(OWNER_SEGMENT_DISPATCHER, VERB_FLUSH, null, e, "Could not upload payloads");
      }
    } catch (IOException e) {
      if (logLevel.log()) {
        error(OWNER_SEGMENT_DISPATCHER, VERB_FLUSH, null, e, "Could not upload payloads",
            queueFile);
      }
      throw e;
    }

    return payloadsUploaded;
  }

  void performFlush() {
    if (queueFile.size() < 1 || !isConnected(context)) {
      dispatchFlush(flushIntervalInMillis);
      return;
    }

    try {
      int payloadsUploaded = networkExecutor.submit(new Callable<Integer>() {
        @Override public Integer call() throws Exception {
          return flush();
        }
      }).get();

      try {
        queueFile.remove(payloadsUploaded);
      } catch (IOException e) {
        throw panic(e, "Unable to remove payloads from queueFile: " + queueFile);
      }

      if (queueFile.size() > 0) {
        performFlush(); // Flush any remaining items.
      } else {
        dispatchFlush(flushIntervalInMillis);
      }
    } catch (InterruptedException e) {
      if (logLevel.log()) {
        error(OWNER_SEGMENT_DISPATCHER, VERB_FLUSH, null, e,
            "Interrupted while waiting for flush.");
      }
    } catch (ExecutionException e) {
      if (logLevel.log()) {
        error(OWNER_SEGMENT_DISPATCHER, VERB_FLUSH, null, e, "Could not upload payloads.");
      }
      dispatchFlush(flushIntervalInMillis);
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
      if (newSize > MAX_PAYLOAD_SIZE) return false;
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
          panic("Unknown dispatcher message: " + msg.what);
      }
    }
  }
}
