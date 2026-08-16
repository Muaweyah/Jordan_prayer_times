package com.jo.prayertimes.tasks.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

object GamificationService {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private suspend fun currentStats(context: Context): UserStats {
        val dao = TasksDatabase.getInstance(context).userStatsDao()
        return dao.get() ?: UserStats().also { dao.upsert(it) }
    }

    suspend fun applyHabit(context: Context, task: Task, positive: Boolean) {
        val dao = TasksDatabase.getInstance(context).userStatsDao()
        val stats = currentStats(context)
        val updated = if (positive) RewardEngine.habitPositive(stats, task.difficulty)
        else RewardEngine.habitNegative(stats, task.difficulty)
        dao.upsert(updated)
    }

    suspend fun applyDailyToggle(context: Context, task: Task, nowCompleted: Boolean) {
        val db = TasksDatabase.getInstance(context)
        val today = dateFormat.format(Date())
        db.dailyLogDao().upsert(DailyLog(taskId = task.id, date = today, completed = nowCompleted))
        if (nowCompleted) {
            val stats = currentStats(context)
            db.userStatsDao().upsert(RewardEngine.dailyComplete(stats, task.difficulty))
        }
    }

    suspend fun applyTodoComplete(context: Context, task: Task) {
        val dao = TasksDatabase.getInstance(context).userStatsDao()
        val stats = currentStats(context)
        val createdDays = try {
            val created = dateFormat.parse(task.createdDate ?: dateFormat.format(Date()))
            val diff = (Date().time - (created?.time ?: Date().time)) / (1000 * 60 * 60 * 24)
            diff.toInt()
        } catch (e: Exception) { 0 }
        dao.upsert(RewardEngine.todoComplete(stats, task.difficulty, createdDays))
    }

    /** يُستدعى مرة واحدة عند فتح التطبيق: يفحص أي مهمة يومية/أسبوعية/شهرية/سنوية كانت مفعّلة أمس ولم تُنجز، ويطبّق العقاب مرة واحدة فقط. */
    suspend fun rolloverCheckIfNeeded(context: Context) {
        val db = TasksDatabase.getInstance(context)
        val statsDao = db.userStatsDao()
        var stats = currentStats(context)
        val today = dateFormat.format(Date())
        if (stats.lastRolloverDate == today) return

        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterday = dateFormat.format(yesterdayCal.time)

        val dailies = db.taskDao().getTasksInRange("0000-00-00", "9999-99-99")
            .filter { it.itemType == "DAILY" }

        for (task in dailies) {
            if (!RecurrenceUtils.isActiveOn(task, yesterdayCal)) continue

            val log = db.dailyLogDao().get(task.id, yesterday)
            if (log?.completed == true) continue
            if (log?.punished == true) continue

            stats = RewardEngine.dailyMissedPunishment(stats, task.difficulty)
            db.dailyLogDao().upsert(DailyLog(taskId = task.id, date = yesterday, completed = false, punished = true))
        }

        statsDao.upsert(stats.copy(lastRolloverDate = today))
    }

    suspend fun seedDefaultDailiesIfNeeded(context: Context) {
        val db = TasksDatabase.getInstance(context)
        val statsDao = db.userStatsDao()
        val stats = currentStats(context)
        if (stats.defaultDailiesSeeded) return

        for (template in DefaultDailyTemplates.list) {
            db.taskDao().insert(DefaultDailyTemplates.toTask(template))
        }
        statsDao.upsert(stats.copy(defaultDailiesSeeded = true))
    }
}
