package com.jo.prayertimes

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private lateinit var btnToggleFullscreen: ImageButton
    private lateinit var btnHome: ImageButton
    private var isFullscreen = false

    private val progressHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quran)

        HomeNavigator.wire(this)
        webViewQuran = findViewById(R.id.webViewQuran)
        progressQuranPage = findViewById(R.id.progressQuranPage)
        btnToggleFullscreen = findViewById(R.id.btnToggleFullscreen)
        btnHome = findViewById(R.id.btnHome)

        btnToggleFullscreen.setOnClickListener { toggleFullscreen() }

        setupWebView()
        loadQuranPage(1)
    }

    private fun setupWebView() {
        webViewQuran.settings.javaScriptEnabled = true
        webViewQuran.settings.domStorageEnabled = true
        webViewQuran.settings.loadWithOverviewMode = true
        webViewQuran.settings.useWideViewPort = true
        webViewQuran.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressQuranPage.visibility = View.GONE
                openTilawaPanel(view)
            }
        }
    }

    /** يحاول فتح قائمة القرّاء (زر "تلاوة") داخل صفحة Quranflash تلقائياً بعد تحميلها. */
    private fun openTilawaPanel(view: WebView?) {
        val js = """
            (function() {
                function findAndClick() {
                    var all = document.querySelectorAll('*');
                    for (var i = 0; i < all.length; i++) {
                        var el = all[i];
                        if (el.children.length === 0 && el.textContent && el.textContent.trim() === 'تلاوة') {
                            el.click();
                            return true;
                        }
                    }
                    return false;
                }
                return findAndClick();
            })();
        """.trimIndent()

        val delays = listOf(800L, 1800L, 3000L)
        for (delayMs in delays) {
            progressHandler.postDelayed({ view?.evaluateJavascript(js, null) }, delayMs)
        }

        // بعد فتح لوحة التلاوة، نحاول الضغط على زر "..." (النقاط الثلاث) لإظهار قائمة القرّاء
        val jsMoreMenu = """
            (function() {
                function findAndClick() {
                    var all = document.querySelectorAll('*');
                    for (var i = 0; i < all.length; i++) {
                        var el = all[i];
                        if (el.children.length === 0 && el.textContent) {
                            var t = el.textContent.trim();
                            if (t === '...' || t === '…' || t === '⋮') {
                                el.click();
                                return true;
                            }
                        }
                    }
                    return false;
                }
                return findAndClick();
            })();
        """.trimIndent()

        val moreMenuDelays = listOf(4000L, 5500L, 7000L)
        for (delayMs in moreMenuDelays) {
            progressHandler.postDelayed({ view?.evaluateJavascript(jsMoreMenu, null) }, delayMs)
        }
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        btnHome.visibility = if (isFullscreen) View.GONE else View.VISIBLE
        btnToggleFullscreen.setImageResource(
            if (isFullscreen) android.R.drawable.ic_menu_close_clear_cancel else android.R.drawable.ic_menu_view
        )
    }

    private fun loadQuranPage(surahNumber: Int) {
        progressQuranPage.visibility = View.VISIBLE
        val url = "https://quran.ksu.edu.sa/m.php?l=ar#aya=${surahNumber}_1&t=1"
        webViewQuran.loadUrl(url)
    }

    override fun onDestroy() {
        super.onDestroy()
        progressHandler.removeCallbacksAndMessages(null)
        webViewQuran.destroy()
    }
}
