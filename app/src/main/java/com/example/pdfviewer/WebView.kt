package com.example.pdfviewer

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView


@Composable
fun WebViewScreen(url:String) {

    AndroidView(
        factory = { context ->
            WebView(context).apply {
//                settings.javaScriptEnabled = true
                webViewClient = WebViewClient()

                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.setSupportZoom(true)
                settings.allowFileAccessFromFileURLs = true
            }
        },
        update = { webView ->
            webView.loadUrl(url)
        }
    )
}