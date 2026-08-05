package com.jo.prayertimes

import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

data class ZikrItem(val text: String, val audioUrl: String, val count: Int)

class AzkarActivity : AppCompatActivity() {

    private lateinit var tvZikrText: TextView
    private lateinit var tvZikrCounter: TextView
    private lateinit var tvZikrTitle: TextView
    private lateinit var btnPlay: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnPrev: ImageButton
    private lateinit var spinnerCategory: Spinner

    private var mediaPlayer: MediaPlayer? = null
    private var isAudioPlaying: Boolean = false
    private var currentIndex: Int = 0
    private var currentList: List<ZikrItem> = listOf()

    // 1. أذكار الصباح - الشيخ ياسر الدوسري
    private val sabahAzkar = listOf(
        ZikrItem("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ...", "https://server11.mp3quran.net/dosr/002.mp3", 1),
        ZikrItem("أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ لاَ إِلَهَ إِلاَّ اللَّهُ...", "https://server11.mp3quran.net/dosr/112.mp3", 1)
    )

    // 2. أذكار المساء - الشيخ ياسر الدوسري
    private val msaaAzkar = listOf(
        ZikrItem("أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ لاَ إِلَهَ إِلاَّ اللَّهُ...", "https://server11.mp3quran.net/dosr/113.mp3", 1),
        ZikrItem("اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ...", "https://server11.mp3quran.net/dosr/114.mp3", 1)
    )

    // 3. تسبيح - الشيخ مشاري العفاسي
    private val tasbeehAzkar = listOf(
        ZikrItem("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ", "https://server8.mp3quran.net/afs/001.mp3", 100)
    )

    // 4. استغفار - الشيخ مشاري العفاسي
    private val istighfarAzkar = listOf(
        ZikrItem("أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ", "https://server8.mp3quran.net/afs/002.mp3", 100)
    )

    // 5. صلاة على الرسول - الشيخ مشاري العفاسي
    private val salawatAzkar = listOf(
        ZikrItem("اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ", "https://server8.mp3quran.net/afs/108.mp3", 100)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_azkar)

        tvZikrText = findViewById(R.id.tvZikrText)
        tvZikrCounter = findViewById(R.id.tvZikrCounter)
        tvZikrTitle = findViewById(R.id.tvZikrTitle)
        btnPlay = findViewById(R.id.btnPlayAzkar)
        btnNext = findViewById(R.id.btnNextAzkar)
        btnPrev = findViewById(R.id.btnPrevAzkar)

        val spinnerView: View? = findViewById(R.id.autoCompleteCategory) ?: findViewById(R.id.spinnerCategory)
        if (spinnerView is Spinner) {
            spinnerCategory = spinnerView
            setupSpinner()
        }

        btnPlay.setColorFilter(Color.WHITE)
        btnNext.setColorFilter(Color.WHITE)
        btnPrev.setColorFilter(Color.WHITE)

        loadCategory(0)

        btnPlay.setOnClickListener {
            if (isAudioPlaying) {
                pauseAudio()
            } else {
                playCurrentZikr()
            }
        }

        btnNext.setOnClickListener {
            if (currentIndex < currentList.size - 1) {
                currentIndex++
                updateUi()
                if (isAudioPlaying) playCurrentZikr()
            }
        }

        btnPrev.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                updateUi()
                if (isAudioPlaying) playCurrentZikr()
            }
        }
    }

    private fun setupSpinner() {
        val categories = arrayOf("أذكار الصباح", "أذكار المساء", "تسبيح", "استغفار", "صلاة على الرسول")
        val adapter = ArrayAdapter(this, R.layout.spinner_item, categories)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        if (::spinnerCategory.isInitialized) {
            spinnerCategory.adapter = adapter
            spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    loadCategory(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun loadCategory(position: Int) {
        pauseAudio()
        currentIndex = 0
        when (position) {
            0 -> {
                currentList = sabahAzkar
                tvZikrTitle.text = "أذكار الصباح - بصوت الشيخ ياسر الدوسري"
            }
            1 -> {
                currentList = msaaAzkar
                tvZikrTitle.text = "أذكار المساء - بصوت الشيخ ياسر الدوسري"
            }
            2 -> {
                currentList = tasbeehAzkar
                tvZikrTitle.text = "تسبيح - بصوت الشيخ مشاري العفاسي"
            }
            3 -> {
                currentList = istighfarAzkar
                tvZikrTitle.text = "استغفار - بصوت الشيخ مشاري العفاسي"
            }
            4 -> {
                currentList = salawatAzkar
                tvZikrTitle.text = "صلاة على الرسول - بصوت الشيخ مشاري العفاسي"
            }
        }
        updateUi()
    }

    private fun updateUi() {
        if (currentList.isNotEmpty()) {
            val item = currentList[currentIndex]
            tvZikrText.text = item.text
            tvZikrCounter.text = "الذكر ${currentIndex + 1} من ${currentList.size} | التكرار: ${item.count}"
        }
    }

    private fun playCurrentZikr() {
        if (currentList.isEmpty()) return
        val item = currentList[currentIndex]

        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
            } else {
                mediaPlayer?.reset()
            }

            mediaPlayer?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(item.audioUrl)
                setOnPreparedListener {
                    start()
                    this@AzkarActivity.isAudioPlaying = true
                    btnPlay.setImageResource(android.R.drawable.ic_media_pause)
                }
                setOnCompletionListener {
                    if (currentIndex < currentList.size - 1) {
                        currentIndex++
                        updateUi()
                        playCurrentZikr()
                    } else {
                        pauseAudio()
                    }
                }
                setOnErrorListener { _, _, _ ->
                    pauseAudio()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            pauseAudio()
        }
    }

    private fun pauseAudio() {
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isAudioPlaying = false
        btnPlay.setImageResource(android.R.drawable.ic_media_play)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
