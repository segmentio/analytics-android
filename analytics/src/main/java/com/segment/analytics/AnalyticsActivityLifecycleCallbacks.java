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

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.segment.analytics.internal.Utils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class AnalyticsActivityLifecycleCallbacks
        implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private Analytics analytics;
    private ExecutorService analyticsExecutor;
    private Boolean shouldTrackApplicationLifecycleEvents;
    private Boolean trackDeepLinks;
    private Boolean shouldRecordScreenViews;
    private PackageInfo packageInfo;

    private AtomicBoolean trackedApplicationLifecycleEvents;
    private AtomicInteger numberOfActivities;
    private AtomicBoolean firstLaunch;

    private AtomicBoolean isChangingActivityConfigurations;
    private Boolean useNewLifecycleMethods;

    // This is just a stub LifecycleOwner which is used when we need to call some lifecycle
    // methods without going through the actual lifecycle callbacks
    private static LifecycleOwner stubOwner =
            new LifecycleOwner() {
                Lifecycle stubLifecycle =
                        new Lifecycle() {
                            @Override
                            public void addObserver(@NonNull LifecycleObserver observer) {
                                // NO-OP
                            }

                            @Override
                            public void removeObserver(@NonNull LifecycleObserver observer) {
                                // NO-OP
                            }

                            @NonNull
                            @Override
                            public Lifecycle.State getCurrentState() {
                                return State.DESTROYED;
                            }
                        };

                @NonNull
                @Override
                public Lifecycle getLifecycle() {
                    return stubLifecycle;
                }
            };

    private AnalyticsActivityLifecycleCallbacks(
            Analytics analytics,
            ExecutorService analyticsExecutor,
            Boolean shouldTrackApplicationLifecycleEvents,
            Boolean trackDeepLinks,
            Boolean shouldRecordScreenViews,
            PackageInfo packageInfo,
            Boolean useNewLifecycleMethods) {
        this.trackedApplicationLifecycleEvents = new AtomicBoolean(false);
        this.numberOfActivities = new AtomicInteger(1);
        this.firstLaunch = new AtomicBoolean(false);
        this.analytics = analytics;
        this.analyticsExecutor = analyticsExecutor;
        this.shouldTrackApplicationLifecycleEvents = shouldTrackApplicationLifecycleEvents;
        this.trackDeepLinks = trackDeepLinks;
        this.shouldRecordScreenViews = shouldRecordScreenViews;
        this.packageInfo = packageInfo;
        this.useNewLifecycleMethods = useNewLifecycleMethods;
        this.isChangingActivityConfigurations = new AtomicBoolean(false);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        // App in background
        if (shouldTrackApplicationLifecycleEvents
                && numberOfActivities.decrementAndGet() == 0
                && !isChangingActivityConfigurations.get()) {
            analytics.track("Application Backgrounded");
        }
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        // App in foreground
        if (shouldTrackApplicationLifecycleEvents
                && numberOfActivities.incrementAndGet() == 1
                && !isChangingActivityConfigurations.get()) {
            Properties properties = new Properties();
            if (firstLaunch.get()) {
                properties
                        .putValue("version", packageInfo.versionName)
                        .putValue("build", String.valueOf(packageInfo.versionCode));
            }
            properties.putValue("from_background", !firstLaunch.getAndSet(false));
            analytics.track("Application Opened", properties);
        }
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        // App created
        if (!trackedApplicationLifecycleEvents.getAndSet(true)
                && shouldTrackApplicationLifecycleEvents) {
            numberOfActivities.set(0);
            firstLaunch.set(true);
            analytics.trackApplicationLifecycleEvents();
        }
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {}

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {}

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {}

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        analytics.runOnMainThread(IntegrationOperation.onActivityCreated(activity, bundle));

        if (!useNewLifecycleMethods) {
            onCreate(stubOwner);
        }

        if (trackDeepLinks) {
            trackDeepLink(activity);
        }
    }

    private void trackDeepLink(Activity activity) {
        Intent intent = activity.getIntent();
        if (intent == null || intent.getData() == null) {
            return;
        }

        Properties properties = new Properties();

        Uri referrer = Utils.getReferrer(activity);
        if (referrer != null) {
            properties.putReferrer(referrer.toString());
        }

        Uri uri = intent.getData();
        try {
            for (String parameter : uri.getQueryParameterNames()) {
                String value = uri.getQueryParameter(parameter);
                if (value != null && !value.trim().isEmpty()) {
                    properties.put(parameter, value);
                }
            }
        } catch (Exception e) {
            analytics
                    .logger("LifecycleCallbacks")
                    .error(e, "failed to get uri params for %s", uri.toString());
        }

        properties.put("url", uri.toString());
        analytics.track("Deep Link Opened", properties);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (shouldRecordScreenViews) {
            analytics.recordScreenViews(activity);
        }
        analytics.runOnMainThread(IntegrationOperation.onActivityStarted(activity));
    }

    @Override
    public void onActivityResumed(Activity activity) {
        analytics.runOnMainThread(IntegrationOperation.onActivityResumed(activity));
        if (!useNewLifecycleMethods) {
            onStart(stubOwner);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        analytics.runOnMainThread(IntegrationOperation.onActivityPaused(activity));
        if (!useNewLifecycleMethods) {
            onPause(stubOwner);
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        analytics.runOnMainThread(IntegrationOperation.onActivityStopped(activity));
        if (!useNewLifecycleMethods) {
            onStop(stubOwner);
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        analytics.runOnMainThread(
                IntegrationOperation.onActivitySaveInstanceState(activity, bundle));
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        analytics.runOnMainThread(IntegrationOperation.onActivityDestroyed(activity));
        if (!useNewLifecycleMethods) {
            onDestroy(stubOwner);
        }
    }

    public static class Builder {
        private Analytics analytics;
        private ExecutorService analyticsExecutor;
        private Boolean shouldTrackApplicationLifecycleEvents;
        private Boolean trackDeepLinks;
        private Boolean shouldRecordScreenViews;
        private PackageInfo packageInfo;
        private Boolean useNewLifecycleMethods;

        public Builder() {}

        public Builder analytics(Analytics analytics) {
            this.analytics = analytics;
            return this;
        }

        Builder analyticsExecutor(ExecutorService analyticsExecutor) {
            this.analyticsExecutor = analyticsExecutor;
            return this;
        }

        Builder shouldTrackApplicationLifecycleEvents(
                Boolean shouldTrackApplicationLifecycleEvents) {
            this.shouldTrackApplicationLifecycleEvents = shouldTrackApplicationLifecycleEvents;
            return this;
        }

        Builder trackDeepLinks(Boolean trackDeepLinks) {
            this.trackDeepLinks = trackDeepLinks;
            return this;
        }

        Builder shouldRecordScreenViews(Boolean shouldRecordScreenViews) {
            this.shouldRecordScreenViews = shouldRecordScreenViews;
            return this;
        }

        Builder packageInfo(PackageInfo packageInfo) {
            this.packageInfo = packageInfo;
            return this;
        }

        Builder useNewLifecycleMethods(boolean useNewLifecycleMethods) {
            this.useNewLifecycleMethods = useNewLifecycleMethods;
            return this;
        }

        public AnalyticsActivityLifecycleCallbacks build() {
            return new AnalyticsActivityLifecycleCallbacks(
                    analytics,
                    analyticsExecutor,
                    shouldTrackApplicationLifecycleEvents,
                    trackDeepLinks,
                    shouldRecordScreenViews,
                    packageInfo,
                    useNewLifecycleMethods);
        }
    }
}
