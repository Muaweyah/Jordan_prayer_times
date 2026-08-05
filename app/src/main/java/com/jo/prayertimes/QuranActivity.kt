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
                cleanUpMushafPage(view)
            }
        }

        webViewQuran.loadUrl("https://www.mp3quran.net/ar/mushaf")
    }

    /** يحاول إخفاء العناصر الزائدة من تصميم الموقع الأصلي (الشريط العلوي، البانر الإعلاني،
     * زر المشاركة، الشريط السفلي) بحثاً عن نص كل عنصر، ويبقي فقط اختيار السورة/القارئ
     * وصفحة المصحف وأزرار التشغيل. هذا حل تخميني لأننا لا نملك وصولاً مباشراً لتصميم الموقع،
     * فقد لا ينجح 100% بجميع الحالات. */
    private fun cleanUpMushafPage(view: WebView?) {
        val js = """
            (function() {
                function hideByExactText(matchTexts, climbLevels) {
                    var all = document.querySelectorAll('*');
                    for (var i = 0; i < all.length; i++) {
                        var el = all[i];
                        if (el.children.length === 0 && el.textContent) {
                            var t = el.textContent.trim();
                            if (matchTexts.indexOf(t) !== -1) {
                                var container = el;
                                for (var up = 0; up < climbLevels && container.parentElement; up++) {
                                    container = container.parentElement;
                                }
                                container.style.display = 'none';
                            }
                        }
                    }
                }
                hideByExactText(['مشاركة'], 2);
                hideByExactText(['المزيد', 'الإذاعة', 'المفضلة', 'الرئيسية', 'تصفح القرآن'], 3);
                hideByExactText(['English'], 3);

                var imgs = document.querySelectorAll('img');
                for (var j = 0; j < imgs.length; j++) {
                    var img = imgs[j];
                    if (img.width > 300 && img.height < 250) {
                        var box = img;
                        for (var up2 = 0; up2 < 2 && box.parentElement; up2++) {
                            box = box.parentElement;
                        }
                        box.style.display = 'none';
                    }
                }
            })();
        """.trimIndent()

        for (delayMs in listOf(500L, 1200L, 2200L)) {
            view?.postDelayed({ view.evaluateJavascript(js, null) }, delayMs)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webViewQuran.destroy()
    }
}
