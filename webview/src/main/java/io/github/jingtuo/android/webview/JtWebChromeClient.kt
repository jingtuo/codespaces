package io.github.jingtuo.android.webview

import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.annotation.RequiresApi

/**
 *
 */
class JtWebChromeClient: WebChromeClient() {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (WebViewHelper.logEnabled) {
            consoleMessage?.let {
                val msg = "sourceId: ${it.sourceId()}, lineNumber: ${it.lineNumber()}, message: ${it.message()}"
                when(it.messageLevel()) {
                    ConsoleMessage.MessageLevel.DEBUG -> Log.d(TAG, msg)
                    ConsoleMessage.MessageLevel.ERROR -> Log.e(TAG, msg)
                    ConsoleMessage.MessageLevel.WARNING -> Log.w(TAG, msg)
                    else -> Log.i(TAG, msg)
                }
            }
        }
        return super.onConsoleMessage(consoleMessage)
    }

    companion object {
        const val TAG = "WebChromeClient"
    }
}