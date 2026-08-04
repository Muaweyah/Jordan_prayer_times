package com.jo.prayertimes

import android.media.MediaPlayer
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private lateinit var seekBarQuran: SeekBar
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
    private var pausedPosition = 0
    private var currentSurahIndex = -1
    private var isUserSeeking = false

    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                if (it.isPlaying && !isUserSeeking) {
                    seekBarQuran.progress = it.currentPosition
                }
            }
            progressHandler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quran)

        HomeNavigator.wire(this)
        spSurahList = findViewById(R.id.spSurahList)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnRewind10 = findViewById(R.id.btnRewind10)
        btnForward10 = findViewById(R.id.btnForward10)
        seekBarQuran = findViewById(R.id.seekBarQuran)
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
        loadQuranPage(1)

        spSurahList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadQuranPage(position + 1)
                if (position != currentSurahIndex) {
                    // السورة المختارة تغيّرت، لا نستأنف من موضع سورة أخرى
                    pausedPosition = 0
                    seekBarQuran.progress = 0
                    btnPlayPause.text = "▶"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        seekBarQuran.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                seekBar?.let { mediaPlayer?.seekTo(it.progress) }
            }
        })

        btnPlayPause.setOnClickListener {
            val mp = mediaPlayer
            if (mp != null && mp.isPlaying) {
                mp.pause()
                pausedPosition = mp.currentPosition
                btnPlayPause.text = "▶"
            } else {
                val selectedPosition = spSurahList.selectedItemPosition
                playAudio(selectedPosition + 1, selectedPosition)
                btnPlayPause.text = "⏸"
            }
        }

        btnRewind10.setOnClickListener {
            mediaPlayer?.let {
                val newPos = (it.currentPosition - 10000).coerceAtLeast(0)
                it.seekTo(newPos)
                seekBarQuran.progress = newPos
            }
        }

        btnForward10.setOnClickListener {
            mediaPlayer?.let {
                val newPos = (it.currentPosition + 10000).coerceAtMost(it.duration)
                it.seekTo(newPos)
                seekBarQuran.progress = newPos
            }
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
                openTilawaPanel(view)
            }
        }
    }

    /** يحاول فتح قائمة القرّاء (زر "تلاوة") داخل صفحة Quranflash تلقائياً بعد تحميلها.
     * الصفحة تطبيق ويب حديث يُبنى محتواه بجافاسكربت بعد التحميل الأولي، لذلك نكرر المحاولة
     * على فترات قصيرة لضمان أن الزر أصبح موجوداً فعلياً قبل محاولة الضغط عليه. */
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

    private fun loadQuranPage(surahNumber: Int) {
        progressQuranPage.visibility = View.VISIBLE
        val url = "https://app.quranflash.com/book/Medina1?ar#/reader/chapter/$surahNumber"
        webViewQuran.loadUrl(url)
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

    private fun playAudio(surahNumber: Int, surahIndex: Int) {
        if (mediaPlayer != null && !mediaPlayer!!.isPlaying && pausedPosition > 0 && surahIndex == currentSurahIndex) {
            mediaPlayer?.start()
            progressHandler.removeCallbacks(progressRunnable)
            progressHandler.post(progressRunnable)
            return
        }

        mediaPlayer?.release()
        progressHandler.removeCallbacks(progressRunnable)
        currentSurahIndex = surahIndex
        val formattedNumber = String.format("%03d", surahNumber)
        val audioUrl = "https://server8.mp3quran.net/afs/$formattedNumber.mp3"

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioUrl)
                prepareAsync()
                setOnPreparedListener { mp ->
                    seekBarQuran.max = mp.duration
                    if (pausedPosition > 0) {
                        mp.seekTo(pausedPosition)
                    }
                    mp.start()
                    progressHandler.removeCallbacks(progressRunnable)
                    progressHandler.post(progressRunnable)
                }
                setOnCompletionListener {
                    pausedPosition = 0
                    seekBarQuran.progress = 0
                    btnPlayPause.text = "▶"
                    progressHandler.removeCallbacks(progressRunnable)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر تشغيل الصوت", Toast.LENGTH_SHORT).show()
            btnPlayPause.text = "▶"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        progressHandler.removeCallbacks(progressRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
        webViewQuran.destroy()
    }
}
