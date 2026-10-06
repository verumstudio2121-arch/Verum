package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.audio.SoundPlayer
import com.example.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AlarmService : Service() {

    private var vibratorJob: Job? = null
    private var vibrator: Vibrator? = null

    companion object {
        const val CHANNEL_ID = "hitech_alarm_service_channel"
        const val NOTIFICATION_ID = 2026

        const val ACTION_START_ALARM = "com.example.alarm.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.alarm.ACTION_STOP_ALARM"

        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_ALARM_SOUND = "alarm_sound"
        const val EXTRA_ALARM_VIBRATE = "alarm_vibrate"
        const val EXTRA_ALARM_SNOOZE_ENABLED = "alarm_snooze_enabled"

        fun stopAlarm(context: Context) {
            val stopIntent = Intent(context, AlarmService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(stopIntent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initVibrator()
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "HiTech Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarm ringing notifications"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == ACTION_STOP_ALARM) {
            stopForegroundAndCleanUp()
            return START_NOT_STICKY
        }

        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: "default_alarm"
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Wake Up"
        val sound = intent.getStringExtra(EXTRA_ALARM_SOUND) ?: "Gentle"
        val shouldVibrate = intent.getBooleanExtra(EXTRA_ALARM_VIBRATE, true)
        val snoozeEnabled = intent.getBooleanExtra(EXTRA_ALARM_SNOOZE_ENABLED, true)

        val notification = buildAlarmNotification(alarmId, label, sound, shouldVibrate, snoozeEnabled)
        startForeground(NOTIFICATION_ID, notification)

        // Read user volume preference
        val prefs = PreferencesManager(applicationContext)
        val volume = prefs.settings.value.alarmVolume

        // Start looping audio
        SoundPlayer.startAlarmLoop(sound, volume)

        // Start looping vibration if enabled
        if (shouldVibrate && prefs.settings.value.vibrationEnabled) {
            startVibrationLoop()
        }

        return START_STICKY
    }

    private fun buildAlarmNotification(
        alarmId: String,
        label: String,
        sound: String,
        vibrate: Boolean,
        snoozeEnabled: Boolean
    ): Notification {
        // Full screen ringing activity intent
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, label)
            putExtra(EXTRA_ALARM_SOUND, sound)
            putExtra(EXTRA_ALARM_VIBRATE, vibrate)
            putExtra(EXTRA_ALARM_SNOOZE_ENABLED, snoozeEnabled)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            alarmId.hashCode() + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, label)
            putExtra(EXTRA_ALARM_SOUND, sound)
            putExtra(EXTRA_ALARM_VIBRATE, vibrate)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            alarmId.hashCode() + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("HiTech Alarm: $label")
            .setContentText("Alarm is ringing")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)

        if (snoozeEnabled) {
            builder.addAction(android.R.drawable.ic_popup_reminder, "Snooze", snoozePendingIntent)
        }

        return builder.build()
    }

    private fun startVibrationLoop() {
        stopVibrationLoop()
        vibratorJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 300, 600), -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(longArrayOf(0, 600, 300, 600), -1)
                    }
                } catch (_: Exception) {}
                delay(2000)
            }
        }
    }

    private fun stopVibrationLoop() {
        vibratorJob?.cancel()
        vibratorJob = null
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    private fun stopForegroundAndCleanUp() {
        SoundPlayer.stopAlarmLoop()
        stopVibrationLoop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopForegroundAndCleanUp()
        super.onDestroy()
    }
}
