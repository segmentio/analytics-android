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

import static com.segment.analytics.internal.Utils.isNullOrEmpty;

import android.content.SharedPreferences;
import android.media.MediaDrm;
import android.os.Build;
import java.security.MessageDigest;
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

        // since getDeviceId causes ANR (i.e. the function hangs forever),
        // we need it in a separate task
        // here we use Future, because it has built-in cancel mechanism
        final Future<?> future =
                executor.submit(
                        new Runnable() {
                            @Override
                            public void run() {
                                String deviceId = getDeviceId();

                                // the function may come back after a long time,
                                // (since thread can't guaranteed to be cancelled),
                                // we need to check if an interrupt signal has been raised
                                if (!Thread.currentThread().isInterrupted()) {
                                    updateDeviceId(deviceId);
                                    updateCache(deviceId);
                                }
                            }
                        });

        // since Future.get is a blocking call,
        // we need to run it on a different thread.
        executor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            future.get(2, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            // if any exception happens (timeout, interrupt, etc),
                            // cancel the task (raise an interrupt signal)
                            future.cancel(true);
                            String fallbackDeviceId = UUID.randomUUID().toString();
                            updateDeviceId(fallbackDeviceId);
                            updateCache(fallbackDeviceId);
                        }

                        // too bad we have to have a latch here just for unit tests
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

    /**
     * Workaround for not able to get device id on Android 10 or above using DRM API {@see
     * https://stackoverflow.com/questions/58103580/android-10-imei-no-longer-available-on-api-29-looking-for-alternatives}
     * {@see https://developer.android.com/training/articles/user-data-ids}
     */
    private String getUniqueID() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return null;

        UUID wideVineUuid = new UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L);
        MediaDrm wvDrm = null;
        try {
            wvDrm = new MediaDrm(wideVineUuid);
            byte[] wideVineId = wvDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(wideVineId);
            return byteArrayToHexString(md.digest());
        } catch (Exception e) {
            // Inspect exception
            return null;
        } finally {
            if (wvDrm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    wvDrm.close();
                } else {
                    wvDrm.release();
                }
            }
        }
    }

    private String byteArrayToHexString(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();
        for (byte element : bytes) {
            buffer.append(String.format("%02x", element));
        }

        return buffer.toString();
    }
}
