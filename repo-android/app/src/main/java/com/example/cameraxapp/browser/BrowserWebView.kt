package com.example.cameraxapp.browser

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.webkit.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    url: String,
    onProgressChanged: (Int) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (WebView, String) -> Unit,
    onConsoleMsgCaptured: (ConsoleMessage) -> Unit,
    onDownloadTriggered: (String, String, String, String, Long) -> Unit,
    userAgentString: String?,
    modifier: Modifier = Modifier,
    updateWebView: (WebView) -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            onConsoleMsgCaptured(it)
                            Log.d("BrowserConsole", "[${it.messageLevel()}] ${it.message()} @ L:${it.lineNumber()} of ${it.sourceId()}")
                            com.example.cameraxapp.AppLogger.d("BrowserConsole", "[${it.messageLevel()}] ${it.message()} @ L:${it.lineNumber()} of ${it.sourceId()}")
                        }
                        return true
                    }
                }
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        url?.let { onPageStarted(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (view != null && url != null) {
                            onPageFinished(view, url)
                        }
                    }

                    @TargetApi(Build.VERSION_CODES.N)
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val path = request?.url?.toString() ?: return false
                        view?.loadUrl(path)
                        return true
                    }

                    @SuppressWarnings("deprecation")
                    override fun shouldOverrideUrlLoading(view: WebView?, urlStr: String?): Boolean {
                        val path = urlStr ?: return false
                        view?.loadUrl(path)
                        return true
                    }
                }

                setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, contentLength ->
                    onDownloadTriggered(downloadUrl, userAgent, contentDisposition, mimetype, contentLength)
                }

                if (!userAgentString.isNullOrBlank()) {
                    settings.userAgentString = userAgentString
                }

                loadUrl(url)
                updateWebView(this)
            }
        },
        update = { webView ->
            if (!userAgentString.isNullOrBlank() && webView.settings.userAgentString != userAgentString) {
                webView.settings.userAgentString = userAgentString
                webView.reload() // Reload webview if the rendering user-agent overrides on-the-fly
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
