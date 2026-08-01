package com.jo.prayertimes

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

/** مصدر واحد موحّد لكل إعدادات التطبيق المخزّنة، تقرأ منه كل الشاشات والخدمات */
class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    /** هل تنبيه/أذان صلاة معيّنة مفعّل؟ (يُستخدم لجدولة المنبّهات ولزر الجرس في الواجهة الرئيسية) */
    fun isNotificationEnabled(prayer: Prayer): Boolean =
        prefs.getBoolean("key_adhan_${prayer.key}", true)

    fun setNotificationEnabled(prayer: Prayer, enabled: Boolean) {
        prefs.edit().putBoolean("key_adhan_${prayer.key}", enabled).apply()
    }

    /** المحافظة المختارة لحساب المواقيت */
    var selectedRegion: String?
        get() = prefs.getString("key_governorate", JordanGovernorates.AMMAN.arabicName)
        set(value) { prefs.edit().putString("key_governorate", value).apply() }

    /** تصحيح التاريخ الهجري بالأيام (يستخدمه HijriDateHelper وشاشة الإعدادات) */
    var hijriOffsetDays: Int
        get() = prefs.getInt("key_hijri_offset", 0)
        set(value) { prefs.edit().putInt("key_hijri_offset", value).apply() }

    /** الثيم: "system" أو "light" أو "dark" أو "purple" */
    var theme: String
        get() = prefs.getString("key_theme", "system") ?: "system"
        set(value) { prefs.edit().putString("key_theme", value).apply() }

    fun applyTheme() {
        val mode = when (theme) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark", "purple" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /** لغة التطبيق: "system" أو "ar" أو "en" */
    var appLanguage: String
        get() = prefs.getString("key_app_language", "system") ?: "system"
        set(value) { prefs.edit().putString("key_app_language", value).apply() }

    /** نظام عرض الوقت: "24" (افتراضي) أو "12" (مع مؤشر صباحاً/مساءً) */
    var timeFormat: String
        get() = prefs.getString("key_time_format", "24") ?: "24"
        set(value) { prefs.edit().putString("key_time_format", value).apply() }
}
