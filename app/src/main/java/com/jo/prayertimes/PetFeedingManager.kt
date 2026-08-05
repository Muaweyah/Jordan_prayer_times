package com.jo.prayertimes

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** يخزّن قائمة الحيوانات الأليفة ووجباتها بصيغة JSON داخل SharedPreferences،
 *  بنفس أسلوب التخزين البسيط المعتمد في بقية التطبيق دون الحاجة لقاعدة بيانات كاملة */
class PetFeedingManager(context: Context) {
    private val prefs = context.getSharedPreferences("pet_feeding", Context.MODE_PRIVATE)

    fun getPets(): MutableList<Pet> {
        val raw = prefs.getString(KEY_PETS, null) ?: return mutableListOf()
        val array = JSONArray(raw)
        val result = mutableListOf<Pet>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val meals = mutableListOf<PetMeal>()
            val mealsArray = obj.optJSONArray("meals") ?: JSONArray()
            for (j in 0 until mealsArray.length()) {
                val m = mealsArray.getJSONObject(j)
                meals.add(
                    PetMeal(
                        id = m.getInt("id"),
                        hour = m.getInt("hour"),
                        minute = m.getInt("minute"),
                        enabled = m.optBoolean("enabled", true)
                    )
                )
            }
            result.add(
                Pet(
                    id = obj.getInt("id"),
                    name = obj.getString("name"),
                    type = PetType.fromName(obj.optString("type")),
                    soundUri = if (obj.has("soundUri") && !obj.isNull("soundUri")) obj.getString("soundUri") else null,
                    meals = meals
                )
            )
        }
        return result
    }

    fun getPet(petId: Int): Pet? = getPets().find { it.id == petId }

    fun savePets(pets: List<Pet>) {
        val array = JSONArray()
        for (pet in pets) {
            val obj = JSONObject()
            obj.put("id", pet.id)
            obj.put("name", pet.name)
            obj.put("type", pet.type.name)
            obj.put("soundUri", pet.soundUri)
            val mealsArray = JSONArray()
            for (meal in pet.meals) {
                val m = JSONObject()
                m.put("id", meal.id)
                m.put("hour", meal.hour)
                m.put("minute", meal.minute)
                m.put("enabled", meal.enabled)
                mealsArray.put(m)
            }
            obj.put("meals", mealsArray)
            array.put(obj)
        }
        prefs.edit().putString(KEY_PETS, array.toString()).apply()
    }

    fun addOrUpdatePet(pet: Pet) {
        val pets = getPets()
        val index = pets.indexOfFirst { it.id == pet.id }
        if (index >= 0) pets[index] = pet else pets.add(pet)
        savePets(pets)
    }

    fun deletePet(petId: Int) {
        val pets = getPets()
        pets.removeAll { it.id == petId }
        savePets(pets)
    }

    /** عدّاد تسلسلي واحد مشترك بين معرّفات الحيوانات والوجبات لضمان تفرّدها في كل التطبيق */
    fun nextId(): Int {
        val next = prefs.getInt(KEY_ID_COUNTER, 0) + 1
        prefs.edit().putInt(KEY_ID_COUNTER, next).apply()
        return next
    }

    companion object {
        private const val KEY_PETS = "pets_json"
        private const val KEY_ID_COUNTER = "id_counter"
    }
}
