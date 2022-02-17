/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.segment.analytics;

import static com.segment.analytics.internal.Utils.getUniqueID;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

import android.content.SharedPreferences;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class GetDeviceIdTask {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private final AnalyticsContext analyticsContext;

    private final SharedPreferences segmentSharedPreference;

    private final CountDownLatch latch;

    private static final String DEVICE_ID_CACHE_KEY = "device.id";

    public GetDeviceIdTask(
            AnalyticsContext analyticsContext,
            SharedPreferences segmentSharedPreference,
            CountDownLatch latch) {
        this.analyticsContext = analyticsContext;
        this.segmentSharedPreference = segmentSharedPreference;
        this.latch = latch;
    }

    public void execute() {
        if (cacheHit()) {
            return;
        }

        final Future<?> future =
                executor.submit(
                        new Runnable() {
                            @Override
                            public void run() {
                                String deviceId = getDeviceId();

                                if (!Thread.currentThread().isInterrupted()) {
                                    updateDeviceId(deviceId);
                                    updateCache(deviceId);
                                }
                            }
                        });

        executor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            future.get(2, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            future.cancel(true);
                            String fallbackDeviceId = UUID.randomUUID().toString();
                            updateDeviceId(fallbackDeviceId);
                            updateCache(fallbackDeviceId);
                        }

                        latch.countDown();
                        executor.shutdownNow();
                    }
                });
    }

    String getDeviceId() {
        // unique id generated from DRM API
        String uniqueID = getUniqueID();
        if (!isNullOrEmpty(uniqueID)) {
            return uniqueID;
        }

        // If this still fails, generate random identifier that does not persist across
        // installations
        return UUID.randomUUID().toString();
    }

    private boolean cacheHit() {
        String cache = segmentSharedPreference.getString(DEVICE_ID_CACHE_KEY, null);

        if (cache != null) {
            updateDeviceId(cache);
            return true;
        } else {
            return false;
        }
    }

    private void updateDeviceId(String deviceId) {
        synchronized (analyticsContext) {
            if (!analyticsContext.containsKey(AnalyticsContext.DEVICE_KEY)) {
                analyticsContext.put(AnalyticsContext.DEVICE_KEY, new AnalyticsContext.Device());
            }

            AnalyticsContext.Device device =
                    (AnalyticsContext.Device) analyticsContext.get(AnalyticsContext.DEVICE_KEY);
            device.put(AnalyticsContext.Device.DEVICE_ID_KEY, deviceId);
        }
    }

    private void updateCache(String deviceId) {
        SharedPreferences.Editor editor = segmentSharedPreference.edit();
        editor.putString(DEVICE_ID_CACHE_KEY, deviceId);
        editor.apply();
    }
}
