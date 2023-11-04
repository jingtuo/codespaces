package io.github.jingtuo.android.webview

import android.content.ComponentName
import android.util.Log
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection

object WebViewServiceConnection: CustomTabsServiceConnection() {

    const val TAG = "WebView"

    override fun onServiceDisconnected(component: ComponentName?) {
        Log.i(TAG, "service disconnected: ${component?.packageName}: ${component?.className}")
    }

    override fun onCustomTabsServiceConnected(component: ComponentName, client: CustomTabsClient) {
        Log.i(TAG, "service connected: ${component.packageName}: ${component.className}")
        client.warmup(1)
    }
}