package com.jo.prayertimes

enum class JordanGovernorates(val arabicName: String, val lat: Double, val lng: Double) {
    AMMAN("عَمّان", 31.9539, 35.9106),
    IRBID("إربد", 32.5556, 35.8500),
    ZARQA("الزرقاء", 32.0728, 36.0880),
    BALQA("البلقاء", 32.0392, 35.7272),
    AQABA("العقبة", 29.5321, 35.0063),
    MAAN("معان", 30.1949, 35.7342),
    KARAK("الكرك", 31.1853, 35.7048),
    TAFILAH("الطفيلة", 30.8374, 35.6181),
    MADABA("مأدبا", 31.7157, 35.7939),
    JERASH("جرش", 32.2811, 35.8992),
    AJLOUN("عجلون", 32.3326, 35.7517),
    MAFRAQ("المفرق", 32.3433, 36.2081);

    companion object {
        fun getNamesList(): List<String> = values().map { it.arabicName }
    }
}
