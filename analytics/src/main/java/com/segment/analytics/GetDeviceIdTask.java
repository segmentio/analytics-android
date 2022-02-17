package com.segment.analytics;

import static com.segment.analytics.internal.Utils.getDeviceId;

import android.content.SharedPreferences;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class GetDeviceIdTask {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private final AnalyticsContext analyticsContext;

    private final SharedPreferences segmentSharedPreference;

    private final static String DEVICE_ID_CACHE_KEY = "device.id";

    public GetDeviceIdTask(AnalyticsContext analyticsContext, SharedPreferences segmentSharedPreference) {
        this.analyticsContext = analyticsContext;
        this.segmentSharedPreference = segmentSharedPreference;
    }

    public void start() {
        if (cacheHit()) {
            return;
        }

        final Future<?> future = executor.submit(new Runnable() {
            @Override
            public void run() {
                String deviceId = getDeviceId();

                if (!Thread.currentThread().isInterrupted()) {
                    updateDeviceId(deviceId);
                    updateCache(deviceId);
                }
            }
        });

        executor.execute(new Runnable() {
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

                executor.shutdownNow();
            }
        });
    }

    private boolean cacheHit() {
        String cache = segmentSharedPreference.getString(DEVICE_ID_CACHE_KEY, null);

        if (cache != null) {
            updateDeviceId(cache);
            return true;
        }
        else {
            return false;
        }
    }

    private void updateDeviceId(String deviceId) {
        if (!analyticsContext.containsKey(AnalyticsContext.DEVICE_KEY)) {
            analyticsContext.put(AnalyticsContext.DEVICE_KEY, new AnalyticsContext.Device());
        }

        AnalyticsContext.Device device = (AnalyticsContext.Device) analyticsContext.get(AnalyticsContext.DEVICE_KEY);
        device.put(AnalyticsContext.Device.DEVICE_ID_KEY, deviceId);
    }

    private void updateCache(String deviceId) {
        SharedPreferences.Editor editor = segmentSharedPreference.edit();
        editor.putString(DEVICE_ID_CACHE_KEY, deviceId);
    }
}
