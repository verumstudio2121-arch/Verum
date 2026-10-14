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
        }

        val now = System.currentTimeMillis()
        var cleaned = false
        for (i in loaded.indices) {
            val a = loaded[i]
            if (a.skippedUntilEpochMillis in 1..now) {
                loaded[i] = a.copy(skippedUntilEpochMillis = 0L)
                cleaned = true
            }
        }
        if (cleaned) {
            persistAlarms(loaded)
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
            val updated = current[index].copy(
                isEnabled = enabled,
                skippedUntilEpochMillis = if (enabled) 0L else current[index].skippedUntilEpochMillis
            )
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

    /**
     * FEATURE 1: OPTION 1 - ONLY FOR TOMORROW
     * Temporarily skips the next scheduled occurrence without changing repeat settings.
     * Automatically schedules for the subsequent occurrence so it resumes normal schedule.
     */
    fun skipAlarmTomorrow(id: String) {
        val current = _alarms.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val alarm = current[index]
            // Calculate when the next occurrence would be
            val nextOccurrence = alarm.getNextTriggerEpochMillis()
            // Set skip boundary to just after the skipped occurrence (+1 minute)
            val updated = alarm.copy(
                isEnabled = true,
                skippedUntilEpochMillis = nextOccurrence + 60_000L
            )
            current[index] = updated
            _alarms.value = current
            persistAlarms(current)

            // Reschedule so AlarmManager targets the subsequent occurrence (for repeat alarms)
            // or cancels if one-time
            if (updated.repeatType == "Once") {
                scheduler.cancel(alarm)
            } else {
                scheduler.schedule(updated)
            }
        }
    }

    /**
     * FEATURE 1: OPTION 2 - UNTIL I TURN IT ON AGAIN
     * Permanently disables the alarm until manually enabled.
     */
    fun disableAlarmPermanently(id: String) {
        val current = _alarms.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = current[index].copy(
                isEnabled = false,
                skippedUntilEpochMillis = 0L
            )
            current[index] = updated
            _alarms.value = current
            persistAlarms(current)
            scheduler.cancel(updated)
        }
    }

    /**
     * Restores an active schedule if alarm was temporarily skipped.
     */
    fun unskipAlarm(id: String) {
        val current = _alarms.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = current[index].copy(
                isEnabled = true,
                skippedUntilEpochMillis = 0L
            )
            current[index] = updated
            _alarms.value = current
            persistAlarms(current)
            scheduler.schedule(updated)
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
        val now = System.currentTimeMillis()
        val current = _alarms.value.toMutableList()
        var changed = false
        for (i in current.indices) {
            val a = current[i]
            if (a.skippedUntilEpochMillis in 1..now) {
                current[i] = a.copy(skippedUntilEpochMillis = 0L)
                changed = true
            }
        }
        if (changed) {
            _alarms.value = current
            persistAlarms(current)
        }

        current.filter { it.isEnabled }.forEach { alarm ->
            if (alarm.repeatType == "Once" && alarm.isTemporarilySkipped()) {
                scheduler.cancel(alarm)
            } else {
                scheduler.schedule(alarm)
            }
        }
    }
}
