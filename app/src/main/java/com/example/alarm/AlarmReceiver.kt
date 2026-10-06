package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.data.AlarmRepository
import com.example.data.PreferencesManager

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.example.alarm.ACTION_TRIGGER_ALARM"
        const val ACTION_DISMISS_ALARM = "com.example.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.ACTION_SNOOZE_ALARM"

        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_ALARM_SOUND = "alarm_sound"
        const val EXTRA_ALARM_VIBRATE = "alarm_vibrate"
        const val EXTRA_ALARM_SNOOZE_ENABLED = "alarm_snooze_enabled"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: ACTION_TRIGGER_ALARM
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: "default_alarm"
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Wake Up"
        val sound = intent.getStringExtra(EXTRA_ALARM_SOUND) ?: "Gentle"
        val vibrate = intent.getBooleanExtra(EXTRA_ALARM_VIBRATE, true)
        val snoozeEnabled = intent.getBooleanExtra(EXTRA_ALARM_SNOOZE_ENABLED, true)

        when (action) {
            ACTION_DISMISS_ALARM -> {
                AlarmService.stopAlarm(context)
                // Handle repetition state in repository
                val repo = AlarmRepository(context)
                val alarm = repo.getAlarm(alarmId)
                if (alarm != null) {
                    if (alarm.repeatType == "Once") {
                        repo.toggleAlarm(alarmId, false)
                    } else {
                        // Re-schedule next repeat occurrence
                        repo.updateAlarm(alarm)
                    }
                }
            }

            ACTION_SNOOZE_ALARM -> {
                AlarmService.stopAlarm(context)
                val prefs = PreferencesManager(context)
                val snoozeMinutes = prefs.settings.value.snoozeDurationMinutes
                val scheduler = AlarmScheduler(context)
                scheduler.scheduleSnooze(alarmId, label, sound, vibrate, snoozeMinutes)
            }

            ACTION_TRIGGER_ALARM -> {
                // Wake device
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "HiTechClock:AlarmWakeLock"
                )
                wakeLock?.acquire(10000L)

                // Start Foreground Service to play alarm audio & vibrate
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    this.action = AlarmService.ACTION_START_ALARM
                    putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmService.EXTRA_ALARM_LABEL, label)
                    putExtra(AlarmService.EXTRA_ALARM_SOUND, sound)
                    putExtra(AlarmService.EXTRA_ALARM_VIBRATE, vibrate)
                    putExtra(AlarmService.EXTRA_ALARM_SNOOZE_ENABLED, snoozeEnabled)
                }
                ContextCompat.startForegroundService(context, serviceIntent)

                // Launch Full Screen Ringing Activity
                val ringingIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NO_USER_ACTION
                    putExtra(AlarmRingingActivity.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmRingingActivity.EXTRA_ALARM_LABEL, label)
                    putExtra(AlarmRingingActivity.EXTRA_ALARM_SOUND, sound)
                    putExtra(AlarmRingingActivity.EXTRA_ALARM_VIBRATE, vibrate)
                    putExtra(AlarmRingingActivity.EXTRA_ALARM_SNOOZE_ENABLED, snoozeEnabled)
                }
                try {
                    context.startActivity(ringingIntent)
                } catch (_: Exception) {}
            }
        }
    }
}
