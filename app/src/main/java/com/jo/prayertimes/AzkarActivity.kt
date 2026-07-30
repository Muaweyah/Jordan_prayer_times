package com.jo.prayertimes

import android.media.MediaPlayer
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.util.Locale

class AzkarActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var btnMorning: Button
    private lateinit var btnEvening: Button
    private lateinit var tvNowPlaying: TextView
    private lateinit var tvTimeRemaining: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var btnPlay: Button
    private lateinit var btnPause: Button
    private lateinit var container: LinearLayout

    private var mediaPlayer: MediaPlayer? = null
    private var isMorning = true

    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    seekBar.progress = it.currentPosition
                    updateTimeRemaining(it)
                }
            }
            progressHandler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_azkar)

        HomeNavigator.wire(this)
        btnMorning = findViewById(R.id.btnMorning)
        btnEvening = findViewById(R.id.btnEvening)
        tvNowPlaying = findViewById(R.id.tvAzkarNowPlaying)
        tvTimeRemaining = findViewById(R.id.tvAzkarTimeRemaining)
        seekBar = findViewById(R.id.seekBarAzkar)
        btnPlay = findViewById(R.id.btnPlayAzkar)
        btnPause = findViewById(R.id.btnPauseAzkar)
        container = findViewById(R.id.containerAzkarList)

        btnMorning.setOnClickListener { switchTo(morning = true) }
        btnEvening.setOnClickListener { switchTo(morning = false) }

        btnPlay.setOnClickListener { playAudio() }
        btnPause.setOnClickListener { pauseAudio() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        switchTo(morning = true)
    }

    private fun switchTo(morning: Boolean) {
        isMorning = morning
        stopAudio()
        highlightButtons()
        renderList()
        prepareAudio()
    }

    private fun highlightButtons() {
        if (isMorning) {
            btnMorning.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
            btnMorning.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            btnEvening.backgroundTintList = ContextCompat.getColorStateList(this, R.color.surface_alt)
            btnEvening.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        } else {
            btnEvening.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
            btnEvening.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            btnMorning.backgroundTintList = ContextCompat.getColorStateList(this, R.color.surface_alt)
            btnMorning.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        }
    }

    private fun loadAzkarList(): List<String> {
        val jsonText = assets.open("azkar.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(jsonText)
        val key = if (isMorning) "أذكار الصباح" else "أذكار المساء"
        val arr = root.getJSONArray(key)
        val list = ArrayList<String>()
        for (i in 0 until arr.length()) list.add(arr.getString(i))
        return list
    }

    private fun renderList() {
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val list = loadAzkarList()
        for (text in list) {
            val card = inflater.inflate(R.layout.item_zikr_card, container, false)
            card.findViewById<TextView>(R.id.zikrText).text = text
            container.addView(card)
        }
    }

    private fun prepareAudio() {
        stopAudio()
        val resId = if (isMorning) R.raw.tasbeeh else R.raw.istighfar
        mediaPlayer = MediaPlayer.create(this, resId)
        mediaPlayer?.let { player ->
            seekBar.max = player.duration
            seekBar.progress = 0
            updateTimeRemaining(player)
            player.setOnCompletionListener {
                seekBar.progress = 0
                updateTimeRemaining(mediaPlayer)
            }
        }
        tvNowPlaying.text = if (isMorning)
            "استماع للتسبيح"
        else
            "استماع للاستغفار"
    }

    private fun playAudio() {
        if (mediaPlayer == null) prepareAudio()
        mediaPlayer?.start()
        progressHandler.post(progressRunnable)
    }

    private fun pauseAudio() {
        mediaPlayer?.let { if (it.isPlaying) it.pause() }
    }

    private fun stopAudio() {
        progressHandler.removeCallbacks(progressRunnable)
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    private fun updateTimeRemaining(player: MediaPlayer?) {
        if (player == null) {
            tvTimeRemaining.text = "الوقت المتبقي: 00:00"
            return
        }
        val remainingMs = (player.duration - player.currentPosition).coerceAtLeast(0)
        val totalSeconds = remainingMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        tvTimeRemaining.text = String.format(Locale("ar"), "الوقت المتبقي: %02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudio()
    }
}
