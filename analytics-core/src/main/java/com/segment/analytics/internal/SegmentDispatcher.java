package com.segment.analytics.internal;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.JsonWriter;
import com.segment.analytics.internal.model.payloads.BasePayload;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.segment.analytics.Logger.OWNER_SEGMENT;
import static com.segment.analytics.Logger.VERB_ENQUEUE;
import static com.segment.analytics.Utils.isConnected;
import static com.segment.analytics.Utils.isNullOrEmpty;
import static com.segment.analytics.Utils.panic;
import static com.segment.analytics.Utils.quitThread;
import static com.segment.analytics.Utils.toISO8601Date;
import static com.segment.analytics.internal.Utils.createDirectory;
import static com.segment.analytics.internal.Utils.THREAD_PREFIX;
import static com.segment.analytics.internal.Utils.createDirectory;
import static com.segment.analytics.internal.Utils.debug;
import static com.segment.analytics.internal.Utils.error;
import static com.segment.analytics.internal.Utils.print;
import static com.segment.analytics.internal.Utils.OWNER_SEGMENT_DISPATCHER;

/** Entity that queues payloads on disks and uploads them periodically. */
public class SegmentDispatcher {
  /**
   * Drop old payloads if queue contains more than 1000 items. Since each item can be at most
   * 450KB, this bounds the queueFile size to ~450MB (ignoring headers), which also leaves room for
   * QueueFile's 2GB limit.
   */
  static final int MAX_QUEUE_SIZE = 1000;
  private static final Charset UTF_8 = Charset.forName("UTF-8");
  private static final String SEGMENT_THREAD_NAME = THREAD_PREFIX + "Segment";
  /**
   * Our servers only accept payloads < 500KB. This limit is 450kb to account for extra information
   * that is not present in payloads themselves, but is added later, such as {@code sentAt},
   * {@code integrations} and json tokens.
   */
  private static final int MAX_PAYLOAD_SIZE = 450000; // 450kb

  final Context context;
  final QueueFile queueFile;
  final Client client;
  final int flushQueueSize;
  final int flushInterval;
  final Stats stats;
  final Handler handler;
  final HandlerThread segmentThread;
  final boolean debuggingEnabled;
  final Map<String, Boolean> integrations;
  final Cartographer cartographer;

  public static synchronized SegmentDispatcher create(Context context, Client client,
      Cartographer cartographer, Stats stats, Map<String, Boolean> integrations, String tag,
      int flushInterval, int flushQueueSize, boolean debuggingEnabled) {
    QueueFile queueFile;
    try {
      File folder = context.getDir("segment-disk-queue", Context.MODE_PRIVATE);
      String name = tag.replaceAll("[^A-Za-z0-9]", "");
      queueFile = createQueueFile(folder, name);
    } catch (IOException e) {
      throw panic(e, "Could not create queue file.");
    }
    return new SegmentDispatcher(context, client, cartographer, queueFile, stats, integrations,
        flushInterval, flushQueueSize, debuggingEnabled);
  }

  SegmentDispatcher(Context context, Client client, Cartographer cartographer, QueueFile queueFile,
      Stats stats, Map<String, Boolean> integrations, int flushInterval, int flushQueueSize,
      boolean debuggingEnabled) {
    this.context = context;
    this.flushQueueSize = Math.min(flushQueueSize, MAX_QUEUE_SIZE);
    this.client = client;
    this.queueFile = queueFile;
    this.stats = stats;
    this.debuggingEnabled = debuggingEnabled;
    this.integrations = integrations;
    this.cartographer = cartographer;
    this.flushInterval = flushInterval * 1000;

    segmentThread = new HandlerThread(SEGMENT_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
    segmentThread.start();
    handler = new SegmentHandler(segmentThread.getLooper(), this);
    dispatchFlush(flushInterval);
  }

  public void dispatchEnqueue(BasePayload payload) {
    handler.sendMessage(handler.obtainMessage(SegmentHandler.REQUEST_ENQUEUE, payload));
  }

  public void dispatchFlush(int delay) {
    handler.removeMessages(SegmentHandler.REQUEST_FLUSH);
    handler.sendMessageDelayed(handler.obtainMessage(SegmentHandler.REQUEST_FLUSH), delay);
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
      if (debuggingEnabled) {
        debug(OWNER_SEGMENT_DISPATCHER, VERB_ENQUEUE, payload.id(),
            "Queue Size: " + queueFile.size());
      }
      String payloadJson = cartographer.toJson(payload);
      if (isNullOrEmpty(payloadJson) || payloadJson.length() > MAX_PAYLOAD_SIZE) {
        throw new IOException("Could not serialize payload " + payload);
      }
      queueFile.add(payloadJson.getBytes(UTF_8));
    } catch (IOException e) {
      if (debuggingEnabled) {
        error(OWNER_SEGMENT_DISPATCHER, VERB_ENQUEUE, payload.id(), e, payload, queueFile);
      }
    }

    if (queueFile.size() >= flushQueueSize) {
      performFlush();
    }
  }

  void performFlush() {
    if (queueFile.size() < 1 || !isConnected(context)) return;

    try {
      Client.Response response = client.upload();
      BatchPayloadWriter writer = new BatchPayloadWriter(response.os).beginObject()
          .integrations(integrations)
          .beginBatchArray();
      int payloadCount = queueFile.forEach(new PayloadVisitor(writer));
      writer.endBatchArray().endObject().close();
      response.close();

      try {
        queueFile.remove(payloadCount);
      } catch (IOException e) {
        throw panic(e, "Unable to remove payloads from queueFile: " + queueFile);
      }
      stats.dispatchFlush(payloadCount);

      if (queueFile.size() > 0) {
        performFlush();
      } else {
        dispatchFlush(flushInterval);
      }
    } catch (IOException e) {
      dispatchFlush(flushInterval);
    }
  }

  public void shutdown() {
    quitThread(segmentThread);
    try {
      queueFile.close();
    } catch (IOException ignored) {
    }
  }

  static class PayloadVisitor implements QueueFile.ElementVisitor {
    final BatchPayloadWriter writer;
    int size;
    int payloadCount;

    PayloadVisitor(BatchPayloadWriter writer) {
      this.writer = writer;
    }

    @Override public boolean read(InputStream in, int length) throws IOException {
      final int newSize = size + length;
      if (newSize > MAX_PAYLOAD_SIZE) return false;
      size = newSize;
      byte[] data = new byte[length];
      in.read(data, 0, length);
      writer.emitPayloadObject(new String(data, UTF_8));
      payloadCount++;
      return true;
    }
  }

  /** A wrapper that emits a JSON formatted batch payload to the underlying writer. */
  static class BatchPayloadWriter {
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
      /**
       * A dictionary of integration names that the message should be proxied to. 'All' is a special
       * name that applies when no key for a specific integration is found, and is case-insensitive.
       */
      jsonWriter.name("integrations").beginObject();
      for (Map.Entry<String, Boolean> entry : integrations.entrySet()) {
        jsonWriter.name(entry.getKey()).value(entry.getValue());
      }
      jsonWriter.endObject();
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

    void close() throws IOException {
      jsonWriter.close();
    }
  }

  private static class SegmentHandler extends Handler {
    static final int REQUEST_ENQUEUE = 0;
    static final int REQUEST_FLUSH = 1;
    private final SegmentDispatcher segmentDispatcher;

    SegmentHandler(Looper looper, SegmentDispatcher segmentDispatcher) {
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
