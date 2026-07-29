package com.jo.prayertimes

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var flipDetector: FlipDetector
    private lateinit var settings: SettingsManager
    private lateinit var repository: PrayerRepository
    private lateinit var adapter: PrayerAdapter

    private lateinit var spRegion: Spinner
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvGregorianDate: TextView
    private lateinit var tvHijriDate: TextView

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateClock()
            refreshHandler.postDelayed(this, 30_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        settings = SettingsManager(this)
        settings.applyTheme()
        if (settings.theme == "purple") {
            setTheme(R.style.Theme_PrayerTimes_Purple)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = PrayerRepository(this)

        spRegion = findViewById(R.id.spRegion)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvGregorianDate = findViewById(R.id.tvGregorianDate)
        tvHijriDate = findViewById(R.id.tvHijriDate)

        val rvPrayerTimes = findViewById<RecyclerView>(R.id.rvPrayerTimes)
        rvPrayerTimes.layoutManager = LinearLayoutManager(this)
        adapter = PrayerAdapter(emptyList(), settings) { _, _ ->
            AlarmScheduler(this).rescheduleAll()
        }
        rvPrayerTimes.adapter = adapter

        rvPrayerTimes.post {
            val rowCount = 6 // الفجر، الشروق، الظهر، العصر، المغرب، العشاء
            val marginPerRowPx = (8 * resources.displayMetrics.density).toInt() // 4dp أعلى + 4dp أسفل لكل بطاقة
            val minItemHeightPx = (56 * resources.displayMetrics.density).toInt() // أقل ارتفاع يضمن عدم قص النص
            val availableHeight = rvPrayerTimes.height
            if (availableHeight > 0) {
                val computedHeight = (availableHeight - marginPerRowPx * rowCount) / rowCount
                adapter.setItemHeight(maxOf(computedHeight, minItemHeightPx))
            }
        }

        setupRegionSpinner()
        setupMenuButton()
        requestNotificationPermissionIfNeeded()

        flipDetector = FlipDetector(this) { stopAdhanService() }

        AlarmScheduler(this).rescheduleAll()
    }

    private fun setupRegionSpinner() {
        val names = JordanGovernorates.getNamesList()
        val adapterSpinner = ArrayAdapter(this, R.layout.spinner_item, names)
        adapterSpinner.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spRegion.adapter = adapterSpinner

        val savedRegion = settings.selectedRegion ?: JordanGovernorates.AMMAN.arabicName
        val savedIndex = names.indexOf(savedRegion).takeIf { it >= 0 } ?: 0
        spRegion.setSelection(savedIndex, false)

        spRegion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val chosen = names[position]
                if (chosen != settings.selectedRegion) {
                    settings.selectedRegion = chosen
                    AlarmScheduler(this@MainActivity).rescheduleAll()
                }
                refreshPrayerTimes()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupMenuButton() {
        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)
        btnMenu.setOnClickListener { anchor ->
            val popup = PopupMenu(this, anchor)
            popup.menu.add(0, 1, 0, getString(R.string.qibla_title))
            popup.menu.add(0, 2, 1, getString(R.string.occasions_title))
            popup.menu.add(0, 3, 2, getString(R.string.quran_title))
            popup.menu.add(0, 4, 3, getString(R.string.azkar_title))
            popup.menu.add(0, 5, 4, getString(R.string.tasbih_title))
            popup.menu.add(0, 6, 5, getString(R.string.settings_title))
            popup.menu.add(0, 7, 6, getString(R.string.about_title))
            popup.setOnMenuItemClickListener { item ->
                val target = when (item.itemId) {
                    1 -> QiblaActivity::class.java
                    2 -> OccasionsActivity::class.java
                    3 -> QuranActivity::class.java
                    4 -> AzkarActivity::class.java
                    5 -> TasbihActivity::class.java
                    6 -> SettingsActivity::class.java
                    7 -> AboutActivity::class.java
                    else -> null
                }
                target?.let { startActivity(Intent(this, it)) }
                true
            }
            popup.show()
        }
    }

    private fun refreshPrayerTimes() {
        val region = settings.selectedRegion ?: JordanGovernorates.AMMAN.arabicName
        val now = Calendar.getInstance()
        val times = repository.timesFor(region, now)

        val rawList = listOf(
            PrayerItem(Prayer.FAJR.arabicLabel, times.fajr, Prayer.FAJR),
            PrayerItem("الشروق", times.sunrise, null),
            PrayerItem(Prayer.DHUHR.arabicLabel, times.dhuhr, Prayer.DHUHR),
            PrayerItem(Prayer.ASR.arabicLabel, times.asr, Prayer.ASR),
            PrayerItem(Prayer.MAGHRIB.arabicLabel, times.maghrib, Prayer.MAGHRIB),
            PrayerItem(Prayer.ISHA.arabicLabel, times.isha, Prayer.ISHA)
        )

        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        var nextIndex = rawList.indexOfFirst { toMinutes(it.time) > nowMinutes }
        if (nextIndex == -1) nextIndex = 0

        val finalList = rawList.mapIndexed { index, item -> item.copy(isNext = index == nextIndex) }
        adapter.updateData(finalList)

        tvHijriDate.text = HijriDateHelper.getFormattedHijriDate(this)
    }

    private fun toMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].trim().toInt() * 60 + parts[1].trim().toInt()
    }

    private fun updateClock() {
        val now = Calendar.getInstance()
        tvCurrentTime.text = SimpleDateFormat("EEEE، HH:mm", Locale.getDefault()).format(now.time)
        tvGregorianDate.text = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(now.time)
        refreshPrayerTimes()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        flipDetector.start()
        refreshHandler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        flipDetector.stop()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            stopAdhanService()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun stopAdhanService() {
        val intent = Intent(this, AdhanService::class.java).apply {
            action = "ACTION_STOP_ADHAN"
        }
        startService(intent)
    }
}
