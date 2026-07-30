package com.jo.prayertimes

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OccasionsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_occasions)

        HomeNavigator.wire(this)
        val listView = findViewById<ListView>(R.id.lvOccasions)
        val occasions = OccasionsCalculator.getUpcomingOccasions(this)

        listView.adapter = OccasionCardAdapter(this, occasions)
        listView.divider = null
        listView.dividerHeight = 0
    }

    private class OccasionCardAdapter(
        context: Context,
        items: List<ComputedOccasion>
    ) : ArrayAdapter<ComputedOccasion>(context, R.layout.item_occasion_card, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_occasion_card, parent, false)

            val entry = getItem(position)
            view.findViewById<TextView>(R.id.occasionTitle).text = entry?.title ?: ""
            view.findViewById<TextView>(R.id.occasionDate).text = entry?.dateText ?: ""
            return view
        }
    }
}
