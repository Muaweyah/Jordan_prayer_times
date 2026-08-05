package com.jo.prayertimes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide

class QuranActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnPlay: ImageButton
    private var player: ExoPlayer? = null
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quran)

        viewPager = findViewById(R.id.viewPagerQuran)
        btnPlay = findViewById(R.id.btnPlayAudio)

        player = ExoPlayer.Builder(this).build()

        val adapter = QuranPagerAdapter(604)
        viewPager.adapter = adapter
        viewPager.layoutDirection = ViewPager2.LAYOUT_DIRECTION_RTL

        btnPlay.setOnClickListener {
            if (isPlaying) {
                player?.pause()
                btnPlay.setImageResource(android.R.drawable.ic_media_play)
                isPlaying = false
            } else {
                val audioUrl = "https://everyayah.com/data/Alafasy_128kbps/001001.mp3"
                val mediaItem = MediaItem.fromUri(audioUrl)
                player?.setMediaItem(mediaItem)
                player?.prepare()
                player?.play()
                btnPlay.setImageResource(android.R.drawable.ic_media_pause)
                isPlaying = true
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
            val formattedPage = String.format("%03d", pageNumber)
            val imageUrl = "https://quran.ksu.edu.jo/png_big/$formattedPage.png"

            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .into(holder.imageView)
        }

        override fun getItemCount(): Int = totalPages
    }
}
