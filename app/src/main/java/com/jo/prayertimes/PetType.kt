package com.jo.prayertimes

enum class PetType(val arabicLabel: String) {
    CAT("قطة"),
    DOG("كلب"),
    BIRD("طائر"),
    FISH("سمك"),
    OTHER("حيوان أليف آخر");

    companion object {
        fun fromName(name: String?): PetType =
            values().find { it.name == name } ?: OTHER
    }
}
