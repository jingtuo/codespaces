package io.github.jingtuo.android.webview

import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi

/**
 * 封装系统[WebViewClient]
 */
open class JtWebViewClient(private val historyEnabled: Boolean): WebViewClient() {
    
    companion object {
        const val TAG = "WebViewClient"
        const val SCHEME_HTTP = "http"
        const val SCHEME_HTTPS = "https"
    }

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url != null) {
            try {
                val uri = Uri.parse(url)
                if (view != null) {
                    return shouldOverrideUrlLoading(view, uri)
                }
            } catch (e: Exception) {
                Log.e(TAG, "An error(${e.message}) occurred while loading the url($url)")
            }
        }
        //由系统处理
        return true
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url
        if (uri != null) {
            try {
                if (view != null) {
                    return shouldOverrideUrlLoading(view, uri)
                }
            } catch (e: Exception) {
                Log.e(TAG, "An error(${e.message}) occurred while loading the url($uri)")
            }
        }
        //取消当前请求
        return true
    }

    /**
     * true: 表示取消当前请求
     * false: 加载当前请求
     */
    open fun shouldOverrideUrlLoading(view: WebView, uri: Uri): Boolean {
        val scheme = uri.scheme
        if (TextUtils.isEmpty(scheme)) {
            return true
        }
        //经验证, 加载百度网页之后, 进行搜索时触发加载新的uri, 但canGoBack()返回false,
        //所以不能使用canGoBack判断是否有历史
        val backForwardList = view.copyBackForwardList()
        if (backForwardList.size == 0) {
            //没有历史, url直接转发
            //交给当前WebView继续加载
            return false
        }
        if (historyEnabled) {
            //支持历史功能
            if (SCHEME_HTTPS == scheme || SCHEME_HTTP == scheme) {
                //交给当前WebView继续加载
                return false
            }
        }
        view.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        return true
    }

    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        Log.e(TAG, "an error($errorCode, $description) occurred while loading the url(${failingUrl})")
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        Log.e(TAG, "an error(${error?.errorCode}, ${error?.description}) occurred while loading the url(${request?.url})")
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        Log.e(TAG, "an http error(${errorResponse?.statusCode}, ${errorResponse?.reasonPhrase}) occurred while loading the url(${request?.url})")
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        super.onReceivedSslError(view, handler, error)
        Log.e(TAG, "an ssl error(${error?.primaryError}, ${error?.certificate}) occurred while loading the url(${error?.url})")
    }

}