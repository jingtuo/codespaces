package io.github.jingtuo.android.webview

import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.os.Build
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
open class JtWebViewClient: WebViewClient() {

    companion object {
        const val TAG = "JtWebView"
    }

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url != null) {
            try {
                val uri = Uri.parse(url)
                if (view != null && shouldOverrideUrlLoading(view, uri)) {
                    return false
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
                if (view != null && shouldOverrideUrlLoading(view, uri)) {
                    return false
                }
            } catch (e: Exception) {
                Log.e(TAG, "An error(${e.message}) occurred while loading the url($uri)")
            }
        }
        //由系统处理
        return true
    }

    /**
     * 是否拦截uri, 返回true表示内部处理
     */
    open fun shouldOverrideUrlLoading(view: WebView, uri: Uri): Boolean {
        return true
    }

    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
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
        Log.e(TAG, "an http error(${error?.primaryError}, ${error?.certificate}) occurred while loading the url(${error?.url})")
    }
}