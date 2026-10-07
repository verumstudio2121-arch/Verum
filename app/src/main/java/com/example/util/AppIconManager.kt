package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

data class CustomAppIcon(
    val id: String,
    val name: String,
    val isPreset: Boolean = false,
    val aliasName: String,
    val customImageFilePath: String? = null
)

class AppIconManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("hitech_app_icon_prefs", Context.MODE_PRIVATE)

    private val iconDir: File by lazy {
        File(appContext.filesDir, "custom_app_icons").apply {
            if (!exists()) mkdirs()
        }
    }

    private val _currentIconStyle = MutableStateFlow(getSavedIconStyle())
    val currentIconStyle: StateFlow<String> = _currentIconStyle.asStateFlow()

    private val _customIconBitmap = MutableStateFlow<Bitmap?>(loadCustomIconBitmap())
    val customIconBitmap: StateFlow<Bitmap?> = _customIconBitmap.asStateFlow()

    companion object {
        const val ICON_DEFAULT = "default"
        const val ICON_CUSTOM = "custom"
        const val ICON_NEON = "neon"
        const val ICON_MINIMAL = "minimal"

        const val ALIAS_MAIN = "com.example.MainActivity"
        const val ALIAS_CUSTOM = "com.example.MainActivityAliasCustom"
        const val ALIAS_NEON = "com.example.MainActivityAliasNeon"
        const val ALIAS_MINIMAL = "com.example.MainActivityAliasMinimal"
    }

    fun getSavedIconStyle(): String {
        return prefs.getString("selected_icon_style", ICON_DEFAULT) ?: ICON_DEFAULT
    }

    fun getCustomIconFilePath(): String? {
        val path = prefs.getString("custom_icon_file_path", null)
        return if (path != null && File(path).exists()) path else null
    }

    private fun loadCustomIconBitmap(): Bitmap? {
        val path = getCustomIconFilePath() ?: return null
        return try {
            BitmapFactory.decodeFile(path)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Saves an image selected from the gallery / photo picker as the user's custom app icon.
     * Resizes and crops to a square bitmap and saves in app internal storage.
     */
    fun setCustomIconFromUri(uri: Uri): Boolean {
        return try {
            val contentResolver = appContext.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return false
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return false

            // Crop to center square if rectangular
            val size = minOf(originalBitmap.width, originalBitmap.height)
            val x = (originalBitmap.width - size) / 2
            val y = (originalBitmap.height - size) / 2
            val squareBitmap = Bitmap.createBitmap(originalBitmap, x, y, size, size)

            // Scale to standard crisp 512x512
            val finalBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

            val targetFile = File(iconDir, "user_app_icon.png")
            FileOutputStream(targetFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            prefs.edit()
                .putString("custom_icon_file_path", targetFile.absolutePath)
                .putString("selected_icon_style", ICON_CUSTOM)
                .apply()

            _customIconBitmap.value = finalBitmap
            _currentIconStyle.value = ICON_CUSTOM

            // Switch active launcher activity-alias
            applyLauncherAlias(ALIAS_CUSTOM)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Sets one of the built-in presets: Default, Neon, or Minimal.
     */
    fun setPresetIconStyle(style: String) {
        val targetAlias = when (style) {
            ICON_NEON -> ALIAS_NEON
            ICON_MINIMAL -> ALIAS_MINIMAL
            ICON_CUSTOM -> {
                if (getCustomIconFilePath() != null) ALIAS_CUSTOM else ALIAS_MAIN
            }
            else -> ALIAS_MAIN
        }

        prefs.edit().putString("selected_icon_style", style).apply()
        _currentIconStyle.value = style
        applyLauncherAlias(targetAlias)
    }

    /**
     * Resets back to default liquid glass timepiece launcher icon.
     */
    fun resetToDefault() {
        prefs.edit().putString("selected_icon_style", ICON_DEFAULT).apply()
        _currentIconStyle.value = ICON_DEFAULT
        applyLauncherAlias(ALIAS_MAIN)
    }

    private fun applyLauncherAlias(activeAlias: String) {
        try {
            val pm = appContext.packageManager
            val allAliases = listOf(ALIAS_MAIN, ALIAS_CUSTOM, ALIAS_NEON, ALIAS_MINIMAL)

            for (alias in allAliases) {
                val component = ComponentName(appContext.packageName, alias)
                val newState = if (alias == activeAlias) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }

                // Only update if changed to avoid unnecessary launcher restarts
                if (pm.getComponentEnabledSetting(component) != newState) {
                    pm.setComponentEnabledSetting(
                        component,
                        newState,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
