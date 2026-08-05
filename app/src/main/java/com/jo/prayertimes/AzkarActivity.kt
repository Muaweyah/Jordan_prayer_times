package com.jo.prayertimes

import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.AdapterView
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
        ZikrItem("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَنْ ذَا الَّذِي يَشْفَعُ عِنْدَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ", "https://server11.mp3quran.net/dosr/002.mp3", 1),
        ZikrItem("قل هو الله أحد، وقل أعوذ برب الفلق، وقل أعوذ برب الناس (ثلاث مرَّات)", "https://server11.mp3quran.net/dosr/112.mp3", 3),
        ZikrItem("أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ لاَ إِلَهَ إِلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَذَا الْيَوْمِ وَخَيْرَ مَا بَعْدَهُ، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَذَا الْيَوْمِ وَشَرِّ مَا بَعْدَهُ، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ.", "https://ia800902.us.archive.org/31/items/Yasser_Al-Dosari_Azkar/01_Sabah.mp3", 1),
        ZikrItem("اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ وَإِلَيْكَ النُّشُورُ.", "https://ia800902.us.archive.org/31/items/Yasser_Al-Dosari_Azkar/02_Sabah.mp3", 1),
        ZikrItem("اللَّهُمَّ أَنْتَ رَبِّي لاَ إِلَهَ إِلاَّ أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لاَ يَغْفِرُ الذُّنُوبَ إِلاَّ أَنْتَ.", "https://ia800902.us.archive.org/31/items/Yasser_Al-Dosari_Azkar/03_Sabah.mp3", 1),
        ZikrItem("اللَّهُمَّ إِنِّي أَصْبَحْتُ أُشْهِدُكَ، وَأُشْهِدُ حَمَلَةَ عَرْشِكَ، وَمَلاَئِكَتَكَ، وَجَمِيعَ خَلْقِكَ، أَنَّكَ أَنْتَ اللَّهُ لاَ إِلَهَ إِلاَّ أَنْتَ وَحْدَكَ لاَ شَرِيكَ لَهُ، وَأَنَّ مُحَمَّداً عَبْدُكَ وَرَسُولُكَ (أربع مرَّات)", "https://ia800902.us.archive.org/31/items/Yasser_Al-Dosari_Azkar/04_Sabah.mp3", 4),
        ZikrItem("اللَّهُمَّ مَا أَصْبَحَ بِي مِنْ نِعْمَةٍ أَوْ بِأَحَدٍ مِنْ خَلْقِكَ فَمِنْكَ وَحْدَكَ لاَ شَرِيكَ لَكَ، فَلَكَ الْحَمْدُ وَلَكَ الشُّكْرُ.", "https://ia800902.us.archive.org/31/items/Yasser_Al-Dosari_Azkar/05_Sabah.mp3", 1),
        ZikrItem("حَسْبِيَ اللَّهُ لاَ إِلَهَ إِلاَّ هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ (سبع مرَّات)", "https://ia800902.us.archive.org/31/items/Yasser_Al-Dosari_Azkar/06_Sabah.mp3", 7),
        ZikrItem("بِسْمِ اللَّهِ الَّذِي لاَ يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الأَرْضِ وَلاَ فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ (ثلاث مرَّات)", "https://ia800902.us.archive.org/31/items/Yasser_Al-Dosari_Azkar/07_Sabah.mp3", 3),
        ZikrItem("رَضِيتُ بِاللَّهِ رَبَّاً، وَبِالإِسْلاَمِ دِيناً، وَبِمُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ نَبِيَّاً (ثلاث مرَّات)", "https://ia800902.us.archive.org/31/items/Yasser_Al-Dosari_Azkar/08_Sabah.mp3", 3),
        ZikrItem("يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ أَصْلِحْ لِي شَأْنِي كُلَّهُ وَلاَ تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ.", "https://ia800902.us.archive.org/31/items/Yasser_Al-Dosari_Azkar/09_Sabah.mp3", 1),
        ZikrItem("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ (مائة مرَّة)", "https://ia800902.us.archive.org/31/items/Yasser_Al-Dosari_Azkar/10_Sabah.mp3", 100)
    )

    // أذكار المساء بصوت الشيخ ياسر الدوسري
    private val msaaAzkar = listOf(
        ZikrItem("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَنْ ذَا الَّذِي يَشْفَعُ عِنْدَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ", "https://server11.mp3quran.net/dosr/002.mp3", 1),
        ZikrItem("قل هو الله أحد، وقل أعوذ برب الفلق، وقل أعوذ برب الناس (ثلاث مرَّات)", "https://server11.mp3quran.net/dosr/112.mp3", 3),
        ZikrItem("أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ لاَ إِلَهَ إِلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَذِهِ اللَّيْلَةِ وَخَيْرَ مَا بَعْدَهَا، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَذِهِ اللَّيْلَةِ وَشَرِّ مَا بَعْدَهَا، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ.", "https://ia800902.us.archive.org/31/items/Yasser_Al-Dosari_Azkar/01_Masaa.mp3", 1),
        ZikrItem("اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ وَإِلَيْكَ الْمَصِيرُ.", "https://ia800902.us.archive.org/31/items/Yasser_Al-Dosari_Azkar/02_Masaa.mp3", 1),
        ZikrItem("اللَّهُمَّ أَنْتَ رَبِّي لاَ إِلَهَ إِلاَّ أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لاَ يَغْفِرُ الذُّنُوبَ إِلاَّ أَنْتَ.", "https://ia800902.us.archive.org/31/items/Yasser_Al-Dosari_Azkar/03_Sabah.mp3", 1),
        ZikrItem("أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ (ثلاث مرَّات)", "https://ia800902.us.archive.org/31/items/Yasser_Al-Dosari_Azkar/04_Masaa.mp3", 3),
        ZikrItem("اللَّهُمَّ إِنِّي أَمْسَيْتُ أُشْهِدُكَ، وَأُشْهِدُ حَمَلَةَ عَرْشِكَ، وَمَلاَئِكَتَكَ، وَجَمِيعَ خَلْقِكَ، أَنَّكَ أَنْتَ اللَّهُ لاَ إِلَهَ إِلاَّ أَنْتَ وَحْدَكَ لاَ شَرِيكَ لَهُ، وَأَنَّ مُحَمَّداً عَبْدُكَ وَرَسُولُكَ (أربع مرَّات)", "https://ia800902.us.archive.org/31/items/Yasser_Al-Dosari_Azkar/05_Masaa.mp3", 4),
        ZikrItem("اللَّهُمَّ مَا أَمْسَى بِي مِنْ نِعْمَةٍ أَوْ بِأَحَدٍ مِنْ خَلْقِكَ فَمِنْكَ وَحْدَكَ لاَ شَرِيكَ لَكَ، فَلَكَ الْحَمْدُ وَلَكَ الشُّكْرُ.", "https://ia800902.us.archive.org/31/items/Yasser_Al-Dosari_Azkar/06_Masaa.mp3", 1)
    )

    // أذكار مكررة بصوت العفاسي وبصوت ندي خالي تماماً من أي مؤثرات أو موسيقى
    private val mukarraraAzkar = listOf(
        ZikrItem("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ (مكررة بصوت الشيخ مشاري العفاسي)", "https://server8.mp3quran.net/afs/001.mp3", 100),
        ZikrItem("أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ (مكررة بصوت الشيخ مشاري العفاسي)", "https://ia800203.us.archive.org/11/items/Afast_Astaghfirullah/Astaghfirullah_Afasi.mp3", 100),
        ZikrItem("اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ (مكررة بصوت عذب ونقي)", "https://ia801503.us.archive.org/15/items/Salawat_Pure_Voice/Salawat.mp3", 100)
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
        btnNext.setColorFilter(Color.WHITE)
        btnPrev.setColorFilter(Color.WHITE)

        setupCategorySelector()
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
        val categories = arrayOf("أذكار الصباح", "أذكار المساء", "أذكار مكررة")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        autoCompleteCategory.setAdapter(adapter)
        autoCompleteCategory.setText(categories[0], false)

        autoCompleteCategory.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            loadAzkarCategory(position)
        }
    }

    private fun loadAzkarCategory(categoryIndex: Int) {
        pauseAudio()
        currentIndex = 0
        when (categoryIndex) {
            0 -> {
                currentList = sabahAzkar
                tvZikrTitle.text = "أذكار الصباح - بصوت الشيخ ياسر الدوسري"
            }
            1 -> {
                currentList = msaaAzkar
                tvZikrTitle.text = "أذكار المساء - بصوت الشيخ ياسر الدوسري"
            }
            2 -> {
                currentList = mukarraraAzkar
                tvZikrTitle.text = "أذكار مكررة - العفاسي وأصوات نقية"
            }
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
