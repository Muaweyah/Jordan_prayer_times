package com.jo.prayertimes

import android.content.Context
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TasbihActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private val dhikrPhrases = arrayOf(
        "سبحان الله",
        "الحمد لله",
        "الله أكبر",
        "لا إله إلا الله",
        "أستغفر الله",
        "لا حول ولا قوة إلا بالله",
        "صلى الله عليه وسلم"
    )

    // عدّاد مستقل لكل ذكر، بحيث لا يختلط العدّ عند التنقل بين الأذكار
    private val counters = HashMap<Int, Int>()
    private var selectedIndex = 0

    private lateinit var tvCount: TextView
    private lateinit var btnTasbih: Button
    private lateinit var spDhikrChoice: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasbih)

        HomeNavigator.wire(this)
        tvCount = findViewById(R.id.tvCount)
        btnTasbih = findViewById(R.id.btnTasbih)
        val btnReset = findViewById<Button>(R.id.btnReset)
        spDhikrChoice = findViewById(R.id.spDhikrChoice)

        val adapter = ArrayAdapter(this, R.layout.spinner_item, dhikrPhrases)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spDhikrChoice.adapter = adapter

        spDhikrChoice.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedIndex = position
                btnTasbih.text = dhikrPhrases[position]
                tvCount.text = (counters[position] ?: 0).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnTasbih.setOnClickListener {
            val newCount = (counters[selectedIndex] ?: 0) + 1
            counters[selectedIndex] = newCount
            tvCount.text = newCount.toString()
        }

        btnReset.setOnClickListener {
            counters[selectedIndex] = 0
            tvCount.text = "0"
        }
    }
}
