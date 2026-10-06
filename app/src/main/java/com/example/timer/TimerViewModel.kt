package com.example.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MainActivity
import com.example.audio.SoundPlayer
import com.example.data.PreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

class TimerViewModel(private val appContext: Context) : ViewModel() {

    private var totalDurationMillis = 0L
    private var remainingMillisAtPause = 0L
    private var targetEpochMillis = 0L
    private var timerJob: Job? = null

    private val _state = MutableStateFlow(TimerState.IDLE)
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    private val _totalMillis = MutableStateFlow(0L)
    val totalMillis: StateFlow<Long> = _totalMillis.asStateFlow()

    // Picked values when idle
    val pickedHours = MutableStateFlow(0)
    val pickedMinutes = MutableStateFlow(5)
    val pickedSeconds = MutableStateFlow(0)

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createTimerNotificationChannel()
    }

    private fun createTimerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "hitech_timer_channel",
                "Timer Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Countdown timer status and completion"
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun setDuration(hours: Int, minutes: Int, seconds: Int) {
        if (_state.value != TimerState.IDLE) return
        pickedHours.value = hours
        pickedMinutes.value = minutes
        pickedSeconds.value = seconds
    }

    fun selectPreset(minutes: Int) {
        if (_state.value != TimerState.IDLE) return
        pickedHours.value = 0
        pickedMinutes.value = minutes
        pickedSeconds.value = 0
    }

    fun start() {
        val totalSecs = (pickedHours.value * 3600) + (pickedMinutes.value * 60) + pickedSeconds.value
        if (totalSecs <= 0) return

        totalDurationMillis = totalSecs * 1000L
        _totalMillis.value = totalDurationMillis
        remainingMillisAtPause = totalDurationMillis
        targetEpochMillis = SystemClock.elapsedRealtime() + totalDurationMillis

        _state.value = TimerState.RUNNING
        launchCountdown()
    }

    fun pause() {
        if (_state.value != TimerState.RUNNING) return
        timerJob?.cancel()
        remainingMillisAtPause = (targetEpochMillis - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        _remainingMillis.value = remainingMillisAtPause
        _state.value = TimerState.PAUSED
        updateNotification("Timer Paused", formatTime(remainingMillisAtPause))
    }

    fun resume() {
        if (_state.value != TimerState.PAUSED) return
        targetEpochMillis = SystemClock.elapsedRealtime() + remainingMillisAtPause
        _state.value = TimerState.RUNNING
        launchCountdown()
    }

    fun cancel() {
        timerJob?.cancel()
        _state.value = TimerState.IDLE
        _remainingMillis.value = 0L
        _totalMillis.value = 0L
        cancelNotification()
    }

    private fun launchCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _state.value == TimerState.RUNNING) {
                val current = SystemClock.elapsedRealtime()
                val left = targetEpochMillis - current
                if (left <= 0) {
                    _remainingMillis.value = 0L
                    _state.value = TimerState.FINISHED
                    onTimerFinished()
                    break
                } else {
                    _remainingMillis.value = left
                    updateNotification("Timer Running", formatTime(left))
                    delay(200)
                }
            }
        }
    }

    private fun onTimerFinished() {
        cancelNotification()
        val prefs = PreferencesManager(appContext)
        val soundName = prefs.settings.value.timerSound
        SoundPlayer.previewSound(soundName, 1.0f)

        // Vibrate
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(1000L)
            }
        } catch (_: Exception) {}

        // Post completion notification
        val openIntent = Intent(appContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            9001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, "hitech_timer_channel")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Timer Finished")
            .setContentText("Your countdown timer has ended.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(9002, notification)
    }

    private fun updateNotification(title: String, text: String) {
        val openIntent = Intent(appContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            9001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, "hitech_timer_channel")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(_state.value == TimerState.RUNNING)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(9001, notification)
    }

    private fun cancelNotification() {
        notificationManager.cancel(9001)
    }

    companion object {
        fun formatTime(millis: Long): String {
            val totalSeconds = (millis + 999) / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }
}
