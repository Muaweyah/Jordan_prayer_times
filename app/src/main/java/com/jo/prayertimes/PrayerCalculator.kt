package com.jo.prayertimes

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

data class DayPrayerTimes(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String
)

object PrayerCalculator {

    var fajrAngle = 18.5
    var ishaAngle = 17.5
    var asrShadowFactor = 1.0

    private const val DEG_TO_RAD = PI / 180.0
    private const val RAD_TO_DEG = 180.0 / PI

    data class Coordinates(val latitude: Double, val longitude: Double)

    fun calculate(
        calendar: Calendar,
        coordinates: Coordinates,
        timeZoneId: String = "Asia/Amman"
    ): DayPrayerTimes {
        val tz = TimeZone.getTimeZone(timeZoneId)
        val cal = calendar.clone() as Calendar
        cal.timeZone = tz

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val jd = julianDate(year, month, day)
        val utcOffsetHours = tz.getOffset(cal.timeInMillis) / 3600000.0
        val lat = coordinates.latitude
        val lng = coordinates.longitude

        val dhuhrMinutes = computeDhuhr(jd, lng, utcOffsetHours)
        val fajrMinutes = dhuhrMinutes - hourAngle(fajrAngle, lat, jd) * 60.0
        val sunriseMinutes = dhuhrMinutes - hourAngle(0.833, lat, jd) * 60.0
        val asrMinutes = dhuhrMinutes + asrHourAngle(asrShadowFactor, lat, jd) * 60.0
        val maghribMinutes = dhuhrMinutes + hourAngle(0.833, lat, jd) * 60.0
        val ishaMinutes = dhuhrMinutes + hourAngle(ishaAngle, lat, jd) * 60.0

        return DayPrayerTimes(
            fajr = formatTime(fajrMinutes),
            sunrise = formatTime(sunriseMinutes),
            dhuhr = formatTime(dhuhrMinutes),
            asr = formatTime(asrMinutes),
            maghrib = formatTime(maghribMinutes),
            isha = formatTime(ishaMinutes)
        )
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year; var m = month
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = (357.529 + 0.98560028 * d) % 360.0
        val q = (280.459 + 0.98564736 * d) % 360.0
        val l = (q + 1.915 * sin(g * DEG_TO_RAD) + 0.020 * sin(2 * g * DEG_TO_RAD)) % 360.0
        val e = 23.439 - 0.00000036 * d
        val decl = asin(sin(e * DEG_TO_RAD) * sin(l * DEG_TO_RAD)) * RAD_TO_DEG
        var ra = atan2(cos(e * DEG_TO_RAD) * sin(l * DEG_TO_RAD), cos(l * DEG_TO_RAD)) * RAD_TO_DEG
        ra = normalizeHours(ra / 15.0)
        var eqt = q / 15.0 - ra
        eqt = normalizeHours(eqt + 12.0) - 12.0
        return Pair(decl, eqt)
    }

    private fun normalizeHours(hours: Double): Double {
        var h = hours
        h -= 24.0 * floor(h / 24.0)
        return h
    }

    private fun computeDhuhr(jd: Double, longitude: Double, utcOffset: Double): Double {
        val (_, eqt) = sunPosition(jd)
        val dhuhrHours = 12.0 + utcOffset - (longitude / 15.0) - eqt
        return dhuhrHours * 60.0
    }

    private fun hourAngle(angleDeg: Double, latitude: Double, jd: Double): Double {
        val (decl, _) = sunPosition(jd)
        val latRad = latitude * DEG_TO_RAD
        val declRad = decl * DEG_TO_RAD
        val angleRad = angleDeg * DEG_TO_RAD
        val cosH = (-sin(angleRad) - sin(latRad) * sin(declRad)) / (cos(latRad) * cos(declRad))
        return acos(cosH.coerceIn(-1.0, 1.0)) * RAD_TO_DEG / 15.0
    }

    private fun asrHourAngle(shadowFactor: Double, latitude: Double, jd: Double): Double {
        val (decl, _) = sunPosition(jd)
        val latRad = latitude * DEG_TO_RAD
        val declRad = decl * DEG_TO_RAD
        val altitudeDeg = atan(1.0 / (shadowFactor + tan(abs(latRad - declRad)))) * RAD_TO_DEG
        return hourAngle(-altitudeDeg, latitude, jd)
    }

    private fun formatTime(totalMinutes: Double): String {
        val minutes = ((totalMinutes % 1440.0) + 1440.0) % 1440.0
        val h = floor(minutes / 60.0).toInt()
        val m = floor(minutes % 60.0).toInt()
        return String.format("%02d:%02d", h, m)
    }
}
