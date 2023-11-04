package io.github.jingtuo.laboratory

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import io.github.jingtuo.android.webview.WebViewFragment
import io.github.jingtuo.android.webview.WebViewHelper
import io.github.jingtuo.codespaces.event.OpenUrl
import io.github.jingtuo.laboratory.databinding.ActivityMainBinding
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val TAG = "WebView"
        const val ACTION_BTN = "io.github.jingtuo.codespaces.ACTION_BTN"
        const val ACTION_MENU = "io.github.jingtuo.codespaces.ACTION_MENU"
    }

    private var mClient: CustomTabsClient? = null

    private lateinit var curTag: String

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i(TAG, "onReceive: ${intent?.action}")
        }

    }

    private var connection: CustomTabsServiceConnection = object : CustomTabsServiceConnection() {
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "onServiceDisconnected: ${name?.packageName}, ${name?.className}")
        }

        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            mClient = client
            Log.i(TAG, "onServiceConnected: ${name.packageName}, ${name.className}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.navView.setOnItemSelectedListener { it ->
            val index = if (R.id.navigation_home == it.itemId) {
                0
            } else if (R.id.navigation_dashboard == it.itemId) {
                1
            } else {
                2
            }
            binding.viewPager.setCurrentItem(index, false)
            true
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_BTN)
            addAction(ACTION_MENU)
        }
        registerReceiver(receiver, filter)
        curTag = "home"
        val adapter = object: FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return 3
            }

            override fun createFragment(position: Int): Fragment {
                val url = if (0 == position) {
                    "https://developer.android.google.cn/"
                } else if (1 == position) {
                    "https://github.com"
                } else {
                    "https://gitee.com"
                }
                return WebViewFragment.Builder(url).build()
            }
        }
        binding.viewPager.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        binding.viewPager.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        val pkName = WebViewHelper.getPackageNameToUse(this, "https://www.baidu.com")
        if (pkName.isNullOrEmpty()) {
            return
        }
        CustomTabsClient.bindCustomTabsService(this, pkName, connection)
    }

    override fun onStop() {
        super.onStop()
        val pkName = WebViewHelper.getPackageNameToUse(this, "https://www.baidu.com")
        if (pkName.isNullOrEmpty()) {
            return
        }
        unbindService(connection)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOpenUrl(openUrl: OpenUrl) {
        var builder = CustomTabsIntent.Builder()
        if (mClient != null) {
            val session = mClient!!.newSession(object : CustomTabsCallback() {
                override fun onPostMessage(message: String, extras: Bundle?) {
                    super.onPostMessage(message, extras)
                    Log.i(TAG, "onPostMessage: $extras")
                }

                override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
                    super.onNavigationEvent(navigationEvent, extras)
                    if (NAVIGATION_FINISHED == navigationEvent) {
                        Toast.makeText(
                            this@MainActivity.applicationContext,
                            "页面加载完成",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
            if (session != null) {
                builder = builder.setSession(session!!)
            }
        }
        val closeBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_back_light)
        val shareBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_share_light)
        val intent = builder.setDefaultColorSchemeParams(
            CustomTabColorSchemeParams.Builder()
                //修改顶部导航栏颜色
                .setToolbarColor(ContextCompat.getColor(this, android.R.color.white))
                .build()
        )
            .setCloseButtonIcon(closeBitmap)//不生效
            .setShowTitle(true)//显示网页标题
            .setUrlBarHidingEnabled(false)//上滑(看下面内容)时, 顶部导航栏是否消失
            .setActionButton(
                shareBitmap, "自定义分享", PendingIntent.getBroadcast(
                    this, 1,
                    Intent(ACTION_BTN), PendingIntent.FLAG_MUTABLE
                )
            )//不生效
            .addMenuItem(
                "自定义MENU", PendingIntent.getBroadcast(
                    this, 1,
                    Intent(ACTION_MENU), PendingIntent.FLAG_MUTABLE
                )
            )//在下拉菜单中添加一个菜单
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)//显示分享
            .build()
        intent.intent.putExtra(
            "androidx.browser.customtabs.extra.INITIAL_ACTIVITY_HEIGHT_IN_PIXEL", 600)
        intent.launchUrl(this@MainActivity, Uri.parse(openUrl.url))
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        unregisterReceiver(receiver)
    }

    private val map = mutableMapOf<String, Fragment>()
}