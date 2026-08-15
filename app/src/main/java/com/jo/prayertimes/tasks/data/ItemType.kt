package com.jo.prayertimes.tasks.data

enum class ItemType { HABIT, DAILY, TODO }
enum class Difficulty(val multiplier: Float, val label: String) {
    EASY(1f, "سهلة"),
    MEDIUM(1.5f, "متوسطة"),
    HARD(2f, "صعبة")
}
