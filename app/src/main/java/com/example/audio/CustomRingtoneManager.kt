package com.example.audio

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class CustomRingtone(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val filePath: String,
    val durationMs: Long = 0L,
    val dateAdded: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("filePath", filePath)
        json.put("durationMs", durationMs)
        json.put("dateAdded", dateAdded)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): CustomRingtone {
            return CustomRingtone(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "Custom Ringtone"),
                filePath = json.optString("filePath", ""),
                durationMs = json.optLong("durationMs", 0L),
                dateAdded = json.optLong("dateAdded", System.currentTimeMillis())
            )
        }
    }
}

class CustomRingtoneManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("hitech_custom_ringtones", Context.MODE_PRIVATE)

    private val ringtoneDir: File by lazy {
        File(appContext.filesDir, "custom_ringtones").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Retrieves all saved custom ringtones.
     */
    fun getCustomRingtones(): List<CustomRingtone> {
        val jsonString = prefs.getString("custom_ringtones_json", null) ?: return emptyList()
        val list = mutableListOf<CustomRingtone>()
        try {
            val arr = JSONArray(jsonString)
            for (i in 0 until arr.length()) {
                val item = CustomRingtone.fromJson(arr.getJSONObject(i))
                // Only keep if local audio file still exists
                if (File(item.filePath).exists()) {
                    list.add(item)
                }
            }
        } catch (_: Exception) {}
        return list
    }

    /**
     * Imports an audio file selected via SAF / System Picker into app-internal storage.
     * Copies the content stream locally so permission persistence across reboots is guaranteed.
     */
    fun importRingtoneFromUri(uri: Uri): CustomRingtone? {
        try {
            val contentResolver = appContext.contentResolver
            var displayName = "Custom Audio"

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        displayName = name
                    }
                }
            }

            // Strip extension for clean sound label
            val cleanTitle = displayName.substringBeforeLast(".")

            val fileExt = displayName.substringAfterLast(".", "mp3")
            val targetFile = File(ringtoneDir, "ringtone_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$fileExt")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            val ringtone = CustomRingtone(
                name = cleanTitle,
                filePath = targetFile.absolutePath
            )

            saveRingtone(ringtone)
            return ringtone
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Deletes a custom ringtone file and removes it from preferences.
     */
    fun deleteRingtone(id: String) {
        val current = getCustomRingtones().toMutableList()
        val item = current.find { it.id == id }
        if (item != null) {
            try {
                File(item.filePath).delete()
            } catch (_: Exception) {}
            current.remove(item)
            persistList(current)
        }
    }

    private fun saveRingtone(ringtone: CustomRingtone) {
        val current = getCustomRingtones().toMutableList()
        current.add(0, ringtone)
        persistList(current)
    }

    private fun persistList(list: List<CustomRingtone>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("custom_ringtones_json", arr.toString()).apply()
    }

    /**
     * Finds a custom ringtone by its identifier, name, or file path.
     */
    fun findRingtone(identifier: String): CustomRingtone? {
        val list = getCustomRingtones()
        return list.find { it.id == identifier || it.name == identifier || it.filePath == identifier }
    }
}
