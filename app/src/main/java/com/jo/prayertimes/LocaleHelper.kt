package com.jo.prayertimes

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** يطبّق لغة التطبيق المحفوظة (نظام/عربي/إنكليزي) على أي Context قبل عرض أي شاشة */
object LocaleHelper {

    fun wrap(context: Context): Context {
        val lang = SettingsManager(context).appLanguage
        if (lang == "system") return context

        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
