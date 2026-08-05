package com.jo.prayertimes

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class QuranActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnPlay: ImageButton
    private lateinit var tvPageInfo: TextView
    private lateinit var tvCurrentAyahInfo: TextView
    private lateinit var autoCompleteSurah: AutoCompleteTextView

    private var mediaPlayer: MediaPlayer? = null
    private var isAudioPlaying: Boolean = false

    private var currentSurah: Int = 1
    private var currentAyah: Int = 1
    private var isUserSelectingSurah: Boolean = false

    private val surahNames = arrayOf(
        "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال", "التوبة", "يونس",
        "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل", "الإسراء", "الكهف", "مريم", "طه",
        "الأنبياء", "الحج", "المؤمنون", "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم",
        "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص", "الزمر", "غافر",
        "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية", "الأحقاف", "محمد", "الفتح", "الحجرات", "ق",
        "الذاريات", "الطور", "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر", "الممتحنة",
        "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم", "الملك", "القلم", "الحاقة", "المعارج",
        "نوح", "الجن", "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ", "النازعات", "عبس",
        "التكوير", "الانفطار", "المطففين", "الانشقاق", "البروج", "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد",
        "الشمس", "الليل", "الضحى", "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات",
        "القارعة", "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر",
        "المسد", "الإخلاص", "الفلق", "الناس"
    )

    private val surahStartPages = intArrayOf(
        1, 2, 50, 77, 106, 128, 151, 177, 187, 208, 221, 235, 249, 255, 262, 267, 282, 293, 305, 312,
        322, 332, 342, 350, 359, 367, 377, 385, 396, 404, 411, 415, 418, 428, 434, 440, 446, 453, 458, 467,
        477, 483, 489, 496, 499, 502, 507, 511, 515, 518, 520, 523, 526, 528, 531, 534, 537, 542, 545, 549,
        551, 553, 554, 556, 558, 560, 562, 564, 566, 568, 570, 572, 574, 575, 577, 578, 580, 582, 583, 585,
        586, 587, 587, 589, 590, 591, 591, 592, 593, 594, 595, 595, 596, 596, 597, 597, 598, 598, 599, 599,
        600, 600, 601, 601, 601, 602, 602, 602, 603, 603, 603, 604, 604, 604
    )

    private val ayahCounts = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 6, 4, 5, 6
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quran)

        viewPager = findViewById(R.id.viewPagerQuran)
        btnPlay = findViewById(R.id.btnPlayAudio)
        tvPageInfo = findViewById(R.id.tvPageInfo)
        autoCompleteSurah = findViewById(R.id.autoCompleteSurah)

        btnPlay.setColorFilter(Color.WHITE)

        setupSurahSearch()

        val adapter = QuranPagerAdapter(604)
        viewPager.adapter = adapter
        viewPager.layoutDirection = ViewPager2.LAYOUT_DIRECTION_RTL

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val pageNum = position + 1
                tvPageInfo.text = "صفحة $pageNum من 604"

                val surahIdx = getSurahIndexForPage(pageNum)
                if (surahIdx != -1 && !isUserSelectingSurah) {
                    autoCompleteSurah.setText(surahNames[surahIdx], false)
                    if (!isAudioPlaying) {
                        currentSurah = surahIdx + 1
                        currentAyah = 1
                    }
                }
            }
        })

        btnPlay.setOnClickListener {
            if (isAudioPlaying) {
                pauseAudio()
            } else {
                playCurrentAyah()
            }
        }
    }

    private fun setupSurahSearch() {
        val adapter = ArrayAdapter(this, R.layout.item_dropdown_surah, surahNames)
        autoCompleteSurah.setAdapter(adapter)
        autoCompleteSurah.threshold = 1
        autoCompleteSurah.setDropDownBackgroundDrawable(ColorDrawable(Color.parseColor("#2E2E2E")))

        autoCompleteSurah.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position) as String
            val index = surahNames.indexOf(selectedName)
            if (index != -1) {
                isUserSelectingSurah = true
                val targetPage = surahStartPages[index]
                viewPager.currentItem = targetPage - 1
                currentSurah = index + 1
                currentAyah = 1
                if (isAudioPlaying) playCurrentAyah()
                isUserSelectingSurah = false
            }
        }
    }

    private fun getSurahIndexForPage(page: Int): Int {
        var idx = surahStartPages.size - 1
        while (idx >= 0) {
            if (page >= surahStartPages[idx]) return idx
            idx--
        }
        return 0
    }

    private fun playCurrentAyah() {
        val s = String.format("%03d", currentSurah)
        val a = String.format("%03d", currentAyah)
        val audioUrl = "https://everyayah.com/data/Alafasy_128kbps/$s$a.mp3"

        // تحديث تفاصيل التظليل النصي للآية وقرينة الصفحة
        val surahName = surahNames[currentSurah - 1]
        tvPageInfo.text = "سورة $surahName - آية $currentAyah"

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
                setDataSource(audioUrl)
                setOnPreparedListener {
                    start()
                    this@QuranActivity.isAudioPlaying = true
                    btnPlay.setImageResource(android.R.drawable.ic_media_pause)
                }
                setOnCompletionListener {
                    moveToNextAyah()
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

    private fun moveToNextAyah() {
        if (currentSurah <= 114) {
            val maxAyahs = ayahCounts[currentSurah - 1]
            if (currentAyah < maxAyahs) {
                currentAyah += 1
            } else {
                currentSurah += 1
                currentAyah = 1
            }

            if (currentSurah <= 114) {
                // الانتقال التلقائي للصفحة إذا لزم الأمر
                val nextSurahStartPage = surahStartPages[currentSurah - 1]
                val currentShowingPage = viewPager.currentItem + 1
                
                // إذا تجاوزت الآية حدود الصفحة الحالية، ننتقل تلقائياً للصفحة القادمة
                if (nextSurahStartPage > currentShowingPage) {
                    viewPager.setCurrentItem(nextSurahStartPage - 1, true)
                }

                playCurrentAyah()
            } else {
                pauseAudio()
                currentSurah = 1
                currentAyah = 1
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    inner class QuranPagerAdapter(private val totalPages: Int) :
        RecyclerView.Adapter<QuranPagerAdapter.PageViewHolder>() {

        inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.imageViewQuranPage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_quran_page, parent, false)
            return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            val pageNumber = position + 1
            val pageUrl = "https://quran.ksu.edu.sa/png_big/$pageNumber.png"

            Glide.with(holder.itemView.context)
                .load(pageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imageView)
        }

        override fun getItemCount(): Int = totalPages
    }
}
