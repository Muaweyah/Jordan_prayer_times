package com.jo.prayertimes

/** الصلوات الخمس التي لها أذان وتنبيه (لا تشمل الشروق لأنه ليس وقت صلاة) */
enum class Prayer(val key: String, val arabicLabel: String, val englishLabel: String) {
    FAJR("fajr", "الفجر", "Fajr"),
    DHUHR("dhuhr", "الظهر", "Dhuhr"),
    ASR("asr", "العصر", "Asr"),
    MAGHRIB("maghrib", "المغرب", "Maghrib"),
    ISHA("isha", "العشاء", "Isha")
}
