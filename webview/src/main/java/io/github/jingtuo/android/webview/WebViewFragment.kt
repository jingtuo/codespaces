package io.github.jingtuo.android.webview

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
/**
 * WebView组件, 私有化构造函数
 * 基于[FragmentManager.findFragmentByTag]管理[Fragment]
 * 1. 使用[FragmentTransaction.add] + [FragmentTransaction.replace], [FragmentManager.getFragments]始终只有一个,
 *      示例:
 *         var fragment = supportFragmentManager.findFragmentByTag(tag)
 *         if (tag == curTag) {
 *             if (fragment != null) {
 *                 return
 *             }
 *         }
 *         val curFragment = supportFragmentManager.findFragmentByTag(curTag)
 *         val transaction = supportFragmentManager.beginTransaction()
 *         if (curFragment == null) {
 *             fragment = WebViewFragment.Builder(url).build()
 *             transaction.add(R.id.fragment_container, fragment, tag)
 *         } else {
 *             if (fragment == null) {
 *                 fragment = WebViewFragment.Builder(url).build()
 *             }
 *             transaction.replace(R.id.fragment_container, fragment, tag)
 *         }
 *         transaction.commit()
 *         curTag = tag
 *      被replace的Fragment会触发onDestroyView、onDestroy、onDetach
 * 2. 使用[FragmentTransaction.add] + [FragmentTransaction.show] + [FragmentTransaction.hide], [FragmentManager.getFragments]包含所有的Fragment
 *      示例:
 *         var fragment = supportFragmentManager.findFragmentByTag(tag)
 *         if (tag == curTag) {
 *             if (fragment != null) {
 *                 return
 *             }
 *         }
 *         val curFragment = supportFragmentManager.findFragmentByTag(curTag)
 *         val transaction = supportFragmentManager.beginTransaction()
 *         if (fragment == null) {
 *             fragment = WebViewFragment.Builder(url).build()
 *             transaction.add(R.id.fragment_container, fragment, tag)
 *         } else {
 *             transaction.show(fragment)
 *         }
 *         if (curFragment != null) {
 *             transaction.hide(fragment)
 *         }
 *         transaction.commit()
 *         curTag = tag
 *      被hide的Fragment会触发Fragment.onHiddenChanged, Activity切后台, 所有的Fragment触发onPause, 再切换前台, 所有的Fragment触发onResume
 * 3. 使用[FragmentTransaction.add] + [FragmentTransaction.attach] + [FragmentTransaction.detach], [FragmentManager.getFragments]始终只有一个,
 *      示例:
 *         var fragment = supportFragmentManager.findFragmentByTag(tag)
 *         if (tag == curTag) {
 *             if (fragment != null) {
 *                 return
 *             }
 *         }
 *         val curFragment = supportFragmentManager.findFragmentByTag(curTag)
 *         val transaction = supportFragmentManager.beginTransaction()
 *         if (fragment == null) {
 *             fragment = WebViewFragment.Builder(url).build()
 *             transaction.add(R.id.fragment_container, fragment, tag)
 *         } else {
 *             transaction.attach(fragment)
 *         }
 *         if (curFragment != null) {
 *             transaction.detach(curFragment)
 *         }
 *         transaction.commit()
 *         curTag = tag
 *      被detach的Fragment会触发onDestroyView
 *
 *  1. 调用[FragmentTransaction.attach]之前必须通过[FragmentTransaction.add]进行添加, 否则不会显示
 *
 *  --------------
 *  1. 使用ViewPager2 + Fragment, 设置ViewPager2.setOffscreenPageLimit(ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT)
 *      切换至第二个Fragment, 第一个Fragment生命周期仅触发onPause, 不会触发onStop、onDestroyView
 *
 */
class WebViewFragment private constructor(): Fragment() {

    private lateinit var webView: WebView

    companion object {

        private const val TAG = "WebViewFragment"

        private const val EXTRA_URL = "URL"

        /**
         *
         */
        private const val EXTRA_MODE = "MODE"

        public const val MODE_NORMAL = 1

        public const val MODE_MULTI_TABS = 2

    }

    class Builder(val url: String) {

        private var mode: Int = MODE_NORMAL

        fun setMode(mode: Int):Builder {
            this.mode =  mode
            return this
        }

        fun build(): Fragment {
            val args = Bundle().apply {
                putString(EXTRA_URL, url)
                putInt(EXTRA_MODE, mode)
            }
            val fragment = WebViewFragment()
            fragment.arguments = args
            return fragment
        }
    }



    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.i(TAG, "onAttach ${arguments?.getString(EXTRA_URL)}")

        tag
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        webView = WebView(requireContext())
        return webView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView.settings.javaScriptEnabled = true
        val webViewClient = JtWebViewClient(false)
        webView.webViewClient = webViewClient
        webView.webChromeClient = JtWebChromeClient()
        arguments?.let {
            val url = it.getString(EXTRA_URL, "")
            webView.loadUrl(url)
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        Log.i(TAG, "onHiddenChanged ${arguments?.getString(EXTRA_URL)} -> $hidden")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.i(TAG, "onDestroyView ${webView.url}")
        //清除高亮匹配的文本
        webView.clearMatches()
        //清除访问的历史栈列表
        webView.clearHistory()
        //重置View的状态, 释放页面资源
        webView.loadUrl("about:blank")
        if (webView.parent is ViewGroup) {
            val viewGroup = webView.parent as ViewGroup
            viewGroup.removeView(webView)
            //destroy方法要求WebView从View树中移除之后才能调用
            webView.destroy()
        }
    }

    override fun onDetach() {
        super.onDetach()
        Log.i(TAG, "onDetach ${arguments?.getString(EXTRA_URL)}")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy ${arguments?.getString(EXTRA_URL)}")
    }

}