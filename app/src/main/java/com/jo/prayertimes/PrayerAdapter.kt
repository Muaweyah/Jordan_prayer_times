package com.jo.prayertimes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class PrayerAdapter(
    private var prayerList: List<PrayerItem>,
    private val settingsManager: SettingsManager,
    private val onBellClick: (PrayerItem, Boolean) -> Unit
) : RecyclerView.Adapter<PrayerAdapter.PrayerViewHolder>() {

    /** ارتفاع كل بطاقة بالبكسل، يُحسب ليملأ كل الصلوات الشاشة دون تمرير */
    private var itemHeightPx: Int = 0

    fun setItemHeight(heightPx: Int) {
        if (heightPx > 0 && heightPx != itemHeightPx) {
            itemHeightPx = heightPx
            notifyDataSetChanged()
        }
    }

    class PrayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.card_prayer)
        val iconView: ImageView = view.findViewById(R.id.iv_prayer_icon)
        val nameView: TextView = view.findViewById(R.id.tv_prayer_name)
        val timeView: TextView = view.findViewById(R.id.tv_prayer_time)
        val bellButton: ImageButton = view.findViewById(R.id.btn_bell_toggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrayerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_prayer, parent, false)
        return PrayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PrayerViewHolder, position: Int) {
        if (itemHeightPx > 0) {
            val params = holder.itemView.layoutParams
            if (params.height != itemHeightPx) {
                params.height = itemHeightPx
                holder.itemView.layoutParams = params
            }
        }

        val item = prayerList[position]
        holder.nameView.text = item.name
        holder.timeView.text = item.time

        val prayer = item.prayer
        if (prayer != null) {
            holder.iconView.setImageResource(iconFor(prayer))
            holder.iconView.setBackgroundResource(bgFor(prayer))

            val isEnabled = settingsManager.isNotificationEnabled(prayer)
            holder.bellButton.visibility = View.VISIBLE
            holder.bellButton.alpha = if (isEnabled) 1.0f else 0.35f
            holder.bellButton.setOnClickListener {
                val newState = !settingsManager.isNotificationEnabled(prayer)
                settingsManager.setNotificationEnabled(prayer, newState)
                holder.bellButton.alpha = if (newState) 1.0f else 0.35f
                onBellClick(item, newState)
            }
        } else {
            // الشروق ليس وقت صلاة: لا أذان له ولا زر جرس
            holder.iconView.setImageResource(R.drawable.ic_sun)
            holder.iconView.setBackgroundResource(R.drawable.bg_icon_sunrise)
            holder.bellButton.visibility = View.INVISIBLE
            holder.bellButton.setOnClickListener(null)
        }

        if (item.isNext) {
            holder.cardView.setCardBackgroundColor(0xFF2D3748.toInt())
            holder.timeView.setTextColor(0xFF10B981.toInt())
        } else {
            holder.cardView.setCardBackgroundColor(0xFF1F2937.toInt())
            holder.timeView.setTextColor(0xFFF59E0B.toInt())
        }
    }

    private fun iconFor(prayer: Prayer): Int = when (prayer) {
        Prayer.FAJR -> R.drawable.ic_moon
        Prayer.DHUHR -> R.drawable.ic_sun
        Prayer.ASR -> R.drawable.ic_sun
        Prayer.MAGHRIB -> R.drawable.ic_sunset
        Prayer.ISHA -> R.drawable.ic_moon
    }

    private fun bgFor(prayer: Prayer): Int = when (prayer) {
        Prayer.FAJR -> R.drawable.bg_icon_fajr
        Prayer.DHUHR -> R.drawable.bg_icon_dhuhr
        Prayer.ASR -> R.drawable.bg_icon_asr
        Prayer.MAGHRIB -> R.drawable.bg_icon_maghrib
        Prayer.ISHA -> R.drawable.bg_icon_isha
    }

    override fun getItemCount(): Int = prayerList.size

    fun updateData(newList: List<PrayerItem>) {
        prayerList = newList
        notifyDataSetChanged()
    }
}
