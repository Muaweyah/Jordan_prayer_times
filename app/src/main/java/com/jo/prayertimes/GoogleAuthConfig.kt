package com.jo.prayertimes

/** معرّف عميل الويب (Web Client ID) من Google Cloud Console.
 *  هذا معرّف عام وليس سرياً، ويُستخدم لطلب رمز هوية (ID token) موثّق من جوجل عند تسجيل الدخول.
 *  لا تضع هنا الـ Client Secret أبداً — تطبيقات الموبايل لا تحتاجه. */
object GoogleAuthConfig {
    const val WEB_CLIENT_ID =
        "693769670623-7e750iuq0h4ks39n9481qkq5db2nrca7.apps.googleusercontent.com"
}
