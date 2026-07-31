package com.jo.prayertimes

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var btnLocateMe: ImageButton
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvGregorianDate: TextView
    private lateinit var tvHijriDate: TextView
    private lateinit var circularCountdown: CircularCountdownView
    private lateinit var ivNextPrayerIcon: ImageView
    private lateinit var tvCountdownBig: TextView
    private lateinit var tvNextPrayerClock: TextView
    private lateinit var tvNextPrayerName: TextView

    /** لحظة حلول الصلاة القادمة (بالمللي ثانية) واسمها، تُستخدم لحساب العد التنازلي كل ثانية دون إعادة بناء القائمة */
    private var nextEventTargetMillis: Long = 0L
    /** لحظة الصلاة/الحدث السابق، تُستخدم كنقطة بداية لحساب نسبة تقدم الحلقة الدائرية */
    private var nextEventStartMillis: Long = 0L
    private var nextEventLabel: String = ""
    private var lastRefreshedMinute: Int = -1

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateClock()
            refreshHandler.postDelayed(this, 1_000)
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
        btnLocateMe = findViewById(R.id.btnLocateMe)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvGregorianDate = findViewById(R.id.tvGregorianDate)
        tvHijriDate = findViewById(R.id.tvHijriDate)
        circularCountdown = findViewById(R.id.circularCountdown)
        ivNextPrayerIcon = findViewById(R.id.ivNextPrayerIcon)
        tvCountdownBig = findViewById(R.id.tvCountdownBig)
        tvNextPrayerClock = findViewById(R.id.tvNextPrayerClock)
        tvNextPrayerName = findViewById(R.id.tvNextPrayerName)

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
        setupLocationButton()
        setupMenuButton()
        requestNotificationPermissionIfNeeded()
        requestExactAlarmPermissionIfNeeded()
        requestBatteryOptimizationExemptionIfNeeded()
        promptXiaomiAutostartIfNeeded()

        flipDetector = FlipDetector(this) { stopAdhanService() }

        AlarmScheduler(this).rescheduleAll()
    }

    /**
     * على أجهزة شاومي/ريدمي/بوكو (MIUI)، يمنع النظام التطبيق من العمل في الخلفية افتراضياً
     * ما لم يُفعَّل "بدء التشغيل التلقائي" يدوياً من إعدادات الأمان الخاصة بشاومي.
     * هذا هو السبب الأكثر شيوعاً لعدم ظهور تنبيهات الأذان على هذه الأجهزة.
     */
    private fun promptXiaomiAutostartIfNeeded() {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val isXiaomiFamily = manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")
        if (!isXiaomiFamily) return

        val prefs = getSharedPreferences("PrayerAppSettings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("xiaomi_autostart_prompt_shown", false)) return

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("إعداد ضروري لأجهزة شاومي")
            .setMessage(
                "لضمان عمل تنبيهات الأذان حتى مع إغلاق الشاشة، افتح صفحة \"بدء التشغيل التلقائي\" ثم فعّل التطبيق، " +
                "وتأكد أيضاً من ضبط توفير البطارية للتطبيق على \"بلا قيود\"."
            )
            .setPositiveButton("فتح الإعدادات") { _, _ ->
                openXiaomiAutostartSettings()
            }
            .setNegativeButton("لاحقاً", null)
            .setOnDismissListener {
                prefs.edit().putBoolean("xiaomi_autostart_prompt_shown", true).apply()
            }
            .show()
    }

    private fun openXiaomiAutostartSettings() {
        val candidateIntents = listOf(
            Intent().setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            ),
            Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT),
            Intent().setClassName("com.miui.securitycenter", "com.miui.securitycenter.Main")
        )
        for (intent in candidateIntents) {
            try {
                startActivity(intent)
                return
            } catch (e: Exception) {
                // نجرّب النية التالية في حال لم تكن هذه الشاشة متوفرة على هذا الإصدار من MIUI
            }
        }
        // كحل أخير، افتح صفحة تفاصيل التطبيق في إعدادات النظام العادية
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    private fun setupLocationButton() {
        btnLocateMe.setOnClickListener { detectNearestGovernorate() }
    }

    /** يحدد المحافظة الأقرب لموقع الجهاز الفعلي عبر GPS/الشبكة، ثم يختارها تلقائياً في القائمة المنسدلة. */
    private fun detectNearestGovernorate() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                300
            )
            return
        }

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val location: Location? = try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        } catch (e: SecurityException) {
            null
        }

        if (location == null) {
            Toast.makeText(
                this,
                "تعذّر تحديد الموقع، تأكد من تفعيل خدمة الموقع (GPS) في إعدادات الهاتف وحاول مجدداً",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val nearest = JordanGovernorates.values().minByOrNull { gov ->
            val dLat = gov.lat - location.latitude
            val dLng = gov.lng - location.longitude
            dLat * dLat + dLng * dLng
        } ?: return

        val names = JordanGovernorates.getNamesList()
        val index = names.indexOf(nearest.arabicName)
        if (index >= 0) {
            spRegion.setSelection(index)
            Toast.makeText(this, "تم تحديد المحافظة: ${nearest.arabicName}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 300 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            detectNearestGovernorate()
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

        if (nextIndex == -1) {
            // كل مواقيت اليوم انتهت، فالحدث القادم هو فجر الغد، ونقطة البداية هي عشاء اليوم
            nextIndex = 0
            val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowTimes = repository.timesFor(region, tomorrow)
            nextEventTargetMillis = timeStringToCalendar(tomorrowTimes.fajr, tomorrow).timeInMillis
            nextEventLabel = Prayer.FAJR.arabicLabel
            nextEventStartMillis = timeStringToCalendar(rawList.last().time, now).timeInMillis
        } else if (nextIndex == 0) {
            // الوقت الحالي قبل فجر اليوم، فنقطة البداية هي عشاء الأمس
            nextEventTargetMillis = timeStringToCalendar(rawList[nextIndex].time, now).timeInMillis
            nextEventLabel = rawList[nextIndex].name
            val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterdayTimes = repository.timesFor(region, yesterday)
            nextEventStartMillis = timeStringToCalendar(yesterdayTimes.isha, yesterday).timeInMillis
        } else {
            nextEventTargetMillis = timeStringToCalendar(rawList[nextIndex].time, now).timeInMillis
            nextEventLabel = rawList[nextIndex].name
            nextEventStartMillis = timeStringToCalendar(rawList[nextIndex - 1].time, now).timeInMillis
        }

        tvNextPrayerClock.text = formatClock12h(nextEventTargetMillis)
        tvNextPrayerName.text = nextEventLabel
        val isNightEvent = nextEventLabel == Prayer.FAJR.arabicLabel || nextEventLabel == Prayer.ISHA.arabicLabel
        ivNextPrayerIcon.setImageResource(if (isNightEvent) R.drawable.ic_moon else R.drawable.ic_sun)

        val finalList = rawList.mapIndexed { index, item -> item.copy(isNext = index == nextIndex) }
        adapter.updateData(finalList)

        tvHijriDate.text = HijriDateHelper.getFormattedHijriDate(this)
    }

    private fun toMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].trim().toInt() * 60 + parts[1].trim().toInt()
    }

    private fun timeStringToCalendar(time: String, dayReference: Calendar): Calendar {
        val parts = time.split(":")
        val cal = dayReference.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, parts[0].trim().toInt())
        cal.set(Calendar.MINUTE, parts[1].trim().toInt())
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    private fun updateClock() {
        val now = Calendar.getInstance()
        tvCurrentTime.text = SimpleDateFormat("EEEE، HH:mm:ss", Locale.getDefault()).format(now.time)
        tvGregorianDate.text = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(now.time)

        // نعيد بناء قائمة المواقيت فقط عند تغيّر الدقيقة، أما العد التنازلي فيُحدَّث كل ثانية بشكل مستقل وخفيف
        val currentMinute = now.get(Calendar.MINUTE)
        if (currentMinute != lastRefreshedMinute) {
            lastRefreshedMinute = currentMinute
            refreshPrayerTimes()
        }
        updateCountdown(now)
    }

    private fun updateCountdown(now: Calendar) {
        if (nextEventTargetMillis == 0L) return
        val remainingMs = (nextEventTargetMillis - now.timeInMillis).coerceAtLeast(0)
        val totalSeconds = remainingMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        tvCountdownBig.text = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

        val totalIntervalMs = (nextEventTargetMillis - nextEventStartMillis).coerceAtLeast(1L)
        val elapsedMs = (totalIntervalMs - remainingMs).coerceIn(0L, totalIntervalMs)
        circularCountdown.setProgress(elapsedMs.toFloat() / totalIntervalMs.toFloat())

        if (remainingMs == 0L) {
            // انتهى العد، أعد حساب الصلاة/الحدث القادم فوراً
            refreshPrayerTimes()
        }
    }

    /** ينسق الوقت بنظام 12 ساعة بأرقام لاتينية مع مؤشر صباحاً/مساءً بالعربية، مثل: 9:08 م */
    private fun formatClock12h(millis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        var hour = cal.get(Calendar.HOUR)
        if (hour == 0) hour = 12
        val minute = cal.get(Calendar.MINUTE)
        val period = if (cal.get(Calendar.AM_PM) == Calendar.PM) "م" else "ص"
        return String.format(Locale.US, "%d:%02d %s", hour, minute, period)
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

    /** بدون هذه الصلاحية على أندرويد 12+ قد لا يعمل تنبيه الأذان في وقته بدقة */
    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /** استثناء التطبيق من تحسين البطارية، وإلا فقد يوقف النظام الجدولة أو الخدمة قبل أن يُطلق الأذان */
    private fun requestBatteryOptimizationExemptionIfNeeded() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
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
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) && AdhanService.isPlaying) {
            stopAdhanService()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun stopAdhanService() {
        val intent = Intent(this, AdhanService::class.java).apply {
            action = AdhanService.ACTION_STOP_ADHAN
        }
        startService(intent)
    }
}
