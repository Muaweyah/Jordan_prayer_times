package com.jo.prayertimes

import android.content.Context
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

class QuranActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var spSurahList: Spinner
    private lateinit var webViewQuran: WebView
    private lateinit var progressQuranPage: ProgressBar
    private lateinit var btnToggleFullscreen: ImageButton
    private lateinit var tvQuranTitle: TextView
    private lateinit var tvPageLabel: TextView
    private lateinit var llQuranRoot: android.widget.LinearLayout
    private lateinit var btnHome: ImageButton
    private var isFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quran)

        HomeNavigator.wire(this)
        spSurahList = findViewById(R.id.spSurahList)
        webViewQuran = findViewById(R.id.webViewQuran)
        progressQuranPage = findViewById(R.id.progressQuranPage)
        btnToggleFullscreen = findViewById(R.id.btnToggleFullscreen)
        tvQuranTitle = findViewById(R.id.tvQuranTitle)
        tvPageLabel = findViewById(R.id.tvPageLabel)
        llQuranRoot = findViewById(R.id.llQuranRoot)
        btnHome = findViewById(R.id.btnHome)

        btnToggleFullscreen.setOnClickListener { toggleFullscreen() }

        setupWebView()

        val surahNames = loadSurahNames()
        val adapter = ArrayAdapter(this, R.layout.spinner_item, surahNames)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spSurahList.adapter = adapter
        loadQuranPage(1)

        spSurahList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadQuranPage(position + 1)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
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
                openTilawaAndReciterMenu(view)
            }
        }
    }

    /** يحاول فتح لوحة "تلاوة" ثم قائمة القرّاء (⋮) تلقائياً بعد تحميل الصفحة، ليتمكن المستخدم
     * من اختيار القارئ الذي يفضله مباشرة من نظام Quranflash المدمج والمتزامن أصلاً مع الصفحات. */
    private fun openTilawaAndReciterMenu(view: WebView?) {
        val jsOpenTilawa = """
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

        val jsOpenReciterMenu = """
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

        for (delayMs in listOf(800L, 1800L, 3000L)) {
            view?.postDelayed({ view.evaluateJavascript(jsOpenTilawa, null) }, delayMs)
        }
        for (delayMs in listOf(4200L, 5600L, 7000L)) {
            view?.postDelayed({ view.evaluateJavascript(jsOpenReciterMenu, null) }, delayMs)
        }
    }

    private fun loadSurahNames(): List<String> {
        return try {
            val json = assets.open("surah_names.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            Array(114) { i -> "سورة رقم ${i + 1}" }.toList()
        }
    }

    private fun loadQuranPage(surahNumber: Int) {
        progressQuranPage.visibility = View.VISIBLE
        val url = "https://app.quranflash.com/book/Medina1?ar#/reader/chapter/$surahNumber"
        webViewQuran.loadUrl(url)
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        val visibility = if (isFullscreen) View.GONE else View.VISIBLE
        tvQuranTitle.visibility = visibility
        spSurahList.visibility = visibility
        tvPageLabel.visibility = visibility
        btnHome.visibility = visibility
        llQuranRoot.setPadding(
            if (isFullscreen) 0 else (16 * resources.displayMetrics.density).toInt(),
            if (isFullscreen) 0 else (16 * resources.displayMetrics.density).toInt(),
            if (isFullscreen) 0 else (16 * resources.displayMetrics.density).toInt(),
            if (isFullscreen) 0 else (16 * resources.displayMetrics.density).toInt()
        )
        btnToggleFullscreen.setImageResource(
            if (isFullscreen) android.R.drawable.ic_menu_close_clear_cancel else android.R.drawable.ic_menu_view
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        webViewQuran.destroy()
    }
}
