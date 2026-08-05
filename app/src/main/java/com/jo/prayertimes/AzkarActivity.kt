package com.jo.prayertimes

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

data class ZikrItem(val text: String, val rawResId: Int, val count: Int)

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

    // 1. أذكار الصباح
    private val sabahAzkar by lazy {
        listOf(
            ZikrItem("أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ لاَ إِلَهَ إِلاَّ اللَّهُ...", R.raw.sabah, 1)
        )
    }

    // 2. أذكار المساء
    private val msaaAzkar by lazy {
        listOf(
            ZikrItem("أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ لاَ إِلَهَ إِلاَّ اللَّهُ...", R.raw.masaa, 1)
        )
    }

    // 3. تسبيح
    private val tasbeehAzkar = listOf(
        ZikrItem("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ", R.raw.tasbeeh, 100)
    )

    // 4. استغفار
    private val istighfarAzkar = listOf(
        ZikrItem("أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ", R.raw.istighfar, 100)
    )

    // 5. صلاة على الرسول
    private val salawatAzkar by lazy {
        listOf(
            ZikrItem("اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ", R.raw.salawat, 100)
        )
    }

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
        btnNext.setColorFilter(Color.WHITE)
        btnPrev.setColorFilter(Color.WHITE)

        setupCategoryDropdown()
        loadCategory(0)

        btnPlay.setOnClickListener {
            if (isAudioPlaying) pauseAudio() else playCurrentZikr()
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

    private fun setupCategoryDropdown() {
        val categories = arrayOf("أذكار الصباح", "أذكار المساء", "تسبيح", "استغفار", "صلاة على الرسول")
        val adapter = ArrayAdapter(this, R.layout.spinner_item, categories)
        autoCompleteCategory.setAdapter(adapter)
        autoCompleteCategory.setTextColor(Color.WHITE)

        autoCompleteCategory.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            loadCategory(position)
        }
    }

    private fun loadCategory(position: Int) {
        pauseAudio()
        currentIndex = 0
        when (position) {
            0 -> {
                currentList = sabahAzkar
                tvZikrTitle.text = "أذكار الصباح"
            }
            1 -> {
                currentList = msaaAzkar
                tvZikrTitle.text = "أذكار المساء"
            }
            2 -> {
                currentList = tasbeehAzkar
                tvZikrTitle.text = "تسبيح"
            }
            3 -> {
                currentList = istighfarAzkar
                tvZikrTitle.text = "استغفار"
            }
            4 -> {
                currentList = salawatAzkar
                tvZikrTitle.text = "صلاة على الرسول"
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
            stopAudio()
            mediaPlayer = MediaPlayer.create(this, item.rawResId)
            mediaPlayer?.apply {
                setOnCompletionListener {
                    if (currentIndex < currentList.size - 1) {
                        currentIndex++
                        updateUi()
                        playCurrentZikr()
                    } else {
                        pauseAudio()
                    }
                }
                start()
                isAudioPlaying = true
                btnPlay.setImageResource(android.R.drawable.ic_media_pause)
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

    private fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        isAudioPlaying = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudio()
    }
}
