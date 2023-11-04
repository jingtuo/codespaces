package io.github.jingtuo.android.webview

import android.app.Activity
import androidx.browser.customtabs.CustomTabsClient

fun Activity.bindCustomTabsService(url: String = "https://www.baidu.com") {
    val pkName = WebViewHelper.getPackageNameToUse(this, url)
    if (pkName.isNullOrEmpty()) {
        return
    }
    CustomTabsClient.bindCustomTabsService(this, packageName, WebViewServiceConnection)
}

fun Activity.unbindCustomTabService(url: String = "https://www.baidu.com") {
    val pkName = WebViewHelper.getPackageNameToUse(this, url)
    if (pkName.isNullOrEmpty()) {
        return
    }
    unbindService(WebViewServiceConnection)
}
