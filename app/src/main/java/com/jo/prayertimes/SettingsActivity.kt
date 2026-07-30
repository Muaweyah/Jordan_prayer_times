package com.jo.prayertimes

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var swFlipToMute: Switch
    private lateinit var swVolumeButtonMute: Switch
    private lateinit var spinnerTheme: Spinner
    private lateinit var spinnerLanguage: Spinner
    private lateinit var btnSaveSettings: Button

    private lateinit var swFajr: Switch
    private lateinit var swDhuhr: Switch
    private lateinit var swAsr: Switch
    private lateinit var swMaghrib: Switch
    private lateinit var swIsha: Switch

    private lateinit var btnHijriMinus: Button
    private lateinit var btnHijriPlus: Button
    private lateinit var tvHijriOffset: TextView
    private var hijriOffset = 0

    private lateinit var settings: SettingsManager

    // ترتيب القيم يطابق ترتيب الخيارات المعروضة في قائمة اللغة المنسدلة
    private val languageValues = arrayOf("system", "ar", "en")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        HomeNavigator.wire(this)
        settings = SettingsManager(this)

        swFlipToMute = findViewById(R.id.swFlipToMute)
        swVolumeButtonMute = findViewById(R.id.swVolumeButtonMute)
        spinnerTheme = findViewById(R.id.spinnerTheme)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)

        swFajr = findViewById(R.id.swFajr)
        swDhuhr = findViewById(R.id.swDhuhr)
        swAsr = findViewById(R.id.swAsr)
        swMaghrib = findViewById(R.id.swMaghrib)
        swIsha = findViewById(R.id.swIsha)

        btnHijriMinus = findViewById(R.id.btnHijriMinus)
        btnHijriPlus = findViewById(R.id.btnHijriPlus)
        tvHijriOffset = findViewById(R.id.tvHijriOffset)

        val prefs = getSharedPreferences("PrayerAppSettings", Context.MODE_PRIVATE)
        swFlipToMute.isChecked = prefs.getBoolean("flip_to_mute", true)
        swVolumeButtonMute.isChecked = prefs.getBoolean("volume_button_mute", true)

        swFajr.isChecked = settings.isNotificationEnabled(Prayer.FAJR)
        swDhuhr.isChecked = settings.isNotificationEnabled(Prayer.DHUHR)
        swAsr.isChecked = settings.isNotificationEnabled(Prayer.ASR)
        swMaghrib.isChecked = settings.isNotificationEnabled(Prayer.MAGHRIB)
        swIsha.isChecked = settings.isNotificationEnabled(Prayer.ISHA)

        hijriOffset = settings.hijriOffsetDays
        updateHijriOffsetLabel()
        btnHijriMinus.setOnClickListener {
            if (hijriOffset > -3) hijriOffset--
            updateHijriOffsetLabel()
        }
        btnHijriPlus.setOnClickListener {
            if (hijriOffset < 3) hijriOffset++
            updateHijriOffsetLabel()
        }

        val themes = arrayOf(getString(R.string.theme_system), getString(R.string.theme_light), getString(R.string.theme_dark), getString(R.string.theme_purple))
        val themeAdapter = ArrayAdapter(this, R.layout.spinner_item, themes)
        themeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerTheme.adapter = themeAdapter

        // نقرأ الثيم الحالي من نفس المصدر الذي تعتمده MainActivity عند بدء التطبيق
        val currentThemeIndex = when (settings.theme) {
            "light" -> 1
            "dark" -> 2
            "purple" -> 3
            else -> 0
        }
        spinnerTheme.setSelection(currentThemeIndex)

        val languages = arrayOf(getString(R.string.lang_system), getString(R.string.lang_arabic), getString(R.string.lang_english))
        val languageAdapter = ArrayAdapter(this, R.layout.spinner_item, languages)
        languageAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerLanguage.adapter = languageAdapter
        spinnerLanguage.setSelection(languageValues.indexOf(settings.appLanguage).takeIf { it >= 0 } ?: 0)

        btnSaveSettings.setOnClickListener {
            val selectedTheme = spinnerTheme.selectedItemPosition
            val selectedLanguage = languageValues[spinnerLanguage.selectedItemPosition]
            val languageChanged = selectedLanguage != settings.appLanguage

            prefs.edit()
                .putBoolean("flip_to_mute", swFlipToMute.isChecked)
                .putBoolean("volume_button_mute", swVolumeButtonMute.isChecked)
                .apply()

            settings.setNotificationEnabled(Prayer.FAJR, swFajr.isChecked)
            settings.setNotificationEnabled(Prayer.DHUHR, swDhuhr.isChecked)
            settings.setNotificationEnabled(Prayer.ASR, swAsr.isChecked)
            settings.setNotificationEnabled(Prayer.MAGHRIB, swMaghrib.isChecked)
            settings.setNotificationEnabled(Prayer.ISHA, swIsha.isChecked)
            settings.hijriOffsetDays = hijriOffset
            settings.appLanguage = selectedLanguage

            // نُطبّق فوراً أي تغيير في تفعيل/تعطيل التنبيهات على التنبيهات المجدولة
            AlarmScheduler(this).rescheduleAll()

            // نخزّن الثيم عبر SettingsManager نفسه الذي تقرأ منه MainActivity في كل مرة يُفتح فيها التطبيق
            val newTheme = when (selectedTheme) {
                1 -> "light"
                2 -> "dark"
                3 -> "purple"
                else -> "system"
            }
            val themeChanged = newTheme != settings.theme
            settings.theme = newTheme
            settings.applyTheme()

            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()

            if (languageChanged || themeChanged) {
                // إعادة تشغيل الشاشة الرئيسية فوراً لتطبيق الثيم أو اللغة الجديدة على كامل التطبيق
                val intent = android.content.Intent(this, MainActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
            finish()
        }
    }

    private fun updateHijriOffsetLabel() {
        tvHijriOffset.text = when {
            hijriOffset == 0 -> getString(R.string.no_change_label)
            hijriOffset > 0 -> "+$hijriOffset"
            else -> "$hijriOffset"
        }
    }
}
