package com.jo.prayertimes

import android.content.Context
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class MushafWordLoc(val surah: Int, val ayah: Int)
data class MushafLine(val type: String, val text: String)
data class MushafPage(val pageNumber: Int, val lines: List<MushafLine>, val ayahOrder: List<MushafWordLoc>)

class QuranActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private lateinit var spSurahList: Spinner
    private lateinit var btnPlayPause: Button
    private lateinit var btnRewind10: Button
    private lateinit var btnForward10: Button
    private lateinit var tvPageNumber: TextView
    private lateinit var llMushafPageLines: LinearLayout
    private lateinit var progressQuranPage: ProgressBar
    private lateinit var btnToggleFullscreen: ImageButton
    private lateinit var tvQuranTitle: TextView
    private lateinit var llQuranControls: LinearLayout
    private lateinit var llIndexBuilding: LinearLayout
    private lateinit var tvBuildingIndexStatus: TextView
    private lateinit var progressBuildIndex: ProgressBar
    private lateinit var llQuranRoot: LinearLayout
    private lateinit var btnHome: ImageButton
    private var isFullscreen = false

    private var mediaPlayer: MediaPlayer? = null
    private var currentPage: MushafPage? = null
    private var playQueue: MutableList<MushafWordLoc> = mutableListOf()
    private var queueIndex = 0
    private var isAutoPlaying = false
    private var surahStartPage = IntArray(115)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quran)

        HomeNavigator.wire(this)
        spSurahList = findViewById(R.id.spSurahList)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnRewind10 = findViewById(R.id.btnRewind10)
        btnForward10 = findViewById(R.id.btnForward10)
        tvPageNumber = findViewById(R.id.tvPageNumber)
        llMushafPageLines = findViewById(R.id.llMushafPageLines)
        progressQuranPage = findViewById(R.id.progressQuranPage)
        btnToggleFullscreen = findViewById(R.id.btnToggleFullscreen)
        tvQuranTitle = findViewById(R.id.tvQuranTitle)
        llQuranControls = findViewById(R.id.llQuranControls)
        llIndexBuilding = findViewById(R.id.llIndexBuilding)
        tvBuildingIndexStatus = findViewById(R.id.tvBuildingIndexStatus)
        progressBuildIndex = findViewById(R.id.progressBuildIndex)
        llQuranRoot = findViewById(R.id.llQuranRoot)
        btnHome = findViewById(R.id.btnHome)

        val surahNames = loadSurahNames()
        val adapter = ArrayAdapter(this, R.layout.spinner_item, surahNames)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spSurahList.adapter = adapter
        spSurahList.visibility = View.GONE
        llQuranControls.visibility = View.GONE

        btnToggleFullscreen.setOnClickListener { toggleFullscreen() }
        btnPlayPause.setOnClickListener { togglePlayPause() }
        btnRewind10.setOnClickListener {
            if (queueIndex > 0) queueIndex--
            if (isAutoPlaying) playCurrentAyah()
        }
        btnForward10.setOnClickListener {
            queueIndex++
            if (isAutoPlaying) playCurrentAyah()
        }

        spSurahList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                stopPlayback()
                startFromSurah(position + 1)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        buildOrLoadSurahIndex()
    }

    private fun loadSurahNames(): List<String> {
        return try {
            val json = assets.open("surah_names.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            Array(114) { i -> "سورة رقم ${i + 1}" }.toList()
        }
    }

    private fun buildOrLoadSurahIndex() {
        val indexFile = File(filesDir, "surah_pages_index.json")
        if (indexFile.exists()) {
            try {
                loadIndexFromFile(indexFile)
                onIndexReady()
                return
            } catch (e: Exception) {
            }
        }

        llIndexBuilding.visibility = View.VISIBLE
        Thread {
            val map = IntArray(115)
            for (page in 1..604) {
                try {
                    val json = fetchPageJson(page)
                    val parsed = parsePage(page, json)
                    for (loc in parsed.ayahOrder) {
                        if (loc.ayah == 1 && loc.surah in 1..114 && map[loc.surah] == 0) {
                            map[loc.surah] = page
                        }
                    }
                } catch (e: Exception) {
                }
                val percent = (page * 100) / 604
                runOnUiThread {
                    progressBuildIndex.progress = percent
                    tvBuildingIndexStatus.text = "جاري تحضير فهرس المصحف لأول مرة... $page/604"
                }
            }
            try {
                val obj = JSONObject()
                for (s in 1..114) obj.put(s.toString(), map[s])
                indexFile.writeText(obj.toString())
            } catch (e: Exception) {
            }
            surahStartPage = map
            runOnUiThread { onIndexReady() }
        }.start()
    }

    private fun loadIndexFromFile(file: File) {
        val obj = JSONObject(file.readText())
        val map = IntArray(115)
        for (s in 1..114) map[s] = obj.optInt(s.toString(), 0)
        surahStartPage = map
    }

    private fun onIndexReady() {
        llIndexBuilding.visibility = View.GONE
        spSurahList.visibility = View.VISIBLE
        llQuranControls.visibility = View.VISIBLE
        startFromSurah(1)
    }

    private fun fetchPageJson(pageNumber: Int): String {
        val padded = String.format("%03d", pageNumber)
        val url = URL("https://raw.githubusercontent.com/zonetecde/mushaf-layout/refs/heads/main/mushaf/page-$padded.json")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.inputStream.bufferedReader(Charsets.UTF_8).use { return it.readText() }
    }

    private fun parsePage(pageNumber: Int, json: String): MushafPage {
        val root = JSONObject(json)
        val linesArray = root.getJSONArray("lines")
        val lines = mutableListOf<MushafLine>()
        val ayahOrder = mutableListOf<MushafWordLoc>()
        val seen = LinkedHashSet<String>()

        for (i in 0 until linesArray.length()) {
            val lineObj = linesArray.getJSONObject(i)
            when (lineObj.optString("type")) {
                "surah-header" -> lines.add(MushafLine("surah-header", lineObj.optString("text", "")))
                "basmala" -> lines.add(MushafLine("basmala", "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"))
                else -> {
                    lines.add(MushafLine("text", lineObj.optString("text", "")))
                    val words = lineObj.optJSONArray("words")
                    if (words != null) {
                        for (w in 0 until words.length()) {
                            val loc = words.getJSONObject(w).optString("location", "")
                            val parts = loc.split(":")
                            if (parts.size >= 2) {
                                val s = parts[0].toIntOrNull() ?: continue
                                val a = parts[1].toIntOrNull() ?: continue
                                val key = "$s:$a"
                                if (seen.add(key)) ayahOrder.add(MushafWordLoc(s, a))
                            }
                        }
                    }
                }
            }
        }
        return MushafPage(pageNumber, lines, ayahOrder)
    }

    private fun renderPage(page: MushafPage) {
        llMushafPageLines.removeAllViews()
        tvPageNumber.text = "صفحة ${page.pageNumber}"
        for (line in page.lines) {
            val tv = TextView(this)
            tv.text = line.text
            tv.gravity = Gravity.CENTER
            when (line.type) {
                "surah-header" -> {
                    tv.setTextColor(0xFF6B21A8.toInt())
                    tv.textSize = 17f
                    tv.setTypeface(null, Typeface.BOLD)
                    tv.setPadding(0, 28, 0, 12)
                }
                "basmala" -> {
                    tv.setTextColor(0xFF1A0B2E.toInt())
                    tv.textSize = 16f
                    tv.setPadding(0, 4, 0, 12)
                }
                else -> {
                    tv.setTextColor(0xFF1A0B2E.toInt())
                    tv.textSize = 16f
                    tv.setPadding(0, 5, 0, 5)
                    tv.setLineSpacing(6f, 1f)
                }
            }
            llMushafPageLines.addView(tv)
        }
    }

    private fun startFromSurah(surahNumber: Int) {
        val startPage = surahStartPage.getOrElse(surahNumber) { 0 }
        if (startPage == 0) {
            Toast.makeText(this, "تعذر تحديد صفحة هذه السورة، حاول لاحقاً", Toast.LENGTH_SHORT).show()
            return
        }
        loadPageAndQueue(startPage, filterSurah = surahNumber)
    }

    private fun loadPageAndQueue(pageNumber: Int, filterSurah: Int? = null) {
        if (pageNumber !in 1..604) return
        progressQuranPage.visibility = View.VISIBLE
        Thread {
            try {
                val json = fetchPageJson(pageNumber)
                val page = parsePage(pageNumber, json)
                runOnUiThread {
                    progressQuranPage.visibility = View.GONE
                    currentPage = page
                    renderPage(page)
                    playQueue = if (filterSurah != null) {
                        page.ayahOrder.filter { it.surah == filterSurah }.toMutableList()
                    } else {
                        page.ayahOrder.toMutableList()
                    }
                    queueIndex = 0
                    if (isAutoPlaying) playCurrentAyah()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressQuranPage.visibility = View.GONE
                    Toast.makeText(this@QuranActivity, "تعذر تحميل الصفحة، تحقق من الإنترنت", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun togglePlayPause() {
        if (isAutoPlaying) {
            isAutoPlaying = false
            mediaPlayer?.pause()
            btnPlayPause.text = "▶"
        } else {
            isAutoPlaying = true
            btnPlayPause.text = "⏸"
            val mp = mediaPlayer
            if (mp != null && !mp.isPlaying && queueIndex < playQueue.size) {
                mp.start()
            } else {
                playCurrentAyah()
            }
        }
    }

    private fun stopPlayback() {
        isAutoPlaying = false
        btnPlayPause.text = "▶"
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun playCurrentAyah() {
        if (queueIndex >= playQueue.size) {
            val nextPage = (currentPage?.pageNumber ?: 0) + 1
            if (nextPage <= 604) {
                loadPageAndQueue(nextPage)
            } else {
                isAutoPlaying = false
                btnPlayPause.text = "▶"
            }
            return
        }
        val loc = playQueue[queueIndex]
        val surahPadded = String.format("%03d", loc.surah)
        val ayahPadded = String.format("%03d", loc.ayah)
        val url = "https://everyayah.com/data/Alafasy_128kbps/$surahPadded$ayahPadded.mp3"

        mediaPlayer?.release()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    queueIndex++
                    if (isAutoPlaying) playCurrentAyah()
                }
            }
        } catch (e: Exception) {
            queueIndex++
            if (isAutoPlaying) playCurrentAyah()
        }
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        val visibility = if (isFullscreen) View.GONE else View.VISIBLE
        tvQuranTitle.visibility = visibility
        spSurahList.visibility = visibility
        llQuranControls.visibility = visibility
        btnHome.visibility = visibility
        llQuranRoot.setPadding(
            if (isFullscreen) 0 else (16 * resources.displayMetrics.density).toInt(),
            if (isFullscreen) 0 else (16 * resources.displayMetrics.density).toInt(),
            if (isFullscreen) 0 else (16 * resources.displayMetrics.density).toInt(),
            if (isFullscreen) 0 else (16 * resources.displayMetrics.density).toInt()
        )
        btnToggleFullscreen.setImageResource(
            if (isFullscreen) android.R.drawable.ic_menu_close_clear_cancel else android.R.drawable.ic_menu_view
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        isAutoPlaying = false
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
