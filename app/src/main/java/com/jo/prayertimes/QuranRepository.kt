package com.jo.prayertimes

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class SurahText(val number: Int, val name: String, val ayahs: List<String>)

/**
 * يوفّر نص القرآن الكريم كاملاً (رواية حفص عن عاصم، الرسم العثماني) من واجهة AlQuran Cloud،
 * وهي نفس المصدر (Tanzil.net) الذي يعتمده مشروع المصحف الإلكتروني بجامعة الملك سعود نفسه
 * لتوفير النص. يتم التحميل مرة واحدة فقط ثم حفظه محلياً للعمل بدون إنترنت لاحقاً.
 */
class QuranRepository(private val context: Context) {

    private val cacheFile = File(context.filesDir, "quran_full_uthmani.json")

    fun isDownloaded(): Boolean = cacheFile.exists() && cacheFile.length() > 100_000

    /** يُنفَّذ على خيط خلفي: يُحمّل النص الكامل من الإنترنت ويخزّنه محلياً */
    fun downloadFullQuran(): Boolean {
        return try {
            val url = URL("https://api.alquran.cloud/v1/quran/quran-uthmani")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 20000
            conn.readTimeout = 30000
            conn.requestMethod = "GET"
            val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            conn.disconnect()
            if (text.contains("\"surahs\"")) {
                cacheFile.writeText(text, Charsets.UTF_8)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    /** يعيد جميع السور من الملف المخزَّن محلياً؛ فارغة إن لم يكتمل التحميل بعد */
    fun loadAllSurahs(): List<SurahText> {
        if (!isDownloaded()) return emptyList()
        return try {
            val root = JSONObject(cacheFile.readText(Charsets.UTF_8))
            val surahsArr: JSONArray = root.getJSONObject("data").getJSONArray("surahs")
            val result = ArrayList<SurahText>()
            for (i in 0 until surahsArr.length()) {
                val s = surahsArr.getJSONObject(i)
                val ayahsArr = s.getJSONArray("ayahs")
                val ayahsList = ArrayList<String>()
                for (j in 0 until ayahsArr.length()) {
                    ayahsList.add(ayahsArr.getJSONObject(j).getString("text"))
                }
                result.add(SurahText(s.getInt("number"), s.getString("name"), ayahsList))
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}
