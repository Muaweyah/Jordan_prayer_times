package com.jo.prayertimes

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/** يستقبل تنبيه وقت إطعام حيوان أليف، فيعرض إشعاراً لطيفاً بنغمة الحيوان الخاصة، ثم يعيد جدولة كل وجبات الإطعام القادمة */
class PetAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PetAlarmScheduler.ACTION_FEED_PET) {
            val petId = intent.getStringExtra(PetAlarmScheduler.EXTRA_PET_ID)
            val mealId = intent.getStringExtra(PetAlarmScheduler.EXTRA_MEAL_ID)
            val pet = petId?.let { PetRepository(context).getPet(it) }
            val meal = pet?.meals?.find { it.id == mealId }
            if (pet != null && meal != null) {
                showFeedingNotification(context, pet, meal)
            }
        }

        // في كل مرة يعمل فيها المستقبِل (بعد تنبيه إطعام أو بعد إعادة الإقلاع) نعيد جدولة كل وجبات الإطعام القادمة
        PetAlarmScheduler(context).rescheduleAll()
    }

    private fun showFeedingNotification(context: Context, pet: Pet, meal: PetMeal) {
        PetNotificationHelper.ensureChannel(context, pet)
        val channelId = PetNotificationHelper.channelIdFor(pet)

        val openIntent = Intent(context, PetFeederActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            PetAlarmScheduler.requestCodeFor(pet.id, meal.id),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mealLabel = meal.label.trim()
        val contentText = if (mealLabel.isNotEmpty()) {
            "حان وقت وجبة \"$mealLabel\" لـ ${pet.name}"
        } else {
            "حان وقت إطعام ${pet.name}"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("${pet.type.emoji} أطعم أليفك")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(PetAlarmScheduler.notificationIdFor(pet.id, meal.id), notification)
    }
}
