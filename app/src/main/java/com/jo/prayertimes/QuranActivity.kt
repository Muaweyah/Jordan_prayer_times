package com.jo.prayertimes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide

class QuranActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnPlay: ImageButton
    private var player: ExoPlayer? = null
    private var isPlaying = false

    private var currentSurah = 1
    private var currentAyah = 1

    // عدد آيات سور القرآن الـ 114 بالترتيب
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

        setupPlayer()

        val adapter = QuranPagerAdapter(604)
        viewPager.adapter = adapter
        viewPager.layoutDirection = ViewPager2.LAYOUT_DIRECTION_RTL

        btnPlay.setOnClickListener {
            if (isPlaying) {
                pauseAudio()
            } else {
                playCurrentAyah()
            }
        }
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        moveToNextAyah()
                    }
                }
            })
        }
    }

    private fun playCurrentAyah() {
        val s = String.format("%03d", currentSurah)
        val a = String.format("%03d", currentAyah)
        val audioUrl = "https://everyayah.com/data/Alafasy_128kbps/${s}${a}.mp3"

        player?.let { p ->
            val mediaItem = MediaItem.fromUri(audioUrl)
            p.setMediaItem(mediaItem)
            p.prepare()
            p.play()
            isPlaying = true
            btnPlay.setImageResource(android.R.drawable.ic_media_pause)
        }
    }

    private fun pauseAudio() {
        player?.pause()
        isPlaying = false
        btnPlay.setImageResource(android.R.drawable.ic_media_play)
    }

    private fun moveToNextAyah() {
        if (currentSurah <= 114) {
            val maxAyahs = ayahCounts[currentSurah - 1]
            if (currentAyah < maxAyahs) {
                currentAyah++
            } else {
                currentSurah++
                currentAyah = 1
            }
            if (currentSurah <= 114) {
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
        player?.release()
        player = null
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
            val imageUrl = "https://cdn.islamic.network/quran/images/high-res/${pageNumber}.png"

            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .into(holder.imageView)
        }

        override fun getItemCount(): Int = totalPages
    }
}
