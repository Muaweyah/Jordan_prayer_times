package com.jo.prayertimes.tasks.data

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/** يحوّل جداول المهام والتصنيفات من Room إلى JSON والعكس،
 *  ليتم دمجها ضمن نفس ملف النسخة الاحتياطية العام (BackupManager) وترفع/تنزل عبر نفس آلية Drive الموجودة.
 *  الاستيراد يدمج بذكاء: أي مهمة (نفس العنوان+التاريخ+التصنيف) موجودة أصلاً محلياً يتم تجاهلها،
 *  والمهام الجديدة فقط تُضاف — لا يُحذف أو يُستبدل أي شيء محلي إطلاقاً. */
object TasksBackupBridge {

    fun exportToJson(context: Context): JSONObject = runBlocking {
        val db = TasksDatabase.getInstance(context)
        val tasks = db.taskDao().getTasksInRange("0000-00-00", "9999-99-99")

        val tasksArray = JSONArray()
        for (t in tasks) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("title", t.title)
            obj.put("category", t.category)
            obj.put("priority", t.priority)
            obj.put("date", t.date)
            obj.put("isCompleted", t.isCompleted)
            obj.put("reminderTime", t.reminderTime ?: JSONObject.NULL)
            obj.put("repeatRule", t.repeatRule ?: JSONObject.NULL)
            obj.put("customSound", t.customSound ?: JSONObject.NULL)
            tasksArray.put(obj)
        }

        val root = JSONObject()
        root.put("tasks", tasksArray)
        root
    }

    fun importFromJson(context: Context, root: JSONObject) = runBlocking {
        if (!root.has("tasks")) return@runBlocking
        val db = TasksDatabase.getInstance(context)
        val dao = db.taskDao()
        val tasksArray = root.getJSONArray("tasks")

        for (i in 0 until tasksArray.length()) {
            val obj = tasksArray.getJSONObject(i)
            val title = obj.getString("title")
            val date = obj.getString("date")
            val category = obj.getString("category")

            val existingCount = dao.countMatching(title, date, category)
            if (existingCount > 0) continue

            val task = Task(
                id = 0,
                title = title,
                category = category,
                priority = obj.optInt("priority", 0),
                date = date,
                isCompleted = obj.getBoolean("isCompleted"),
                reminderTime = obj.optString("reminderTime", null)?.takeIf { it != "null" },
                repeatRule = obj.optString("repeatRule", null)?.takeIf { it != "null" },
                customSound = obj.optString("customSound", null)?.takeIf { it != "null" }
            )
            dao.insert(task)
        }
    }
}
