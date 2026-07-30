package com.jo.prayertimes

/** عنصر عرض في قائمة الصلوات. prayer=null يعني وقت الشروق (لا أذان له ولا زر جرس) */
data class PrayerItem(
    val name: String,
    val time: String,
    val prayer: Prayer? = null,
    val isNext: Boolean = false
)
