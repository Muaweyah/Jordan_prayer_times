package com.jo.prayertimes.tasks.data

/** محرك المكافآت والعقاب: يحسب الخبرة/العملة/الصحة بناءً على نوع البند ودرجة الصعوبة،
 *  ويطبق قواعد المستويات الثلاثة (عادات، مهام يومية، مهام فردية) بشكل مستقل عن واجهة العرض. */
object RewardEngine {
    private fun diff(level: String): Difficulty =
        try { Difficulty.valueOf(level) } catch (e: Exception) { Difficulty.EASY }

    fun xpForLevel(level: Int): Int = level * 100

    fun applyXpAndGold(stats: UserStats, xpGain: Int, goldGain: Int): UserStats {
        var newXp = stats.xp + xpGain
        var newLevel = stats.level
        var newMaxHealth = stats.maxHealth
        while (newXp >= xpForLevel(newLevel)) {
            newXp -= xpForLevel(newLevel)
            newLevel++
            newMaxHealth += 5
        }
        return stats.copy(
            xp = newXp,
            level = newLevel,
            maxHealth = newMaxHealth,
            gold = stats.gold + goldGain
        )
    }

    fun habitPositive(stats: UserStats, difficulty: String): UserStats {
        val d = diff(difficulty)
        return applyXpAndGold(stats, (5 * d.multiplier).toInt(), (2 * d.multiplier).toInt())
    }

    fun habitNegative(stats: UserStats, difficulty: String): UserStats {
        val d = diff(difficulty)
        val damage = (4 * d.multiplier).toInt()
        return stats.copy(health = (stats.health - damage).coerceAtLeast(0))
    }

    fun dailyComplete(stats: UserStats, difficulty: String): UserStats {
        val d = diff(difficulty)
        return applyXpAndGold(stats, (8 * d.multiplier).toInt(), (3 * d.multiplier).toInt())
    }

    fun dailyMissedPunishment(stats: UserStats, difficulty: String): UserStats {
        val d = diff(difficulty)
        val damage = (6 * d.multiplier).toInt()
        return stats.copy(health = (stats.health - damage).coerceAtLeast(0))
    }

    fun todoComplete(stats: UserStats, difficulty: String, daysOpen: Int): UserStats {
        val d = diff(difficulty)
        val ageBonus = daysOpen.coerceIn(0, 30)
        val xpGain = (15 * d.multiplier).toInt() + ageBonus
        val goldGain = (8 * d.multiplier).toInt() + (ageBonus / 2)
        return applyXpAndGold(stats, xpGain, goldGain)
    }
}
