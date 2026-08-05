package com.jo.prayertimes

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** يخزّن قائمة الحيوانات الأليفة ووجبات إطعامها بصيغة JSON داخل تفضيلات مشتركة خاصة بهذه الميزة */
class PetRepository(context: Context) {
    private val prefs = context.getSharedPreferences("pet_feeder_prefs", Context.MODE_PRIVATE)

    fun getAllPets(): MutableList<Pet> {
        val json = prefs.getString(KEY_PETS, null) ?: return mutableListOf()
        val result = mutableListOf<Pet>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val meals = mutableListOf<PetMeal>()
                val mealsArr = obj.optJSONArray("meals") ?: JSONArray()
                for (j in 0 until mealsArr.length()) {
                    val m = mealsArr.getJSONObject(j)
                    meals.add(
                        PetMeal(
                            id = m.optString("id", UUID.randomUUID().toString()),
                            hour = m.optInt("hour", 8),
                            minute = m.optInt("minute", 0),
                            enabled = m.optBoolean("enabled", true),
                            label = m.optString("label", "")
                        )
                    )
                }
                val soundUri = obj.optString("soundUri", "")
                result.add(
                    Pet(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", ""),
                        type = PetType.fromKey(obj.optString("type", "other")),
                        soundUri = soundUri.ifBlank { null },
                        meals = meals
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun getPet(petId: String): Pet? = getAllPets().find { it.id == petId }

    fun savePets(pets: List<Pet>) {
        val arr = JSONArray()
        for (pet in pets) {
            val obj = JSONObject()
            obj.put("id", pet.id)
            obj.put("name", pet.name)
            obj.put("type", pet.type.key)
            obj.put("soundUri", pet.soundUri ?: "")
            val mealsArr = JSONArray()
            for (meal in pet.meals) {
                val m = JSONObject()
                m.put("id", meal.id)
                m.put("hour", meal.hour)
                m.put("minute", meal.minute)
                m.put("enabled", meal.enabled)
                m.put("label", meal.label)
                mealsArr.put(m)
            }
            obj.put("meals", mealsArr)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_PETS, arr.toString()).apply()
    }

    /** يضيف حيواناً جديداً أو يستبدل بياناته إن كان موجوداً مسبقاً بنفس الهوية */
    fun upsertPet(pet: Pet) {
        val pets = getAllPets()
        val index = pets.indexOfFirst { it.id == pet.id }
        if (index >= 0) pets[index] = pet else pets.add(pet)
        savePets(pets)
    }

    fun deletePet(petId: String) {
        val pets = getAllPets().filterNot { it.id == petId }
        savePets(pets)
    }

    companion object {
        private const val KEY_PETS = "key_pets_json"
    }
}
