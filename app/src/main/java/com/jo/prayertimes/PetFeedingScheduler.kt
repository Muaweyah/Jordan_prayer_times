package com.jo.prayertimes

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/** يجدول تنبيهات إطعام الحيوانات الأليفة بنفس منطق جدولة الأذان: يحسب أقرب موعد قادم
 *  (اليوم أو غداً) لكل وجبة مفعّلة، ويعيد الجدولة تلقائياً بعد كل تنبيه لضمان استمرارها يومياً */
class PetFeedingScheduler(private val context: Context) {
    private val manager = PetFeedingManager(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun rescheduleAll() {
        val pets = manager.getPets()
        val now = Calendar.getInstance()
        for (pet in pets) {
            for (meal in pet.meals) {
                if (!meal.enabled) {
                    cancelMeal(meal.id)
                    continue
                }
                val trigger = (now.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, meal.hour)
                    set(Calendar.MINUTE, meal.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (trigger.timeInMillis <= System.currentTimeMillis()) {
                    trigger.add(Calendar.DAY_OF_YEAR, 1)
                }
                scheduleMeal(pet.id, meal.id, trigger)
            }
        }
    }

    fun cancelMeal(mealId: Int) {
        alarmManager.cancel(pendingIntentFor(mealId))
    }

    private fun scheduleMeal(petId: Int, mealId: Int, trigger: Calendar) {
        val pendingIntent = pendingIntentFor(mealId, petId)
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

    private fun pendingIntentFor(mealId: Int, petId: Int = -1): PendingIntent {
        val intent = Intent(context, PetFeedingReceiver::class.java).apply {
            action = ACTION_FEED_PET
            putExtra(EXTRA_MEAL_ID, mealId)
            if (petId != -1) putExtra(EXTRA_PET_ID, petId)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + mealId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_FEED_PET = "com.jo.prayertimes.ACTION_FEED_PET"
        const val EXTRA_PET_ID = "extra_pet_id"
        const val EXTRA_MEAL_ID = "extra_meal_id"
        private const val REQUEST_CODE_BASE = 20000
    }
}
