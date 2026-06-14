package com.asepfiles.app

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class HtmlViewerActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_html_viewer)

        webView = findViewById(R.id.html_webview)
        val title = findViewById<TextView>(R.id.html_title)
        val path = intent.getStringExtra("file_path") ?: return finish()
        val file = File(path)
        title.text = file.name

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = false
        }
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file://" + file.absolutePath)

        findViewById<ImageButton>(R.id.btn_html_back).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btn_html_reload).setOnClickListener { webView.reload() }
        findViewById<ImageButton>(R.id.btn_html_edit).setOnClickListener {
            val i = android.content.Intent(this, CodeEditorActivity::class.java)
            i.putExtra("file_path", path)
            startActivity(i)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}