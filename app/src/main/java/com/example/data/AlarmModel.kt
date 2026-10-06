package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class AlarmModel(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val label: String = "Alarm",
    val isEnabled: Boolean = true,
    val repeatType: String = "Once", // "Once", "Every day", "Monday-Friday", "Saturday-Sunday", "Custom"
    val customDays: Set<Int> = emptySet(), // 1 = Monday ... 7 = Sunday
    val soundName: String = "Gentle",
    val vibrate: Boolean = true,
    val snoozeEnabled: Boolean = true
) {
    fun getFormattedTime(is24Hour: Boolean = false): String {
        return if (is24Hour) {
            String.format("%02d:%02d", hour, minute)
        } else {
            val h = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            String.format("%d:%02d", h, minute)
        }
    }

    fun getAmPm(): String {
        return if (hour < 12) "AM" else "PM"
    }

    fun getRepeatSummary(): String {
        return when (repeatType) {
            "Once" -> "Once"
            "Every day" -> "Every day"
            "Monday-Friday" -> "Mon - Fri"
            "Saturday-Sunday" -> "Sat - Sun"
            "Custom" -> {
                if (customDays.isEmpty()) "Once"
                else if (customDays.size == 7) "Every day"
                else {
                    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    customDays.sorted().joinToString(" ") { dayNames[it - 1] }
                }
            }
            else -> repeatType
        }
    }

    /**
     * Calculates the next epoch timestamp (in milliseconds) when this alarm should fire.
     */
    fun getNextTriggerEpochMillis(now: ZonedDateTime = ZonedDateTime.now()): Long {
        val targetLocalTime = LocalTime.of(hour, minute, 0)
        var triggerDateTime = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        when (repeatType) {
            "Once" -> {
                if (!triggerDateTime.isAfter(now)) {
                    triggerDateTime = triggerDateTime.plusDays(1)
                }
            }
            "Every day" -> {
                if (!triggerDateTime.isAfter(now)) {
                    triggerDateTime = triggerDateTime.plusDays(1)
                }
            }
            "Monday-Friday" -> {
                while (!triggerDateTime.isAfter(now) ||
                    triggerDateTime.dayOfWeek == DayOfWeek.SATURDAY ||
                    triggerDateTime.dayOfWeek == DayOfWeek.SUNDAY
                ) {
                    triggerDateTime = triggerDateTime.plusDays(1)
                }
            }
            "Saturday-Sunday" -> {
                while (!triggerDateTime.isAfter(now) ||
                    (triggerDateTime.dayOfWeek != DayOfWeek.SATURDAY &&
                     triggerDateTime.dayOfWeek != DayOfWeek.SUNDAY)
                ) {
                    triggerDateTime = triggerDateTime.plusDays(1)
                }
            }
            "Custom" -> {
                if (customDays.isEmpty()) {
                    if (!triggerDateTime.isAfter(now)) {
                        triggerDateTime = triggerDateTime.plusDays(1)
                    }
                } else {
                    var count = 0
                    while (count < 14) { // safety limit
                        val currentDayInt = triggerDateTime.dayOfWeek.value // 1 (Mon) to 7 (Sun)
                        if (triggerDateTime.isAfter(now) && customDays.contains(currentDayInt)) {
                            break
                        }
                        triggerDateTime = triggerDateTime.plusDays(1)
                        count++
                    }
                }
            }
        }

        return triggerDateTime.toInstant().toEpochMilli()
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("hour", hour)
        json.put("minute", minute)
        json.put("label", label)
        json.put("isEnabled", isEnabled)
        json.put("repeatType", repeatType)
        val daysArray = JSONArray()
        customDays.forEach { daysArray.put(it) }
        json.put("customDays", daysArray)
        json.put("soundName", soundName)
        json.put("vibrate", vibrate)
        json.put("snoozeEnabled", snoozeEnabled)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): AlarmModel {
            val customDays = mutableSetOf<Int>()
            val daysArray = json.optJSONArray("customDays")
            if (daysArray != null) {
                for (i in 0 until daysArray.length()) {
                    customDays.add(daysArray.getInt(i))
                }
            }
            return AlarmModel(
                id = json.getString("id"),
                hour = json.getInt("hour"),
                minute = json.getInt("minute"),
                label = json.optString("label", "Alarm"),
                isEnabled = json.optBoolean("isEnabled", true),
                repeatType = json.optString("repeatType", "Once"),
                customDays = customDays,
                soundName = json.optString("soundName", "Gentle"),
                vibrate = json.optBoolean("vibrate", true),
                snoozeEnabled = json.optBoolean("snoozeEnabled", true)
            )
        }
    }
}
