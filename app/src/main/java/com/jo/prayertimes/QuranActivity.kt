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
    private lateinit var btnPlayQuran: Button
    private lateinit var btnPauseQuran: Button
    private lateinit var seekBarQuran: SeekBar
    private lateinit var tvCurrentSurahName: TextView
    private lateinit var tvTimeRemaining: TextView

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
                    updateRemainingTime(it.duration - it.currentPosition)
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
        btnPlayQuran = findViewById(R.id.btnPlayQuran)
        btnPauseQuran = findViewById(R.id.btnPauseQuran)
        seekBarQuran = findViewById(R.id.seekBarQuran)
        tvCurrentSurahName = findViewById(R.id.tvCurrentSurahName)
        tvTimeRemaining = findViewById(R.id.tvTimeRemaining)

        val surahNames = loadSurahNames()
        val adapter = ArrayAdapter(this, R.layout.spinner_item, surahNames)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spSurahList.adapter = adapter
        tvCurrentSurahName.text = surahNames.firstOrNull() ?: ""

        spSurahList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                tvCurrentSurahName.text = surahNames.getOrNull(position) ?: ""
                if (position != currentSurahIndex) {
                    // السورة المختارة تغيّرت، لا نستأنف من موضع سورة أخرى
                    pausedPosition = 0
                    seekBarQuran.progress = 0
                    updateRemainingTime(0)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        seekBarQuran.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.let { updateRemainingTime(it.duration - progress) }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                seekBar?.let { mediaPlayer?.seekTo(it.progress) }
            }
        })

        btnPlayQuran.setOnClickListener {
            val selectedPosition = spSurahList.selectedItemPosition
            playAudio(selectedPosition + 1, selectedPosition)
        }

        btnPauseQuran.setOnClickListener {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    pausedPosition = it.currentPosition
                    Toast.makeText(this, "تم الإيقاف المؤقت", Toast.LENGTH_SHORT).show()
                }
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

    private fun updateRemainingTime(remainingMs: Int) {
        val safeMs = remainingMs.coerceAtLeast(0)
        val totalSeconds = safeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        tvTimeRemaining.text = String.format("الوقت المتبقي: %02d:%02d", minutes, seconds)
    }

    private fun playAudio(surahNumber: Int, surahIndex: Int) {
        if (mediaPlayer != null && !mediaPlayer!!.isPlaying && pausedPosition > 0 && surahIndex == currentSurahIndex) {
            mediaPlayer?.start()
            progressHandler.removeCallbacks(progressRunnable)
            progressHandler.post(progressRunnable)
            Toast.makeText(this, "استئناف التلاوة...", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@QuranActivity, "جاري تلاوة السورة", Toast.LENGTH_SHORT).show()
                }
                setOnCompletionListener {
                    pausedPosition = 0
                    seekBarQuran.progress = 0
                    updateRemainingTime(0)
                    progressHandler.removeCallbacks(progressRunnable)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر تشغيل الصوت", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        progressHandler.removeCallbacks(progressRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
