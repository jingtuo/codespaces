package io.github.jingtuo.android.webview

import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService
import androidx.fragment.app.FragmentActivity


/**
 * 基于[CustomTabsIntent.launchUrl]默认打开的浏览器页面, [ActivityLifecycleCallbacks]是无法监控到, 查看手机的任务, 只能看到打开的网页
 */
class WebViewHelper {

    companion object {

        const val TAG = "WebView"

        private var sPackageNameToUse: String? = null

        fun openUrl(activity: FragmentActivity, url: String) {
            val pkName = getPackageNameToUse(activity, url)
            if (pkName.isNullOrEmpty()) {
                return
            }
            val intent = CustomTabsIntent.Builder().apply{

            }.build()
            intent.launchUrl(activity, Uri.parse(url))
        }

        /**
         * Goes through all apps that handle VIEW intents and have a warmup service. Picks
         * the one chosen by the user if there is one, otherwise makes a best effort to return a
         * valid package name.
         *
         * This is **not** threadsafe.
         *
         * @param context [Context] to use for accessing [PackageManager].
         * @return The package name recommended to use for connecting to custom tabs related components.
         */
        fun getPackageNameToUse(context: Context, url: String): String? {
            if (sPackageNameToUse != null) return sPackageNameToUse
            val pm = context.packageManager
            // Get default VIEW intent handler.
            val activityIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            val defaultViewHandlerInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.resolveActivity(activityIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                pm.resolveActivity(activityIntent, 0)
            }
            var defaultViewHandlerPackageName: String? = null
            if (defaultViewHandlerInfo != null) {
                defaultViewHandlerPackageName = defaultViewHandlerInfo.activityInfo.packageName
            }

            //先获取所有支持ACTION_VIEW的Activity信息
            val resolvedActivityList = pm.queryIntentActivities(activityIntent, 0)
            val packagesSupportingCustomTabs: MutableList<String> = ArrayList()
            for (info in resolvedActivityList) {
                val serviceIntent = Intent()
                serviceIntent.action = CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
                serviceIntent.setPackage(info.activityInfo.packageName)
                val resolveServiceInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.resolveService(serviceIntent, PackageManager.ResolveInfoFlags.of(0))
                } else {
                    pm.resolveService(serviceIntent, 0)
                }
                //再检测对应的安装包是否支持ACTION_CUSTOM_TABS_CONNECTION
                if (resolveServiceInfo != null) {
                    packagesSupportingCustomTabs.add(info.activityInfo.packageName)
                }
            }
            //只有同时支持ACTION_VIEW和ACTION_CUSTOM_TABS_CONNECTION才算支持
            sPackageNameToUse = if (packagesSupportingCustomTabs.isEmpty()) {
                null
            } else if (!TextUtils.isEmpty(defaultViewHandlerPackageName)
                && !hasSpecializedHandlerIntents(context, activityIntent)
                && packagesSupportingCustomTabs.contains(defaultViewHandlerPackageName)
            ) {
                defaultViewHandlerPackageName
            } else {
                // Otherwise, pick the next favorite Custom Tabs provider.
                packagesSupportingCustomTabs[0]
            }
            return sPackageNameToUse
        }

        /**
         *
         * 检测是否支持特定的域名和Path, 待优化
         *
         * Used to check whether there is a specialized handler for a given intent.
         * @param intent The intent to check with.
         * @return Whether there is a specialized handler for the given intent.
         */
                private fun hasSpecializedHandlerIntents(context: Context, intent: Intent): Boolean {
            try {
                val pm = context.packageManager
                val handlers = pm.queryIntentActivities(
                    intent,
                    PackageManager.GET_RESOLVED_FILTER
                )
                if (handlers.size == 0) {
                    return false
                }
                for (resolveInfo in handlers) {
                    val filter = resolveInfo.filter ?: continue
                    if (filter.countDataAuthorities() == 0 || filter.countDataPaths() == 0) continue
                    if (resolveInfo.activityInfo == null) continue
                    return true
                }
            } catch (e: RuntimeException) {
                Log.e(TAG, "Runtime exception while getting specialized handlers")
            }
            return false
        }

        @JvmStatic
        var logEnabled = false

    }

}