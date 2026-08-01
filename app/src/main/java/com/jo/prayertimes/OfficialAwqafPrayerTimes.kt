package com.jo.prayertimes

import android.content.Context
import org.json.JSONObject
import java.util.Calendar

/**
 * يقرأ الجدول الرسمي الصادر عن وزارة الأوقاف والشؤون والمقدسات الإسلامية الأردنية لعام 2026
 * (المرفق كملف assets/prayer_times_2026.json، وأصله في calendar2026.pdf)، ويوفّر مواقيت الصلاة
 * لأي منطقة من مناطق هذا الجدول ليوم معيّن، بدل الاعتماد على الحساب الفلكي المعايَر.
 *
 * الجدول مبني على 365 مفتاحاً ("0" إلى "364") يمثّل كل منها رقم اليوم في السنة (اليوم الأول
 * من كانون الثاني = "0")، وتحت كل يوم توجد كل "منطقة" رسمية بمواقيتها الست.
 */
object OfficialAwqafPrayerTimes {

    private const val ASSET_FILE_NAME = "prayer_times_2026.json"
    private const val TABLE_YEAR = 2026

    // البنية بعد التحميل: يوم في السنة (0-364) -> اسم المنطقة الرسمي -> المواقيت
    private var cachedTable: Map<Int, Map<String, DayPrayerTimes>>? = null

    /**
     * يعيد مواقيت الصلاة الرسمية لمنطقة معيّنة في تاريخ معيّن، أو null إذا كانت السنة المطلوبة
     * غير 2026 (الجدول يغطي هذه السنة فقط)، أو إذا لم توجد المنطقة أو اليوم في الجدول.
     */
    fun timesFor(context: Context, officialZoneKey: String, calendar: Calendar): DayPrayerTimes? {
        if (calendar.get(Calendar.YEAR) != TABLE_YEAR) return null
        val table = loadTable(context) ?: return null
        val dayOfYearIndex = calendar.get(Calendar.DAY_OF_YEAR) - 1
        return table[dayOfYearIndex]?.get(officialZoneKey)
    }

    /** يحوّل وقتاً صباحياً (فجر/شروق) دون أي تعديل على الساعة */
    private fun normalizeMorningTime(raw: String): String {
        val parts = raw.split(":")
        val hour = parts[0].trim().toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        return String.format("%02d:%02d", hour, minute)
    }

    /** يحوّل وقتاً بعد الظهر (ظهر/عصر/مغرب/عشاء) من صيغة 12 ساعة الغامضة إلى 24 ساعة صحيحة */
    private fun normalizeAfternoonTime(raw: String): String {
        val parts = raw.split(":")
        var hour = parts[0].trim().toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        if (hour in 1..11) hour += 12
        return String.format("%02d:%02d", hour, minute)
    }

    private fun loadTable(context: Context): Map<Int, Map<String, DayPrayerTimes>>? {
        cachedTable?.let { return it }
        return try {
            val json = context.assets.open(ASSET_FILE_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(json)
            val parsed = mutableMapOf<Int, Map<String, DayPrayerTimes>>()
            root.keys().forEach { dayKey ->
                val dayObject = root.getJSONObject(dayKey)
                val zones = mutableMapOf<String, DayPrayerTimes>()
                dayObject.keys().forEach { zoneKey ->
                    val zoneTimes = dayObject.getJSONObject(zoneKey)
                    zones[zoneKey] = DayPrayerTimes(
                        fajr = normalizeMorningTime(zoneTimes.getString("fajr")),
                        sunrise = normalizeMorningTime(zoneTimes.getString("shorouq")),
                        dhuhr = normalizeAfternoonTime(zoneTimes.getString("dhuhr")),
                        asr = normalizeAfternoonTime(zoneTimes.getString("asr")),
                        maghrib = normalizeAfternoonTime(zoneTimes.getString("maghrib")),
                        isha = normalizeAfternoonTime(zoneTimes.getString("isha"))
                    )
                }
                parsed[dayKey.toInt()] = zones
            }
            cachedTable = parsed
            parsed
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
