package com.jo.prayertimes

/** وجبة إطعام واحدة ضمن جدول حيوان أليف؛ id فريد على مستوى التطبيق كله ليُستخدم كرمز طلب المنبّه */
data class PetMeal(
    val id: Int,
    var hour: Int,
    var minute: Int,
    var enabled: Boolean = true
)

data class Pet(
    val id: Int,
    var name: String,
    var type: PetType,
    var soundUri: String? = null,
    var meals: MutableList<PetMeal> = mutableListOf()
)
