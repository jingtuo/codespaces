package io.github.jingtuo.codespaces

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log

class App : Application() {


    override fun onCreate() {
        super.onCreate()
        Log.i("Test", "isMainProcess: ${isMainProcess()}")
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

}