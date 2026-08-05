package com.jo.prayertimes

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

class QuranActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var spSurahList: Spinner
    private lateinit var btnPlayPause: Button
    private lateinit var btnRewind10: Button
    private lateinit var btnForward10: Button
    private lateinit var tvAyahIndicator: TextView
    private lateinit var webViewQuran: WebView
    private lateinit var progressQuranPage: ProgressBar
    private lateinit var btnToggleFullscreen: ImageButton
    private lateinit var tvQuranTitle: TextView
    private lateinit var tvPageLabel: TextView
    private lateinit var llQuranControls: LinearLayout
    private lateinit var llQuranRoot: LinearLayout
    private lateinit var btnHome: ImageButton
    private var isFullscreen = false

    private var mediaPlayer: MediaPlayer? = null
    private var isAutoPlaying = false
    private var currentSurah = 1
    private var currentAyah = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quran)

        HomeNavigator.wire(this)
        spSurahList = findViewById(R.id.spSurahList)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnRewind10 = findViewById(R.id.btnRewind10)
        btnForward10 = findViewById(R.id.btnForward10)
        tvAyahIndicator = findViewById(R.id.tvAyahIndicator)
        webViewQuran = findViewById(R.id.webViewQuran)
        progressQuranPage = findViewById(R.id.progressQuranPage)
        btnToggleFullscreen = findViewById(R.id.btnToggleFullscreen)
        tvQuranTitle = findViewById(R.id.tvQuranTitle)
        tvPageLabel = findViewById(R.id.tvPageLabel)
        llQuranControls = findViewById(R.id.llQuranControls)
        llQuranRoot = findViewById(R.id.llQuranRoot)
        btnHome = findViewById(R.id.btnHome)

        btnToggleFullscreen.setOnClickListener { toggleFullscreen() }

        setupWebView()

        val surahNames = loadSurahNames()
        val adapter = ArrayAdapter(this, R.layout.spinner_item, surahNames)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spSurahList.adapter = adapter
        goToAyah(1, 1, updateWebView = true)

        spSurahList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                stopPlayback()
                goToAyah(position + 1, 1, updateWebView = true)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnPlayPause.setOnClickListener { togglePlayPause() }

        btnRewind10.setOnClickListener {
            if (currentAyah > 1) {
                goToAyah(currentSurah, currentAyah - 1, updateWebView = true)
                if (isAutoPlaying) playCurrentAyahAudio()
            }
        }

        btnForward10.setOnClickListener {
            goToAyah(currentSurah, currentAyah + 1, updateWebView = true)
            if (isAutoPlaying) playCurrentAyahAudio()
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
            }
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

    /** ينتقل لآية معيّنة، ويأمر صفحة KSU (إن طُلب) بالانتقال لنفس الآية فتنعرض صفحتها الحقيقية تلقائياً */
    private fun goToAyah(surah: Int, ayah: Int, updateWebView: Boolean) {
        currentSurah = surah
        currentAyah = ayah
        tvAyahIndicator.text = "آية $ayah"
        if (updateWebView) {
            progressQuranPage.visibility = View.VISIBLE
            val url = "https://quran.ksu.edu.sa/m.php?l=ar#aya=${surah}_${ayah}&t=1"
            webViewQuran.loadUrl(url)
        } else {
            val js = "if (window.location) { window.location.hash = \'aya=${surah}_${ayah}&t=1\'; }"
            webViewQuran.evaluateJavascript(js, null)
        }
    }

    private fun togglePlayPause() {
        if (isAutoPlaying) {
            stopPlayback()
        } else {
            isAutoPlaying = true
            btnPlayPause.text = "⏸"
            playCurrentAyahAudio()
        }
    }

    private fun stopPlayback() {
        isAutoPlaying = false
        btnPlayPause.text = "▶"
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun playCurrentAyahAudio() {
        val surahPadded = String.format("%03d", currentSurah)
        val ayahPadded = String.format("%03d", currentAyah)
        val audioUrl = "https://everyayah.com/data/Alafasy_128kbps/$surahPadded$ayahPadded.mp3"

        mediaPlayer?.release()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioUrl)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    val nextAyah = currentAyah + 1
                    goToAyah(currentSurah, nextAyah, updateWebView = false)
                    if (isAutoPlaying) playCurrentAyahAudio()
                }
                setOnErrorListener { _, _, _ ->
                    // على الأغلب انتهت السورة (لا توجد آية بهذا الرقم)، ننتقل للسورة التالية
                    val nextSurah = currentSurah + 1
                    if (nextSurah <= 114) {
                        spSurahList.setSelection(nextSurah - 1)
                    } else {
                        stopPlayback()
                    }
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر تشغيل الصوت", Toast.LENGTH_SHORT).show()
            stopPlayback()
        }
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        val visibility = if (isFullscreen) View.GONE else View.VISIBLE
        tvQuranTitle.visibility = visibility
        spSurahList.visibility = visibility
        llQuranControls.visibility = visibility
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
        mediaPlayer?.release()
        mediaPlayer = null
        webViewQuran.destroy()
    }
}
