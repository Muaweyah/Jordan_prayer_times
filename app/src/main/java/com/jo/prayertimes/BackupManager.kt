package com.jo.prayertimes

import android.content.Context
import android.content.SharedPreferences
import com.jo.prayertimes.tasks.data.TasksBackupBridge
import org.json.JSONArray
import org.json.JSONObject

/** يبني نسخة JSON كاملة من كل تفضيلات التطبيق (الإعدادات + بيانات الحيوانات الأليفة + المهام اليومية) ويستعيدها.
 *  يعمل بشكل عام على أي مفتاح موجود حالياً، دون الحاجة لتحديثه يدوياً كل ما تُضاف ميزة جديدة
 *  تستخدم SharedPreferences، بالإضافة لجدول المهام بقاعدة بيانات Room المنفصلة. */
class BackupManager(private val context: Context) {
    private val prefFiles = listOf("app_settings", "pet_feeding")

    fun buildBackupJson(): String {
        val root = JSONObject()
        for (name in prefFiles) {
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            root.put(name, serializePrefs(prefs))
        }
        val tasksJson = TasksBackupBridge.exportToJson(context)
        root.put("daily_tasks", tasksJson)
        return root.toString()
    }

    fun restoreFromJson(json: String) {
        val root = JSONObject(json)
        for (name in prefFiles) {
            if (!root.has(name)) continue
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            deserializeInto(prefs, root.getJSONObject(name))
        }
        if (root.has("daily_tasks")) {
            TasksBackupBridge.importFromJson(context, root.getJSONObject("daily_tasks"))
        }
    }

    private fun serializePrefs(prefs: SharedPreferences): JSONObject {
        val obj = JSONObject()
        for ((key, value) in prefs.all) {
            val entry = JSONObject()
            when (value) {
                is Boolean -> {
                    entry.put("t", "bool"); entry.put("v", value)
                }
                is Int -> {
                    entry.put("t", "int"); entry.put("v", value)
                }
                is Long -> {
                    entry.put("t", "long"); entry.put("v", value)
                }
                is Float -> {
                    entry.put("t", "float"); entry.put("v", value.toDouble())
                }
                is String -> {
                    entry.put("t", "string"); entry.put("v", value)
                }
                is Set<*> -> {
                    entry.put("t", "stringSet")
                    val arr = JSONArray()
                    for (item in value) arr.put(item.toString())
                    entry.put("v", arr)
                }
                else -> continue
            }
            obj.put(key, entry)
        }
        return obj
    }

    private fun deserializeInto(prefs: SharedPreferences, obj: JSONObject) {
        val editor = prefs.edit()
        editor.clear()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val entry = obj.getJSONObject(key)
            when (entry.getString("t")) {
                "bool" -> editor.putBoolean(key, entry.getBoolean("v"))
                "int" -> editor.putInt(key, entry.getInt("v"))
                "long" -> editor.putLong(key, entry.getLong("v"))
                "float" -> editor.putFloat(key, entry.getDouble("v").toFloat())
                "string" -> editor.putString(key, entry.getString("v"))
                "stringSet" -> {
                    val arr = entry.getJSONArray("v")
                    val set = mutableSetOf<String>()
                    for (i in 0 until arr.length()) set.add(arr.getString(i))
                    editor.putStringSet(key, set)
                }
            }
        }
        editor.apply()
    }
}
