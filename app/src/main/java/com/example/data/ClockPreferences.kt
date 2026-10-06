package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val appearance: String = "Dark", // Dark, Light, System
    val accentColor: String = "Light Blue", // Light Blue, Lavender, Mint, Coral
    val clockFormat: String = "12 hour", // 12 hour, 24 hour
    val showSeconds: Boolean = false,
    val alarmVolume: Float = 0.85f,
    val defaultAlarmSound: String = "Gentle",
    val vibrationEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 10, // 5, 10, 15, 20, 30
    val timerSound: String = "Bell"
)

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("hitech_clock_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            appearance = prefs.getString("appearance", "Dark") ?: "Dark",
            accentColor = prefs.getString("accent_color", "Light Blue") ?: "Light Blue",
            clockFormat = prefs.getString("clock_format", "12 hour") ?: "12 hour",
            showSeconds = prefs.getBoolean("show_seconds", false),
            alarmVolume = prefs.getFloat("alarm_volume", 0.85f),
            defaultAlarmSound = prefs.getString("default_alarm_sound", "Gentle") ?: "Gentle",
            vibrationEnabled = prefs.getBoolean("vibration_enabled", true),
            snoozeDurationMinutes = prefs.getInt("snooze_duration", 10),
            timerSound = prefs.getString("timer_sound", "Bell") ?: "Bell"
        )
    }

    fun updateAppearance(value: String) {
        prefs.edit().putString("appearance", value).apply()
        _settings.value = _settings.value.copy(appearance = value)
    }

    fun updateAccentColor(value: String) {
        prefs.edit().putString("accent_color", value).apply()
        _settings.value = _settings.value.copy(accentColor = value)
    }

    fun updateClockFormat(value: String) {
        prefs.edit().putString("clock_format", value).apply()
        _settings.value = _settings.value.copy(clockFormat = value)
    }

    fun updateShowSeconds(value: Boolean) {
        prefs.edit().putBoolean("show_seconds", value).apply()
        _settings.value = _settings.value.copy(showSeconds = value)
    }

    fun updateAlarmVolume(value: Float) {
        prefs.edit().putFloat("alarm_volume", value).apply()
        _settings.value = _settings.value.copy(alarmVolume = value)
    }

    fun updateDefaultAlarmSound(value: String) {
        prefs.edit().putString("default_alarm_sound", value).apply()
        _settings.value = _settings.value.copy(defaultAlarmSound = value)
    }

    fun updateVibrationEnabled(value: Boolean) {
        prefs.edit().putBoolean("vibration_enabled", value).apply()
        _settings.value = _settings.value.copy(vibrationEnabled = value)
    }

    fun updateSnoozeDuration(minutes: Int) {
        prefs.edit().putInt("snooze_duration", minutes).apply()
        _settings.value = _settings.value.copy(snoozeDurationMinutes = minutes)
    }

    fun updateTimerSound(value: String) {
        prefs.edit().putString("timer_sound", value).apply()
        _settings.value = _settings.value.copy(timerSound = value)
    }
}
