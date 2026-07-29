package com.jo.prayertimes

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        HomeNavigator.wire(this)
        val tvAboutDetails = findViewById<TextView>(tvAboutDetailsId())
        tvAboutDetails.text = "تطبيق مواقيت الصلاة الأردنية والقرآن الكريم\n\n" +
                "• إعداد وتطوير: معاوية ظاهر الحراحشة\n" +
                "• رقم التواصل: 00962796321618\n\n" +
                "شرح التطبيق:\n" +
                "يحتوي التطبيق على مواقيت الصلاة الدقيقة لجميع محافظات المملكة الأردنية الهاشمية، وتلاوات القرآن الكريم الكاملة بصوت الشيخ مشاري العفاسي مع ميزة شريط التقدم والاستئناف من نقطة التوقف، أذكار الصباح والمساء، بوصلة القبلة، المسبحة الإلكترونية، والتقويم والمناسبات الإسلامية، مع خيارات متقدمة لإدارة الثيمات وكتم الأذان بقلب الهاتف."

        findViewById<Button>(R.id.btnCloseAbout).setOnClickListener {
            finish()
        }
    }

    private fun tvAboutDetailsId(): Int {
        return R.id.tvAboutDetails
    }
}
