package com.jo.prayertimes.tasks.data

data class DailyTemplate(val title: String, val categoryId: String, val frequency: String)

/** frequency: EVERY_DAY, WEEKLY, MONTHLY, YEARLY */
object DefaultDailyTemplates {
    val list = listOf(
        DailyTemplate("الوضوء", "worship", "EVERY_DAY"),
        DailyTemplate("الصلوات الخمس", "worship", "EVERY_DAY"),
        DailyTemplate("الأذكار", "worship", "EVERY_DAY"),
        DailyTemplate("قراءة القرآن", "worship", "EVERY_DAY"),

        DailyTemplate("شرب الماء", "health", "EVERY_DAY"),
        DailyTemplate("تنظيف الأسنان", "health", "EVERY_DAY"),
        DailyTemplate("الأكل الصحي", "health", "EVERY_DAY"),
        DailyTemplate("ممارسة الرياضة", "health", "WEEKLY"),
        DailyTemplate("الاستحمام", "health", "EVERY_DAY"),
        DailyTemplate("قص الأظافر", "health", "WEEKLY"),
        DailyTemplate("قص الشعر", "health", "MONTHLY"),

        DailyTemplate("ترتيب السرير", "home", "EVERY_DAY"),
        DailyTemplate("غسيل الملابس", "home", "EVERY_DAY"),
        DailyTemplate("جلي الأواني", "home", "EVERY_DAY"),
        DailyTemplate("تنظيف المطبخ", "home", "WEEKLY"),
        DailyTemplate("تنظيف الحمام", "home", "WEEKLY"),
        DailyTemplate("ترتيب الخزائن", "home", "MONTHLY"),
        DailyTemplate("تنظيف الكنب", "home", "MONTHLY"),

        DailyTemplate("مراجعة مهام اليوم", "work", "EVERY_DAY"),
        DailyTemplate("تقرير أسبوعي", "work", "WEEKLY"),
        DailyTemplate("تقييم الأداء الشهري", "work", "MONTHLY"),

        DailyTemplate("تعلم شيء جديد", "selfdev", "EVERY_DAY"),
        DailyTemplate("مراجعة الأهداف", "selfdev", "MONTHLY"),
        DailyTemplate("تقييم السنة", "selfdev", "YEARLY"),

        DailyTemplate("التواصل الاجتماعي", "leisure", "EVERY_DAY"),
        DailyTemplate("التلوين", "leisure", "WEEKLY"),

        DailyTemplate("تنظيف الرمل", "pets", "EVERY_DAY"),
        DailyTemplate("مواعيد الوجبات", "pets", "EVERY_DAY"),
        DailyTemplate("الطعام الخاص", "pets", "WEEKLY"),

        DailyTemplate("الاتصالات الهاتفية", "family", "EVERY_DAY"),
        DailyTemplate("الزيارات", "family", "MONTHLY"),
        DailyTemplate("المشاركة في المناسبات", "family", "MONTHLY")
    )

    fun toTask(t: DailyTemplate): Task {
        return when (t.frequency) {
            "EVERY_DAY" -> Task(
                title = t.title, category = t.categoryId, date = null,
                itemType = "DAILY", recurrenceType = "WEEKLY", recurrenceDays = "0,1,2,3,4,5,6", isRecurring = true
            )
            "WEEKLY" -> Task(
                title = t.title, category = t.categoryId, date = null,
                itemType = "DAILY", recurrenceType = "WEEKLY", recurrenceDays = "0", isRecurring = true
            )
            "MONTHLY" -> Task(
                title = t.title, category = t.categoryId, date = null,
                itemType = "DAILY", recurrenceType = "MONTHLY", monthDay = 1, isRecurring = true
            )
            else -> Task(
                title = t.title, category = t.categoryId, date = null,
                itemType = "DAILY", recurrenceType = "YEARLY", yearMonth = 1, yearDay = 1, isRecurring = true
            )
        }
    }
}
