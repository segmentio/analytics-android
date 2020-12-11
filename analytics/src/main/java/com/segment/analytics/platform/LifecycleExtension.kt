package com.segment.analytics.platform

import android.app.Activity
import android.os.Bundle
import com.segment.analytics.Analytics

interface AndroidLifecycle {
    fun onActivityCreated(activity: Activity, savedInstanceState: Bundle) {}
    fun onActivityStarted(activity: Activity) {}
    fun onActivityResumed(activity: Activity) {}
    fun onActivityPaused(activity: Activity) {}
    fun onActivityStopped(activity: Activity) {}
    fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    fun onActivityDestroyed(activity: Activity) {}
}

class AndroidLifecycleExtension : Extension {
    override val type: Extension.Type = Extension.Type.Before
    override val name: String = "AndroidLifecycleExtension"
    override var analytics: Analytics? = null

    fun onActivityCreated(activity: Activity, savedInstanceState: Bundle) {
        analytics?.timeline?.applyClosure {
            if (it is AndroidLifecycle) {
                it.onActivityCreated(activity, savedInstanceState)
            }
        }
    }

    fun onActivityStarted(activity: Activity) {
        analytics?.timeline?.applyClosure {
            if (it is AndroidLifecycle) {
                it.onActivityStarted(activity)
            }
        }
    }

    fun onActivityResumed(activity: Activity) {
        analytics?.timeline?.applyClosure {
            if (it is AndroidLifecycle) {
                it.onActivityResumed(activity)
            }
        }
    }

    fun onActivityPaused(activity: Activity) {
        analytics?.timeline?.applyClosure {
            if (it is AndroidLifecycle) {
                it.onActivityPaused(activity)
            }
        }
    }

    fun onActivityStopped(activity: Activity) {
        analytics?.timeline?.applyClosure {
            if (it is AndroidLifecycle) {
                it.onActivityStopped(activity)
            }
        }
    }

    fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        analytics?.timeline?.applyClosure {
            if (it is AndroidLifecycle) {
                it.onActivitySaveInstanceState(activity, outState)
            }
        }
    }

    fun onActivityDestroyed(activity: Activity) {
        analytics?.timeline?.applyClosure {
            if (it is AndroidLifecycle) {
                it.onActivityDestroyed(activity)
            }
        }
    }
}