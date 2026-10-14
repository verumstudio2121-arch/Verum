package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val appearance: String = "Dark", // Dark, Light, System
    val accentColor: String = "Light Blue", // Light Blue, Lavender, Mint, Coral, or custom hex
    val interfaceStyle: String = "Mix Liquid", // "Mix Liquid", "Pure Liquid Glass", "Minimal"
    val clockFormat: String = "12 hour", // 12 hour, 24 hour
    val showSeconds: Boolean = false,
    val alarmVolume: Float = 0.85f,
    val defaultAlarmSound: String = "Gentle",
    val vibrationEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 10, // 5, 10, 15, 20, 30
    val timerSound: String = "Bell",
    val backgroundType: String = "Black", // "Black" (Default), "Cosmos", "Cyber Grid", "Aurora Wave", "Midnight Deep", "Minimal Slate", "Custom Photo"
    val customPhotoPath: String? = null,
    val backgroundDim: Float = 0.40f
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
            interfaceStyle = prefs.getString("interface_style", "Mix Liquid") ?: "Mix Liquid",
            clockFormat = prefs.getString("clock_format", "12 hour") ?: "12 hour",
            showSeconds = prefs.getBoolean("show_seconds", false),
            alarmVolume = prefs.getFloat("alarm_volume", 0.85f),
            defaultAlarmSound = prefs.getString("default_alarm_sound", "Gentle") ?: "Gentle",
            vibrationEnabled = prefs.getBoolean("vibration_enabled", true),
            snoozeDurationMinutes = prefs.getInt("snooze_duration", 10),
            timerSound = prefs.getString("timer_sound", "Bell") ?: "Bell",
            backgroundType = prefs.getString("background_type", "Black") ?: "Black",
            customPhotoPath = prefs.getString("custom_photo_path", null),
            backgroundDim = prefs.getFloat("background_dim", 0.40f)
        )
    }

    fun updateAppearance(value: String) {
        prefs.edit().putString("appearance", value).apply()
        _settings.value = _settings.value.copy(appearance = value)
    }

    fun updateInterfaceStyle(value: String) {
        prefs.edit().putString("interface_style", value).apply()
        _settings.value = _settings.value.copy(interfaceStyle = value)
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

    fun updateBackgroundType(value: String) {
        prefs.edit().putString("background_type", value).apply()
        _settings.value = _settings.value.copy(backgroundType = value)
    }

    fun updateCustomPhotoPath(path: String?) {
        if (path == null) {
            prefs.edit().remove("custom_photo_path").apply()
        } else {
            prefs.edit().putString("custom_photo_path", path).apply()
        }
        _settings.value = _settings.value.copy(customPhotoPath = path)
    }

    fun updateBackgroundDim(dim: Float) {
        val clamped = dim.coerceIn(0.0f, 0.90f)
        prefs.edit().putFloat("background_dim", clamped).apply()
        _settings.value = _settings.value.copy(backgroundDim = clamped)
    }
}
