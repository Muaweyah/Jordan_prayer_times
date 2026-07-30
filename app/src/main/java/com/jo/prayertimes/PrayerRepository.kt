package com.jo.prayertimes

import android.content.Context
import java.util.Calendar

/** يحسب مواقيت الصلاة ليوم معيّن بالاعتماد على إحداثيات المحافظة المختارة */
class PrayerRepository(private val context: Context) {

    fun timesFor(regionArabicName: String, calendar: Calendar): DayPrayerTimes {
        val governorate = JordanGovernorates.values()
            .find { it.arabicName == regionArabicName }
            ?: JordanGovernorates.AMMAN

        val coordinates = PrayerCalculator.Coordinates(governorate.lat, governorate.lng)
        return PrayerCalculator.calculate(calendar, coordinates)
    }
}
