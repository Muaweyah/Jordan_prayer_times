package com.jo.prayertimes.tasks.data

import java.util.Locale

object DefaultCategories {
    val list = listOf(
        Category("worship", "العبادات", "Worship", "🕌", "#26A69A"),
        Category("health", "الصحة والعناية الشخصية", "Health & Self-Care", "❤️", "#EF5350"),
        Category("home", "الأعمال المنزلية", "House Chores", "🏠", "#FFA726"),
        Category("work", "العمل والإنتاجية", "Work & Productivity", "💼", "#42A5F5"),
        Category("selfdev", "التنمية الذاتية", "Self-Development", "📚", "#AB47BC"),
        Category("leisure", "الترفيه والترويح", "Entertainment & Leisure", "🎮", "#66BB6A"),
        Category("pets", "رعاية الحيوانات الأليفة", "Pet Care", "🐾", "#8D6E63"),
        Category("family", "العائلة والأقارب", "Family & Relatives", "👨‍👩‍👧‍👦", "#EC407A")
    )

    fun displayName(category: Category): String {
        val lang = Locale.getDefault().language
        return if (lang == "ar") category.nameAr else category.nameEn
    }
}
