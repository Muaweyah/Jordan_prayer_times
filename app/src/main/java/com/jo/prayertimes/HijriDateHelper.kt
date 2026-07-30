package com.jo.prayertimes

import android.content.Context
import android.icu.util.Calendar as IcuCalendar
import android.icu.util.ULocale

/** يحسب التاريخ الهجري الفعلي (تقويم أم القرى)، مع إمكانية تصحيحه يدوياً بفارق أيام من الإعدادات */
object HijriDateHelper {

    private val hijriMonths = arrayOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    fun getFormattedHijriDate(context: Context): String {
        val offsetDays = SettingsManager(context).hijriOffsetDays
        val hijri = IcuCalendar.getInstance(ULocale("ar_JO@calendar=islamic-umalqura"))
        if (offsetDays != 0) {
            hijri.add(IcuCalendar.DAY_OF_MONTH, offsetDays)
        }
        val day = hijri.get(IcuCalendar.DAY_OF_MONTH)
        val monthIndex = hijri.get(IcuCalendar.MONTH)
        val year = hijri.get(IcuCalendar.YEAR)
        val monthName = hijriMonths.getOrElse(monthIndex) { "" }
        return "$day $monthName $year هـ"
    }
}
