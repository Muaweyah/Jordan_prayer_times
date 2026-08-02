package com.jo.prayertimes

import android.media.MediaPlayer
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
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
    private lateinit var tvAyahText: TextView

    private var mediaPlayer: MediaPlayer? = null
    private var pausedPosition = 0
    private var currentSurahIndex = -1
    private var isUserSeeking = false

    private var surahAyatMap: Map<String, List<String>> = emptyMap()

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
        tvAyahText = findViewById(R.id.tvAyahText)

        surahAyatMap = loadQuranData()

        val surahNames = loadSurahNames()
        val adapter = ArrayAdapter(this, R.layout.spinner_item, surahNames)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spSurahList.adapter = adapter
        updateAyahText(surahNames.firstOrNull())

        spSurahList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val name = surahNames.getOrNull(position) ?: ""
                updateAyahText(name)
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

    private fun loadSurahNames(): List<String> {
        return try {
            val json = assets.open("surah_names.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            Array(114) { i -> "سورة رقم ${i + 1}" }.toList()
        }
    }

    private fun loadQuranData(): Map<String, List<String>> {
        return try {
            val json = assets.open("quran.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            val map = mutableMapOf<String, List<String>>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val surahName = obj.getString("surah")
                val ayatArray = obj.getJSONArray("ayat")
                val ayatList = (0 until ayatArray.length()).map { ayatArray.getString(it) }
                map[surahName] = ayatList
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun updateAyahText(surahName: String?) {
        val ayat = surahAyatMap[surahName]
        tvAyahText.text = if (ayat != null) {
            ayat.mapIndexed { index, ayah -> "${ayah} ﴿${index + 1}﴾" }.joinToString("\n\n")
        } else {
            "نص السورة غير متوفر حالياً"
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
    }
}
