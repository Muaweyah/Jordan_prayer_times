package com.jo.prayertimes

import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
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
    private lateinit var autoCompleteCategory: AutoCompleteTextView

    private var mediaPlayer: MediaPlayer? = null
    private var isAudioPlaying: Boolean = false
    private var currentIndex: Int = 0
    private var currentList: List<ZikrItem> = listOf()

    // أذكار الصباح بصوت الشيخ ياسر الدوسري
    private val sabahAzkar = listOf(
        ZikrItem("أَكْبَرُ اللَّهِ لاَ إِلَٰهَ إِلاَّ هُوَ الْحَيُّ الْقَيُّومُ... (آية الكرسي)", "https://server11.mp3quran.net/dosr/002.mp3", 1),
        ZikrItem("أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ لاَ إِلَهَ إِلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ...", "https://backup.islamway.net/one/dossary/01_sabah.mp3", 1),
        ZikrItem("اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ وَإِلَيْكَ النُّشُورُ.", "https://backup.islamway.net/one/dossary/02_sabah.mp3", 1),
        ZikrItem("اللَّهُمَّ أَنْتَ رَبِّي لاَ إِلَهَ إِلاَّ أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ... (سيد الاستغفار)", "https://backup.islamway.net/one/dossary/03_sabah.mp3", 1)
    )

    // أذكار المساء بصوت الشيخ ياسر الدوسري
    private val msaaAzkar = listOf(
        ZikrItem("أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ لاَ إِلَهَ إِلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ...", "https://backup.islamway.net/one/dossary/01_masaa.mp3", 1),
        ZikrItem("اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ وَإِلَيْكَ الْمَصِيرُ.", "https://backup.islamway.net/one/dossary/02_masaa.mp3", 1),
        ZikrItem("أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ.", "https://backup.islamway.net/one/dossary/03_masaa.mp3", 3)
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
        autoCompleteCategory = findViewById(R.id.autoCompleteCategory)

        btnPlay.setColorFilter(Color.WHITE)

        setupCategorySelector()
        
        // البدء بأذكار الصباح افتراضياً
        loadAzkarCategory(0)

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
                updateUiForCurrentZikr()
                if (isAudioPlaying) playCurrentZikr()
            }
        }

        btnPrev.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                updateUiForCurrentZikr()
                if (isAudioPlaying) playCurrentZikr()
            }
        }
    }

    private fun setupCategorySelector() {
        val categories = arrayOf("أذكار الصباح", "أذكار المساء")
        val adapter = ArrayAdapter(this, R.layout.item_dropdown_surah, categories)
        autoCompleteCategory.setAdapter(adapter)
        autoCompleteCategory.setText(categories[0], false)

        autoCompleteCategory.setOnItemClickListener { _, _, position, _ ->
            loadAzkarCategory(position)
        }
    }

    private fun loadAzkarCategory(categoryIndex: Int) {
        pauseAudio()
        currentIndex = 0
        if (categoryIndex == 0) {
            currentList = sabahAzkar
            tvZikrTitle.text = "أذكار الصباح - بصوت الشيخ ياسر الدوسري"
        } else {
            currentList = msaaAzkar
            tvZikrTitle.text = "أذكار المساء - بصوت الشيخ ياسر الدوسري"
        }
        updateUiForCurrentZikr()
    }

    private fun updateUiForCurrentZikr() {
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
                    // الانتقال التلقائي للذكر التالي عند انتهاء الصوت (مثل القرآن)
                    if (currentIndex < currentList.size - 1) {
                        currentIndex++
                        updateUiForCurrentZikr()
                        playCurrentZikr()
                    } else {
                        pauseAudio()
                        currentIndex = 0
                        updateUiForCurrentZikr()
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
