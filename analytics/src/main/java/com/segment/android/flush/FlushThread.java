package com.segment.android.flush;

import android.os.Handler;
import android.util.Log;
import com.segment.android.Analytics;
import com.segment.android.Logger;
import com.segment.android.db.IPayloadDatabaseLayer;
import com.segment.android.db.IPayloadDatabaseLayer.PayloadCallback;
import com.segment.android.db.IPayloadDatabaseLayer.RemoveCallback;
import com.segment.android.models.BasePayload;
import com.segment.android.models.Batch;
import com.segment.android.request.IRequestLayer;
import com.segment.android.request.IRequestLayer.RequestCallback;
import com.segment.android.stats.AnalyticsStatistics;
import com.segment.android.utils.LooperThreadWithHandler;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * A Looper/Handler backed flushing thread
 */
public class FlushThread extends LooperThreadWithHandler implements IFlushLayer {

  /**
   * A factory to create a batch around a list of payload actions
   */
  public interface BatchFactory {
    Batch create(List<BasePayload> payloads);
  }

  private IRequestLayer requestLayer;
  private IPayloadDatabaseLayer databaseLayer;
  private BatchFactory batchFactory;

  public FlushThread(IPayloadDatabaseLayer databaseLayer, BatchFactory batchFactory,
      IRequestLayer requestLayer) {

    this.requestLayer = requestLayer;
    this.batchFactory = batchFactory;
    this.databaseLayer = databaseLayer;
  }

  /**
   * Request a flush of data to the server. This method does not block, but asks for
   * a flush to happen.
   *
   * A flush consists of several operations - specifically getting messages from the database,
   * making a request to the server, and deleting the sent messages from the local database.
   *
   * If there's anything left in the queue, another flush will be asked for.
   */
  public void flush(final FlushCallback callback) {
    Handler handler = handler();
    handler.post(new Runnable() {
      @Override
      public void run() {
        // we're on the context of the flushing thread

        // we want to wait until the flush completes
        final CountDownLatch latch = new CountDownLatch(1);
        Logger.d("Starting flush operation ..");
        flushNextPayload(new FlushCallback() {
          @Override
          public void onFlushCompleted(boolean success, Batch batch) {
            if (success && batch.getBatch().size() > 0) {
              // we successfully sent a batch to the server, and we might
              // have more to send, so lets trigger another flush
              flush(null);
            }

            // we're done with this flush operation
            latch.countDown();

            if (success) {
              Logger.d("Flush op success. [" + batch.getBatch().size() + " items]");
            } else {
              Logger.w("Flush op failed. [" + batch.getBatch().size() + " items]");
            }

            if (callback != null) callback.onFlushCompleted(success, batch);
          }
        });

        // okay, lets block until the flush completed. this is important because
        // we don't want two flushes happening simultaneously
        try {
          latch.await();
        } catch (InterruptedException e) {
          Logger.w("Failed to wait for flush to finish. " + Log.getStackTraceString(e));
        }
      }
    });
  }

  /**
   * Flushes the next payload asynchronously.
   */
  private void flushNextPayload(final FlushCallback callback) {

    // ask the database for the next payload batch
    databaseLayer.nextPayload(new PayloadCallback() {

      @Override
      public void onPayload(final long minId, final long maxId, final List<BasePayload> payloads) {

        // we are currently executing on the database
        // thread so we're still technically locking the
        // database
        final String range = "[" + minId + " - " + maxId + "]";

        // let's frame the actions with a the batch
        final Batch batch = batchFactory.create(payloads);

        if (payloads.size() == 0) {
          // there is nothing to flush, we're done
          if (callback != null) callback.onFlushCompleted(true, batch);
        } else {
          Logger.i("Sending batch to the servers .. " + range);
          // now let's make a request on the flushing thread
          requestLayer.send(batch, new RequestCallback() {

            @Override
            public void onRequestCompleted(boolean success) {
              // we are now executing in the context of the request thread

              if (!success) {
                Logger.w("Failed to batch to the servers .. " + range);
                // if we failed at flushing (connectivity issues), return
                if (callback != null) callback.onFlushCompleted(false, batch);
              } else {
                Logger.d("Successfully sent batch to the server." + range);
                Logger.d("Removing flushed items from the db  .. " + range);
                // if we were successful, we need to first delete the old items from the
                // database, and then continue flushing
                databaseLayer.removePayloads(minId, maxId, new RemoveCallback() {

                  @Override
                  public void onRemoved(int removed) {
                    // we are again executing in the context of the database thread
                    AnalyticsStatistics statistics = Analytics.getStatistics();

                    if (removed == -1) {

                      for (int i = 0; i < removed; i += 1)
                        statistics.updateFailed(1);

                      Logger.e("We failed to remove payload from the database." + range);

                      if (callback != null) callback.onFlushCompleted(false, batch);
                    } else if (removed == 0) {

                      for (int i = 0; i < removed; i += 1)
                        statistics.updateFailed(1);

                      Logger.e("We didn't end up removing anything from the database. " + range);

                      if (callback != null) callback.onFlushCompleted(false, batch);
                    } else {

                      for (int i = 0; i < removed; i += 1)
                        statistics.updateSuccessful(1);

                      Logger.i("Successfully removed items from the flush db." + range);

                      if (callback != null) callback.onFlushCompleted(true, batch);
                    }
                  }
                });
              }
            }
          });
        }
      }
    });
  }
}