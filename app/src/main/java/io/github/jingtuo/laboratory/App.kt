package io.github.jingtuo.laboratory

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Log
import io.github.jingtuo.android.webview.WebViewHelper

class App : Application(), ActivityLifecycleCallbacks {

    companion object {
        const val TAG = "CodeSpaces"
    }


    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "App onCreate")
        WebViewHelper.logEnabled = true
        if (isMainProcess()) {
            registerActivityLifecycleCallbacks(this)
        }
    }

    private fun isMainProcess(): Boolean = packageName == getCurProcessName()

    private fun getCurProcessName(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Process.myProcessName()
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        getProcessName()
    } else {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val list = am.runningAppProcesses
        val id = Process.myPid()
        var processName = ""
        for (item in list) {
            if (id == item.pid) {
                processName = item.processName
                break
            }
        }
        processName
    }

    override fun onActivityCreated(activity: Activity, p1: Bundle?) {
        Log.i(TAG, "onCreate: $activity is ${activity.javaClass.name}")
    }

    override fun onActivityStarted(activity: Activity) {
        Log.i(TAG, "onStarted: $activity is ${activity.javaClass.name}")
    }

    override fun onActivityResumed(activity: Activity) {
        Log.i(TAG, "onResumed: $activity is ${activity.javaClass.name}")
    }

    override fun onActivityPaused(activity: Activity) {
        Log.i(TAG, "onPaused: $activity is ${activity.javaClass.name}")
    }

    override fun onActivityStopped(activity: Activity) {
        Log.i(TAG, "onStopped: $activity is ${activity.javaClass.name}")
    }

    override fun onActivitySaveInstanceState(activity: Activity, p1: Bundle) {
        Log.i(TAG, "onSaveInstanceState: $activity is ${activity.javaClass.name}")
    }

    override fun onActivityDestroyed(activity: Activity) {
        Log.i(TAG, "onDestroyed: $activity is ${activity.javaClass.name}")
    }

}