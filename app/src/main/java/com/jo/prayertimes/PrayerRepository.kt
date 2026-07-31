package com.jo.prayertimes

import android.content.Context
import java.util.Calendar

/** يحسب مواقيت الصلاة ليوم معيّن بالاعتماد على إحداثيات المحافظة المختارة */
class PrayerRepository(private val context: Context) {

    fun timesFor(regionArabicName: String, calendar: Calendar): DayPrayerTimes {
        val governorate = JordanGovernorates.values()
            .find { it.arabicName == regionArabicName }
            ?: JordanGovernorates.AMMAN

        val officialZoneKey = governorate.officialZoneKey
        if (officialZoneKey != null) {
            val officialTimes = OfficialAwqafPrayerTimes.timesFor(context, officialZoneKey, calendar)
            if (officialTimes != null) return officialTimes
            // لم تُغطَّ هذه السنة بالجدول الرسمي (الجدول يغطي 2026 فقط): نعتمد الحساب الفلكي المعايَر كبديل
        }

        val coordinates = PrayerCalculator.Coordinates(governorate.lat, governorate.lng)
        return PrayerCalculator.calculate(calendar, coordinates)
    }
}
