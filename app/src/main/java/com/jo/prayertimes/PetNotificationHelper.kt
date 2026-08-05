package com.jo.prayertimes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build

/** يبني قناة إشعار خاصة بكل حيوان أليف على حدة، لتحمل نغمة التنبيه المخصّصة له دون التأثير على حيوانات أخرى */
object PetNotificationHelper {

    /** رقم القناة يتضمّن هوية الحيوان ونغمته، فإذا غيّر المستخدم النغمة تُبنى قناة جديدة تلقائياً بدل تعديل قناة موجودة
     *  (قنوات الإشعارات في أندرويد لا يمكن تغيير نغمتها بعد إنشائها) */
    fun channelIdFor(pet: Pet): String = "pet_channel_${pet.id}_${(pet.soundUri ?: "default").hashCode()}"

    fun ensureChannel(context: Context, pet: Pet) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channelId = channelIdFor(pet)
        if (manager.getNotificationChannel(channelId) != null) return

        // نحذف قنوات هذا الحيوان القديمة (بنغمتها السابقة) لتجنّب تراكم قنوات لا فائدة منها بعد كل تغيير نغمة
        manager.notificationChannels
            .filter { it.id.startsWith("pet_channel_${pet.id}_") && it.id != channelId }
            .forEach { manager.deleteNotificationChannel(it.id) }

        val channel = NotificationChannel(
            channelId,
            "تنبيهات إطعام: ${pet.name}",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            val soundUri = pet.soundUri?.let { Uri.parse(it) }
            if (soundUri != null) {
                setSound(
                    soundUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
        }
        manager.createNotificationChannel(channel)
    }

    /** يحذف كل قنوات حيوان مُحذوف بالكامل من التطبيق */
    fun deleteAllChannelsForPet(context: Context, petId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notificationChannels
            .filter { it.id.startsWith("pet_channel_${petId}_") }
            .forEach { manager.deleteNotificationChannel(it.id) }
    }
}
