package com.example.worldclock

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.abs

data class WorldCity(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val country: String,
    val timezoneId: String
) {
    fun getZonedDateTime(): ZonedDateTime {
        return try {
            ZonedDateTime.now(ZoneId.of(timezoneId))
        } catch (_: Exception) {
            ZonedDateTime.now(ZoneId.of("UTC"))
        }
    }

    fun getFormattedTime(is24Hour: Boolean = false): String {
        val dt = getZonedDateTime()
        val pattern = if (is24Hour) "HH:mm" else "h:mm"
        return dt.format(DateTimeFormatter.ofPattern(pattern))
    }

    fun getAmPm(): String {
        return getZonedDateTime().format(DateTimeFormatter.ofPattern("a")).uppercase()
    }

    /**
     * Calculates "Today", "Tomorrow", or "Yesterday" relative to user's device local date.
     */
    fun getDayRelation(localZone: ZoneId = ZoneId.systemDefault()): String {
        val localDate = LocalDate.now(localZone)
        val cityDate = LocalDate.now(try { ZoneId.of(timezoneId) } catch (_: Exception) { ZoneId.of("UTC") })

        val daysDiff = ChronoUnit.DAYS.between(localDate, cityDate)
        return when {
            daysDiff == 0L -> "Today"
            daysDiff > 0L -> "Tomorrow"
            else -> "Yesterday"
        }
    }

    /**
     * Calculates time difference relative to user's local timezone (e.g. "+8 HRS", "-3 HRS", "Same time").
     */
    fun getTimeDifferenceString(localZone: ZoneId = ZoneId.systemDefault()): String {
        return try {
            val now = Instant.now()
            val localOffsetSeconds = localZone.rules.getOffset(now).totalSeconds
            val cityOffsetSeconds = ZoneId.of(timezoneId).rules.getOffset(now).totalSeconds

            val diffSeconds = cityOffsetSeconds - localOffsetSeconds
            val diffHours = diffSeconds / 3600
            val diffMinutes = (abs(diffSeconds) % 3600) / 60

            when {
                diffSeconds == 0 -> "Same time"
                diffMinutes == 0 -> {
                    val sign = if (diffHours > 0) "+" else ""
                    "$sign$diffHours ${if (abs(diffHours) == 1) "HR" else "HRS"}"
                }
                else -> {
                    val sign = if (diffSeconds > 0) "+" else "-"
                    "$sign${abs(diffHours)}h ${diffMinutes}m"
                }
            }
        } catch (_: Exception) {
            "0 HRS"
        }
    }
}
