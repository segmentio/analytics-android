package com.segment.analytics;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

import com.segment.analytics.internal.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by kellyfj on 5/8/15.
 */
public class QueueFileStressTest extends AndroidTestCase {

    protected Context context;
    protected static final String writeKey = "q1r1kn4a44";
    private static Analytics analyticsInstance;
    private static boolean firstTimeThrough = true;

    @Override
    protected void setUp() {
        //delegates to the given context, but performs database and
        // file operations with a renamed database/file
        // name (prefixes default names with a given prefix).
        context = new RenamingDelegatingContext(getContext(), "test_");
        Analytics.Builder builder = new Analytics.Builder(context, writeKey);
        analyticsInstance = builder.build();

        if(firstTimeThrough) {
            sleepUntilSettingsLoaded();
            firstTimeThrough = false;
        }
    }

    @Override
    public void tearDown() throws IOException {
        //Make sure any extra elements left over are flushed
        analyticsInstance.flush();
    }

    public void testQueueFileUnderStress_track() {

        Analytics.Builder builder = new Analytics.Builder(context, writeKey);
        analyticsInstance = builder.logLevel(Analytics.LogLevel.VERBOSE).build();


        int maxQueueSize = SegmentDispatcher.MAX_QUEUE_SIZE;
        int aboveTheMax = 100;
        int veryLargeFlushSize = maxQueueSize + aboveTheMax;


        //WHEN we create enough events to cause a flush
        List<Integer> idList = new ArrayList<Integer>();
        //Avoid hitting the FLush Size
        for (int i = 1; i <= veryLargeFlushSize - 1; i++) {
            long id = System.currentTimeMillis();
            analyticsInstance.track("FJK QueueFileStressTest - testQueueFileUnderStress_track - " + i + " of " + veryLargeFlushSize + " ID:[" + id + "]", null, null);
        }

        //analyticsInstance.flush();

        sleepUntilFlushTimerExpires();
    }

    public void testQueueFileUnderStress_screen() {

        Analytics.Builder builder = new Analytics.Builder(context, writeKey);
        analyticsInstance = builder.build();


        int maxQueueSize = SegmentDispatcher.MAX_QUEUE_SIZE;
        int aboveTheMax = 100;
        int veryLargeFlushSize = maxQueueSize + aboveTheMax;


        //WHEN we create enough events to cause a flush
        List<Integer> idList = new ArrayList<Integer>();
        //Avoid hitting the FLush Size
        for (int i = 1; i <= veryLargeFlushSize - 1; i++) {
            long id = System.currentTimeMillis();
            analyticsInstance.screen("FJK QueueFileStressTest - testQueueFileUnderStress_screen - " + i + " of " + veryLargeFlushSize + " ID:[" + id + "]", null);
        }

        //analyticsInstance.flush();

        sleepUntilFlushTimerExpires();
    }

    public void sleepUntilFlushTimerExpires() {
        try {
            Thread.sleep(Utils.DEFAULT_FLUSH_INTERVAL + 3000);
        } catch (InterruptedException ignored) {

        }
    }

    public void sleepUntilSettingsLoaded() {
        try {
            Thread.sleep(60*1000);
        } catch (InterruptedException ignored) {

        }
    }
}
