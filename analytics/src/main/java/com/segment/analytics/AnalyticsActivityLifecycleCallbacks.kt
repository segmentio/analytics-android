package com.segment.analytics

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.segment.analytics.integrations.Logger
import com.segment.analytics.internal.Private
import com.segment.analytics.platform.AndroidLifecycle
import com.segment.analytics.platform.Extension
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal class AnalyticsActivityLifecycleCallbacks(
        private val analyticsObj: Analytics,
        private val analyticsExecutor: ExecutorService,
        private val shouldTrackApplicationLifecycleEvents: Boolean,
        private val trackDeepLinks: Boolean,
        private val shouldRecordScreenViews: Boolean,
        private val packageInfo: PackageInfo,
        private val useNewLifecycleMethods: Boolean,
        private val sharedPreferences: SharedPreferences,
        private val logger: Logger
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver, Extension {

    override val type = Extension.Type.Before
    override val name = "AnalyticsActivityLifecycleCallbacksExtension"
    override var analytics: Analytics? = analyticsObj
    private val trackedApplicationLifecycleEvents = AtomicBoolean(false)
    private val numberOfActivities = AtomicInteger(1)
    private val firstLaunch = AtomicBoolean(false)
    private val isChangingActivityConfigurations = AtomicBoolean(false)

//    private val application: Application
//    private val lifecycle: Lifecycle
//
//    init {
//        setupListeners()
//    }
//
//    private fun setupListeners() {
//        application.registerActivityLifecycleCallbacks(this)
//        if (useNewLifecycleMethods) {
//            lifecycle.addObserver(this)
//        }
//    }

    private fun runOnAnalyticsThread(block: () -> Unit) {
        if (!analyticsObj.isShutdown) {
            analyticsExecutor.submit(block)
        }
    }

    /* OLD LIFECYCLE HOOKS */
    override fun onActivityCreated(activity: Activity?, bundle: Bundle?) {
        runOnAnalyticsThread {
            analyticsObj.timeline?.applyClosure { extension: Extension? ->
                if (extension is AndroidLifecycle) {
                    (extension as AndroidLifecycle).onActivityCreated(activity, bundle)
                }
            }
        }
        if (!useNewLifecycleMethods) {
            onCreate(stubOwner)
        }
        if (trackDeepLinks) {
            trackDeepLink(activity)
        }
    }

    override fun onActivityStarted(activity: Activity?) {
        if (shouldRecordScreenViews) {
            recordScreenViews(activity)
        }
        runOnAnalyticsThread {
            analyticsObj.timeline.applyClosure { extension: Extension? ->
                if (extension is AndroidLifecycle) {
                    (extension as AndroidLifecycle).onActivityStarted(activity)
                }
            }
        }
    }

    override fun onActivityResumed(activity: Activity?) {
        runOnAnalyticsThread {
            analyticsObj.timeline.applyClosure { extension: Extension? ->
                if (extension is AndroidLifecycle) {
                    (extension as AndroidLifecycle).onActivityResumed(activity)
                }
            }
        }
        if (!useNewLifecycleMethods) {
            onStart(stubOwner)
        }
    }

    override fun onActivityPaused(activity: Activity?) {
        runOnAnalyticsThread {
            analyticsObj.timeline.applyClosure { extension: Extension? ->
                if (extension is AndroidLifecycle) {
                    (extension as AndroidLifecycle).onActivityPaused(activity)
                }
            }
        }
        if (!useNewLifecycleMethods) {
            onPause(stubOwner)
        }
    }

    override fun onActivityStopped(activity: Activity?) {
        runOnAnalyticsThread {
            analyticsObj.timeline.applyClosure { extension: Extension? ->
                if (extension is AndroidLifecycle) {
                    (extension as AndroidLifecycle).onActivityStopped(activity)
                }
            }
        }
        if (!useNewLifecycleMethods) {
            onStop(stubOwner)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity?, bundle: Bundle?) {
        runOnAnalyticsThread {
            analyticsObj.timeline.applyClosure { extension: Extension? ->
                if (extension is AndroidLifecycle) {
                    (extension as AndroidLifecycle).onActivitySaveInstanceState(activity, bundle)
                }
            }
        }
    }

    override fun onActivityDestroyed(activity: Activity?) {
        runOnAnalyticsThread {
            analyticsObj.timeline.applyClosure { extension: Extension? ->
                if (extension is AndroidLifecycle) {
                    (extension as AndroidLifecycle).onActivityDestroyed(activity)
                }
            }
        }
        if (!useNewLifecycleMethods) {
            onDestroy(stubOwner)
        }
    }

    /* NEW LIFECYCLE HOOKS (These get called alongside the old ones) */

    override fun onStop(owner: LifecycleOwner) {
        // App in background
        if (shouldTrackApplicationLifecycleEvents
                && numberOfActivities.decrementAndGet() == 0 && !isChangingActivityConfigurations.get()) {
            analyticsObj.track("Application Backgrounded")
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        // App in foreground
        if (shouldTrackApplicationLifecycleEvents
                && numberOfActivities.incrementAndGet() == 1 && !isChangingActivityConfigurations.get()) {
            val properties = Properties()
            if (firstLaunch.get()) {
                properties
                        .putValue("version", packageInfo.versionName)
                        .putValue("build", packageInfo.versionCode.toString())
            }
            properties.putValue("from_background", !firstLaunch.getAndSet(false))
            analyticsObj.track("Application Opened", properties)
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        // App created
        if (!trackedApplicationLifecycleEvents.getAndSet(true)
                && shouldTrackApplicationLifecycleEvents) {
            numberOfActivities.set(0)
            firstLaunch.set(true)
            trackApplicationLifecycleEvents()
        }
    }

    override fun onResume(owner: LifecycleOwner) {}
    override fun onPause(owner: LifecycleOwner) {}
    override fun onDestroy(owner: LifecycleOwner) {}

    private fun trackDeepLink(activity: Activity?) {
        val intent = activity?.intent
        if (intent == null || intent.data == null) {
            return
        }
        val properties = Properties()
        val uri = intent.data
        uri?.let {
            for (parameter in uri.queryParameterNames) {
                val value = uri.getQueryParameter(parameter)
                if (value != null && value.trim().isNotEmpty()) {
                    properties[parameter] = value
                }
            }
            properties["url"] = uri.toString()
        }
        analyticsObj.track("Deep Link Opened", properties)
    }

    @Private
    fun trackApplicationLifecycleEvents() {
        // Get the current version.
        val packageInfo = packageInfo
        val currentVersion = packageInfo.versionName
        val currentBuild = packageInfo.versionCode

        // Get the previous recorded version.
        val sharedPreferences = sharedPreferences
        val previousVersion = sharedPreferences.getString(VERSION_KEY, null)
        val previousBuild = sharedPreferences.getInt(BUILD_KEY, -1)

        // Check and track Application Installed or Application Updated.
        if (previousBuild == -1) {
            analyticsObj.track(
                    "Application Installed",
                    Properties() //
                            .putValue(VERSION_KEY, currentVersion)
                            .putValue(BUILD_KEY, currentBuild.toString()))
        } else if (currentBuild != previousBuild) {
            analyticsObj.track(
                    "Application Updated",
                    Properties() //
                            .putValue(VERSION_KEY, currentVersion)
                            .putValue(BUILD_KEY, currentBuild.toString())
                            .putValue("previous_$VERSION_KEY", previousVersion)
                            .putValue("previous_$BUILD_KEY", previousBuild.toString()))
        }

        // Update the recorded version.
        val editor = sharedPreferences.edit()
        editor.putString(VERSION_KEY, currentVersion)
        editor.putInt(BUILD_KEY, currentBuild)
        editor.apply()
    }

    @Private
    fun recordScreenViews(activity: Activity?) {
        val packageManager = activity?.packageManager
        try {
            val info = packageManager?.getActivityInfo(
                    activity.componentName, PackageManager.GET_META_DATA)
            val activityLabel = info?.loadLabel(packageManager)
            analyticsObj.screen(activityLabel.toString())
        } catch (e: PackageManager.NameNotFoundException) {
            throw AssertionError("Activity Not Found: $e")
        } catch (e: Exception) {
            logger.error(e, "Unable to track screen view for %s", activity.toString())
        }
    }

    companion object {
        private const val VERSION_KEY = "version"
        private const val BUILD_KEY = "build"

        // This is just a stub LifecycleOwner which is used when we need to call some lifecycle
        // methods without going through the actual lifecycle callbacks
        private val stubOwner: LifecycleOwner = object : LifecycleOwner {
            var stubLifecycle: Lifecycle = object : Lifecycle() {
                override fun addObserver(observer: LifecycleObserver) {
                    // NO-OP
                }

                override fun removeObserver(observer: LifecycleObserver) {
                    // NO-OP
                }

                override fun getCurrentState(): State {
                    return State.DESTROYED
                }
            }

            override fun getLifecycle(): Lifecycle {
                return stubLifecycle
            }
        }
    }

}
