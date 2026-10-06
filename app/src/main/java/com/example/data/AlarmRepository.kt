package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.alarm.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class AlarmRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("hitech_alarms_db", Context.MODE_PRIVATE)
    private val scheduler = AlarmScheduler(context)

    private val _alarms = MutableStateFlow<List<AlarmModel>>(emptyList())
    val alarms: StateFlow<List<AlarmModel>> = _alarms.asStateFlow()

    init {
        loadAlarms()
    }

    private fun loadAlarms() {
        val jsonString = prefs.getString("alarms_json", null)
        val loaded = mutableListOf<AlarmModel>()
        if (jsonString != null) {
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    loaded.add(AlarmModel.fromJson(array.getJSONObject(i)))
                }
            } catch (_: Exception) {}
        }

        if (loaded.isEmpty() && !prefs.getBoolean("has_initialized_defaults", false)) {
            // Seed initial realistic alarms inspired by user prompt
            val defaultAlarms = listOf(
                AlarmModel(
                    hour = 7,
                    minute = 0,
                    label = "Wake Up",
                    isEnabled = true,
                    repeatType = "Monday-Friday",
                    soundName = "Gentle",
                    vibrate = true,
                    snoozeEnabled = true
                ),
                AlarmModel(
                    hour = 8,
                    minute = 30,
                    label = "Workout",
                    isEnabled = false,
                    repeatType = "Custom",
                    customDays = setOf(1, 3, 5), // Mon, Wed, Fri
                    soundName = "Wake Up",
                    vibrate = true,
                    snoozeEnabled = true
                )
            )
            loaded.addAll(defaultAlarms)
            prefs.edit().putBoolean("has_initialized_defaults", true).apply()
            persistAlarms(loaded)
            // Schedule any enabled defaults
            defaultAlarms.filter { it.isEnabled }.forEach { scheduler.schedule(it) }
        }

        _alarms.value = loaded.sortedWith(compareBy({ it.hour }, { it.minute }))
    }

    private fun persistAlarms(list: List<AlarmModel>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        prefs.edit().putString("alarms_json", array.toString()).apply()
    }

    fun addAlarm(alarm: AlarmModel) {
        val current = _alarms.value.toMutableList()
        current.add(alarm)
        current.sortWith(compareBy({ it.hour }, { it.minute }))
        _alarms.value = current
        persistAlarms(current)

        if (alarm.isEnabled) {
            scheduler.schedule(alarm)
        }
    }

    fun updateAlarm(alarm: AlarmModel) {
        val current = _alarms.value.toMutableList()
        val index = current.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            current[index] = alarm
            current.sortWith(compareBy({ it.hour }, { it.minute }))
            _alarms.value = current
            persistAlarms(current)

            if (alarm.isEnabled) {
                scheduler.schedule(alarm)
            } else {
                scheduler.cancel(alarm)
            }
        }
    }

    fun toggleAlarm(id: String, enabled: Boolean) {
        val current = _alarms.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = current[index].copy(isEnabled = enabled)
            current[index] = updated
            _alarms.value = current
            persistAlarms(current)

            if (enabled) {
                scheduler.schedule(updated)
            } else {
                scheduler.cancel(updated)
            }
        }
    }

    fun deleteAlarm(id: String) {
        val current = _alarms.value.toMutableList()
        val alarm = current.find { it.id == id }
        if (alarm != null) {
            scheduler.cancel(alarm)
            current.remove(alarm)
            _alarms.value = current
            persistAlarms(current)
        }
    }

    fun getAlarm(id: String): AlarmModel? {
        return _alarms.value.find { it.id == id }
    }

    fun rescheduleAllEnabledAlarms() {
        _alarms.value.filter { it.isEnabled }.forEach {
            scheduler.schedule(it)
        }
    }
}
