package com.jo.prayertimes

import android.content.Context
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

class QuranActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var webViewQuran: WebView
    private lateinit var progressQuranPage: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quran)

        HomeNavigator.wire(this)
        webViewQuran = findViewById(R.id.webViewQuran)
        progressQuranPage = findViewById(R.id.progressQuranPage)

        webViewQuran.settings.javaScriptEnabled = true
        webViewQuran.settings.domStorageEnabled = true
        webViewQuran.settings.loadWithOverviewMode = true
        webViewQuran.settings.useWideViewPort = true
        webViewQuran.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressQuranPage.visibility = View.GONE
            }
        }

        webViewQuran.loadUrl("https://www.mp3quran.net/ar/mushaf")
    }

    override fun onDestroy() {
        super.onDestroy()
        webViewQuran.destroy()
    }
}
