package com.jo.prayertimes

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/** يجدول تنبيهات إطعام الحيوانات الأليفة: تنبيه مستقل لكل وجبة مفعّلة عند أقرب وقت قادم لها (اليوم أو غداً) */
class PetAlarmScheduler(private val context: Context) {
    private val repository = PetRepository(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** يعيد جدولة جميع تنبيهات الإطعام بحسب البيانات المحفوظة حالياً؛ يُستدعى بعد كل حفظ للحيوانات وبعد كل إقلاع وبعد كل تنبيه */
    fun rescheduleAll() {
        for (pet in repository.getAllPets()) {
            for (meal in pet.meals) {
                if (meal.enabled) scheduleMeal(pet, meal) else cancelMeal(pet.id, meal.id)
            }
        }
    }

    /** يُلغي كل تنبيهات وجبات حيوان معيّن، يُستخدم عند حذف الحيوان أو قبل حفظ تعديلات وجباته */
    fun cancelForPet(pet: Pet) {
        for (meal in pet.meals) cancelMeal(pet.id, meal.id)
    }

    private fun scheduleMeal(pet: Pet, meal: PetMeal) {
        val trigger = nextTriggerFor(meal.hour, meal.minute)
        val pendingIntent = pendingIntentFor(pet.id, meal.id)
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

    private fun cancelMeal(petId: String, mealId: String) {
        alarmManager.cancel(pendingIntentFor(petId, mealId))
    }

    private fun pendingIntentFor(petId: String, mealId: String): PendingIntent {
        val intent = Intent(context, PetAlarmReceiver::class.java).apply {
            action = ACTION_FEED_PET
            putExtra(EXTRA_PET_ID, petId)
            putExtra(EXTRA_MEAL_ID, mealId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(petId, mealId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerFor(hour: Int, minute: Int): Calendar {
        val now = Calendar.getInstance()
        val trigger = now.clone() as Calendar
        trigger.set(Calendar.HOUR_OF_DAY, hour)
        trigger.set(Calendar.MINUTE, minute)
        trigger.set(Calendar.SECOND, 0)
        trigger.set(Calendar.MILLISECOND, 0)
        if (trigger.timeInMillis <= now.timeInMillis) {
            trigger.add(Calendar.DAY_OF_YEAR, 1)
        }
        return trigger
    }

    companion object {
        const val ACTION_FEED_PET = "com.jo.prayertimes.ACTION_FEED_PET"
        const val EXTRA_PET_ID = "extra_pet_id"
        const val EXTRA_MEAL_ID = "extra_meal_id"

        /** رمز طلب فريد وثابت مشتق من هوية الحيوان والوجبة معاً، يضمن إلغاء التنبيه الصحيح عند التعديل أو الحذف */
        fun requestCodeFor(petId: String, mealId: String): Int =
            (petId + "_" + mealId).hashCode() and 0x7FFFFFFF

        /** رقم إشعار فريد لكل وجبة، بمعزل عن إشعارات الأذان (501) وبقية إشعارات التطبيق */
        fun notificationIdFor(petId: String, mealId: String): Int =
            2_000_000 + (requestCodeFor(petId, mealId) % 900_000)
    }
}
