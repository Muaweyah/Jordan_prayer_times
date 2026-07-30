package com.jo.prayertimes

import android.content.Context
import android.icu.util.Calendar as IcuCalendar
import android.icu.util.ULocale
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** مناسبة جاهزة للعرض بعد حساب أقرب موعد قادم لها تلقائياً */
data class ComputedOccasion(val title: String, val dateText: String, val date: Date)

/** يحسب تاريخ كل مناسبة (دينية أو وطنية) تلقائياً كل عام، بدل الاعتماد على نص ثابت يحتاج تحديثاً يدوياً سنوياً.
 *  المناسبات الدينية تُحسب عبر تقويم أم القرى الهجري الرسمي المدمج في أندرويد،
 *  والمناسبات الوطنية ذات التاريخ الميلادي الثابت تتكرر كل عام بنفس اليوم والشهر. */
object OccasionsCalculator {

    private const val HIJRI_LOCALE = "ar_JO@calendar=islamic-umalqura"

    /** (مِعرّف عنوان المناسبة، مِعرّف نص اليوم/الشهر الهجري الثابت، الشهر الهجري 0-11، اليوم الهجري) */
    private val hijriOccasions = listOf(
        HijriOccasion(R.string.occ_islamic_new_year, R.string.occ_islamic_new_year_hijri, 0, 1),
        HijriOccasion(R.string.occ_ashura, R.string.occ_ashura_hijri, 0, 10),
        HijriOccasion(R.string.occ_mawlid, R.string.occ_mawlid_hijri, 2, 12),
        HijriOccasion(R.string.occ_isra_miraj, R.string.occ_isra_miraj_hijri, 6, 27),
        HijriOccasion(R.string.occ_ramadan_start, R.string.occ_ramadan_start_hijri, 8, 1),
        HijriOccasion(R.string.occ_laylat_qadr, R.string.occ_laylat_qadr_hijri, 8, 27),
        HijriOccasion(R.string.occ_eid_fitr, R.string.occ_eid_fitr_hijri, 9, 1),
        HijriOccasion(R.string.occ_arafah, R.string.occ_arafah_hijri, 11, 9),
        HijriOccasion(R.string.occ_eid_adha, R.string.occ_eid_adha_hijri, 11, 10)
    )

    /** (مِعرّف عنوان المناسبة، الشهر الميلادي 0-11، اليوم الميلادي) */
    private val gregorianOccasions = listOf(
        GregorianOccasion(R.string.occ_new_year, 0, 1),
        GregorianOccasion(R.string.occ_labour_day, 4, 1),
        GregorianOccasion(R.string.occ_independence_day, 4, 25),
        GregorianOccasion(R.string.occ_christmas, 11, 25)
    )

    private data class HijriOccasion(val titleRes: Int, val hijriLabelRes: Int, val month: Int, val day: Int)
    private data class GregorianOccasion(val titleRes: Int, val month: Int, val day: Int)

    fun getUpcomingOccasions(context: Context): List<ComputedOccasion> {
        val offsetDays = SettingsManager(context).hijriOffsetDays
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        val result = mutableListOf<ComputedOccasion>()

        for (occ in hijriOccasions) {
            val date = nextHijriOccurrence(occ.month, occ.day, today.timeInMillis, offsetDays)
            val hijriLabel = context.getString(occ.hijriLabelRes)
            val gregorianPrefix = context.getString(R.string.occ_gregorian_prefix)
            val dateText = "$hijriLabel - $gregorianPrefix ${formatDate(date)}"
            result.add(ComputedOccasion(context.getString(occ.titleRes), dateText, date))
        }

        for (occ in gregorianOccasions) {
            val date = nextGregorianOccurrence(occ.month, occ.day, today)
            result.add(ComputedOccasion(context.getString(occ.titleRes), formatDate(date), date))
        }

        return result.sortedBy { it.date }
    }

    private fun buildHijriDate(year: Int, month: Int, day: Int): Date {
        val cal = IcuCalendar.getInstance(ULocale(HIJRI_LOCALE))
        cal.clear()
        cal.set(IcuCalendar.YEAR, year)
        cal.set(IcuCalendar.MONTH, month)
        cal.set(IcuCalendar.DAY_OF_MONTH, day)
        return cal.time
    }

    private fun nextHijriOccurrence(month: Int, day: Int, todayMillis: Long, offsetDays: Int): Date {
        val hijriNow = IcuCalendar.getInstance(ULocale(HIJRI_LOCALE))
        val currentHijriYear = hijriNow.get(IcuCalendar.YEAR)

        var candidate = buildHijriDate(currentHijriYear, month, day)
        var candidateMillis = candidate.time - offsetDays * 86_400_000L
        if (candidateMillis < todayMillis) {
            candidate = buildHijriDate(currentHijriYear + 1, month, day)
            candidateMillis = candidate.time - offsetDays * 86_400_000L
        }
        return Date(candidateMillis)
    }

    private fun nextGregorianOccurrence(month: Int, day: Int, today: Calendar): Date {
        val cal = today.clone() as Calendar
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, day)
        if (cal.timeInMillis < today.timeInMillis) {
            cal.add(Calendar.YEAR, 1)
        }
        return cal.time
    }

    private fun formatDate(date: Date): String =
        SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(date)
}
