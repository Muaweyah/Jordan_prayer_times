package com.jo.prayertimes

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** يرفع وينزّل ملف النسخة الاحتياطية من/إلى مساحة appDataFolder الخاصة بالتطبيق على Drive،
 *  عبر واجهة Drive REST v3 مباشرة (بدون مكتبات Google API الثقيلة).
 *  appDataFolder غير ظاهرة للمستخدم بتطبيق Drive العادي؛ خاصة بهذا التطبيق فقط. */
class DriveSyncManager {

    companion object {
        private const val BACKUP_FILE_NAME = "jordan_prayer_backup.json"
        private const val FILES_ENDPOINT = "https://www.googleapis.com/drive/v3/files"
        private const val UPLOAD_ENDPOINT = "https://www.googleapis.com/upload/drive/v3/files"
    }

    /** تُستدعى من خيط خلفي فقط؛ ترجع معرّف الملف إن وُجد بمساحة appDataFolder أو null */
    @Throws(Exception::class)
    fun findBackupFileId(accessToken: String): String? {
        val query = URLEncoder.encode("name='$BACKUP_FILE_NAME'", "UTF-8")
        val fields = URLEncoder.encode("files(id,name)", "UTF-8")
        val url = URL("$FILES_ENDPOINT?spaces=appDataFolder&q=$query&fields=$fields")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        val response = readResponse(conn)
        conn.disconnect()
        val files = JSONObject(response).optJSONArray("files") ?: return null
        return if (files.length() > 0) files.getJSONObject(0).getString("id") else null
    }

    /** ينشئ الملف إن لم يكن موجوداً، أو يحدّث محتواه إن كان موجوداً مسبقاً */
    @Throws(Exception::class)
    fun uploadBackup(accessToken: String, jsonContent: String) {
        val existingId = findBackupFileId(accessToken)
        if (existingId == null) {
            createBackupFile(accessToken, jsonContent)
        } else {
            updateBackupFile(accessToken, existingId, jsonContent)
        }
    }

    /** يجلب محتوى النسخة الاحتياطية المخزّنة على Drive، أو null إن لم تكن موجودة بعد */
    @Throws(Exception::class)
    fun downloadBackup(accessToken: String): String? {
        val fileId = findBackupFileId(accessToken) ?: return null
        val url = URL("$FILES_ENDPOINT/$fileId?alt=media")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        val response = readResponse(conn)
        conn.disconnect()
        return response
    }

    private fun createBackupFile(accessToken: String, jsonContent: String) {
        val boundary = "jordan_prayer_backup_boundary"
        val metadata = JSONObject().apply {
            put("name", BACKUP_FILE_NAME)
            put("parents", JSONArray().put("appDataFolder"))
        }
        val body = buildString {
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata.toString())
            append("\r\n--").append(boundary).append("\r\n")
            append("Content-Type: application/json\r\n\r\n")
            append(jsonContent)
            append("\r\n--").append(boundary).append("--")
        }

        val url = URL("$UPLOAD_ENDPOINT?uploadType=multipart")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        writeBody(conn, body)
        readResponse(conn)
        conn.disconnect()
    }

    private fun updateBackupFile(accessToken: String, fileId: String, jsonContent: String) {
        val url = URL("$UPLOAD_ENDPOINT/$fileId?uploadType=media")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "PATCH"
        conn.doOutput = true
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.setRequestProperty("Content-Type", "application/json")
        writeBody(conn, jsonContent)
        readResponse(conn)
        conn.disconnect()
    }

    private fun writeBody(conn: HttpURLConnection, body: String) {
        val out: OutputStream = conn.outputStream
        out.write(body.toByteArray(StandardCharsets.UTF_8))
        out.flush()
        out.close()
    }

    private fun readResponse(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val reader = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8))
        val text = reader.readText()
        reader.close()
        if (code !in 200..299) {
            throw Exception("Drive API error ($code): $text")
        }
        return text
    }
}
