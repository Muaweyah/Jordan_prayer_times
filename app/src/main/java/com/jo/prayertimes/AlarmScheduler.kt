package com.jo.prayertimes

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val repository = PrayerRepository(context)
    private val settings = SettingsManager(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNextAlarm(region: String) {
        val now = Calendar.getInstance()
        val todayTimes = repository.timesFor(region, now)

        val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowTimes = repository.timesFor(region, tomorrow)

        val todayMap = mapOf(
            Prayer.FAJR to todayTimes.fajr,
            Prayer.DHUHR to todayTimes.dhuhr,
            Prayer.ASR to todayTimes.asr,
            Prayer.MAGHRIB to todayTimes.maghrib,
            Prayer.ISHA to todayTimes.isha
        )
        val tomorrowMap = mapOf(
            Prayer.FAJR to tomorrowTimes.fajr,
            Prayer.DHUHR to tomorrowTimes.dhuhr,
            Prayer.ASR to tomorrowTimes.asr,
            Prayer.MAGHRIB to tomorrowTimes.maghrib,
            Prayer.ISHA to tomorrowTimes.isha
        )

        for (prayer in Prayer.values()) {
            if (!settings.isNotificationEnabled(prayer)) {
                cancelAlarm(prayer)
                continue
            }

            var trigger = timeStringToCalendar(todayMap.getValue(prayer), now)
            if (trigger.timeInMillis <= System.currentTimeMillis()) {
                trigger = timeStringToCalendar(tomorrowMap.getValue(prayer), tomorrow)
            }
            scheduleAlarm(prayer, trigger)
        }
    }

    fun rescheduleAll() {
        val region = settings.selectedRegion ?: JordanGovernorates.AMMAN.arabicName
        scheduleNextAlarm(region)
    }

    private fun timeStringToCalendar(time: String, dayReference: Calendar): Calendar {
        val parts = time.split(":")
        val cal = dayReference.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, parts[0].trim().toInt())
        cal.set(Calendar.MINUTE, parts[1].trim().toInt())
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    private fun pendingIntentFor(prayer: Prayer): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_PLAY_ADHAN
            putExtra(EXTRA_PRAYER_KEY, prayer.key)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(prayer),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleAlarm(prayer: Prayer, trigger: Calendar) {
        val pendingIntent = pendingIntentFor(prayer)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.timeInMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.timeInMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(prayer: Prayer) {
        alarmManager.cancel(pendingIntentFor(prayer))
    }

    private fun requestCodeFor(prayer: Prayer): Int = 1000 + prayer.ordinal

    companion object {
        const val ACTION_PLAY_ADHAN = "com.jo.prayertimes.ACTION_PLAY_ADHAN"
        const val EXTRA_PRAYER_KEY = "extra_prayer_key"
    }
}
