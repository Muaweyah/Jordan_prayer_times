package com.jo.prayertimes

import android.app.Activity
import android.content.Intent
import android.widget.ImageButton

/** يربط زر المنزل العائم (إن وُجد بالتخطيط الحالي) بالعودة إلى الشاشة الرئيسية من أي شاشة فرعية */
object HomeNavigator {
    fun wire(activity: Activity) {
        activity.findViewById<ImageButton>(R.id.btnHome)?.setOnClickListener {
            val intent = Intent(activity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            activity.startActivity(intent)
            activity.finish()
        }
    }
}
