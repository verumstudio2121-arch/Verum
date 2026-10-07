package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SoundPlayer {
    val AVAILABLE_SOUNDS = listOf(
        "Gentle",
        "Classic",
        "Digital",
        "Wake Up",
        "Bell",
        "Pulse"
    )

    private var previewJob: Job? = null
    private var activeTrack: AudioTrack? = null
    private var previewMediaPlayer: MediaPlayer? = null
    private var alarmMediaPlayer: MediaPlayer? = null
    private var isAlarmLooping = false
    private var alarmLoopJob: Job? = null

    /**
     * Synthesizes audio samples for each sound preset.
     * Returns 16-bit PCM mono samples at 44100 Hz.
     */
    fun generateToneSamples(soundName: String, sampleRate: Int = 44100): ShortArray {
        return when (soundName) {
            "Classic" -> generateClassicAlarm(sampleRate)
            "Gentle" -> generateGentleChime(sampleRate)
            "Digital" -> generateDigitalPulse(sampleRate)
            "Wake Up" -> generateWakeUpMelody(sampleRate)
            "Bell" -> generateBellTone(sampleRate)
            "Pulse" -> generatePulsingAlert(sampleRate)
            else -> generateGentleChime(sampleRate)
        }
    }

    private fun generateClassicAlarm(sampleRate: Int): ShortArray {
        val duration = (0.75 * sampleRate).toInt()
        val buffer = ShortArray(duration)
        val f1 = 880.0
        val f2 = 1174.0

        for (i in 0 until duration) {
            val t = i.toDouble() / sampleRate
            val envelope = when {
                t < 0.15 -> 1.0
                t < 0.22 -> 0.0
                t < 0.37 -> 1.0
                t < 0.44 -> 0.0
                t < 0.59 -> 1.0
                else -> 0.0
            }
            val wave = 0.6 * sin(2.0 * PI * f1 * t) + 0.4 * sin(2.0 * PI * f2 * t)
            buffer[i] = (wave * envelope * 24000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateGentleChime(sampleRate: Int): ShortArray {
        val duration = (1.8 * sampleRate).toInt()
        val buffer = ShortArray(duration)
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 987.77)
        val noteDur = 0.35

        for (i in 0 until duration) {
            val t = i.toDouble() / sampleRate
            var sample = 0.0
            for ((idx, freq) in notes.withIndex()) {
                val start = idx * noteDur
                if (t >= start) {
                    val dt = t - start
                    val decay = exp(-dt * 3.5)
                    sample += sin(2.0 * PI * freq * dt) * decay * 0.45
                    sample += sin(2.0 * PI * freq * 2.0 * dt) * decay * 0.15
                }
            }
            buffer[i] = (sample * 26000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateDigitalPulse(sampleRate: Int): ShortArray {
        val duration = (1.0 * sampleRate).toInt()
        val buffer = ShortArray(duration)
        val freqs = doubleArrayOf(1046.5, 1318.5, 1567.98, 2093.0)

        for (i in 0 until duration) {
            val t = i.toDouble() / sampleRate
            val step = (t * 8).toInt() % 4
            val stepTime = (t * 8) - (t * 8).toInt()
            val freq = freqs[step]
            val env = (1.0 - stepTime).coerceAtLeast(0.0)
            val wave = sin(2.0 * PI * freq * t) + 0.3 * sin(2.0 * PI * freq * 3.0 * t)
            buffer[i] = (wave * env * 22000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateWakeUpMelody(sampleRate: Int): ShortArray {
        val duration = (1.6 * sampleRate).toInt()
        val buffer = ShortArray(duration)
        val notes = doubleArrayOf(587.33, 739.99, 880.0, 1174.66)
        val noteInterval = 0.28

        for (i in 0 until duration) {
            val t = i.toDouble() / sampleRate
            var sample = 0.0
            for ((idx, freq) in notes.withIndex()) {
                val start = idx * noteInterval
                if (t >= start) {
                    val dt = t - start
                    val decay = exp(-dt * 3.0)
                    sample += sin(2.0 * PI * freq * dt) * decay * 0.5
                }
            }
            buffer[i] = (sample * 25000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateBellTone(sampleRate: Int): ShortArray {
        val duration = (1.8 * sampleRate).toInt()
        val buffer = ShortArray(duration)
        val baseFreq = 659.25

        for (i in 0 until duration) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 2.8)
            val wave = 0.5 * sin(2.0 * PI * baseFreq * t) +
                    0.25 * sin(2.0 * PI * (baseFreq * 2.02) * t) +
                    0.15 * sin(2.0 * PI * (baseFreq * 3.01) * t) +
                    0.1 * sin(2.0 * PI * (baseFreq * 4.2) * t)
            buffer[i] = (wave * decay * 28000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generatePulsingAlert(sampleRate: Int): ShortArray {
        val duration = (1.0 * sampleRate).toInt()
        val buffer = ShortArray(duration)
        val freq = 480.0

        for (i in 0 until duration) {
            val t = i.toDouble() / sampleRate
            val mod = (0.5 * (1.0 + sin(2.0 * PI * 4.0 * t)))
            val wave = sin(2.0 * PI * freq * t)
            buffer[i] = (wave * mod * 25000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    /**
     * Resolves whether a sound represents a custom file path or custom ringtone.
     */
    fun isCustomSound(context: Context?, soundName: String): Boolean {
        if (soundName.startsWith("/") || soundName.startsWith("file://") || soundName.startsWith("content://")) {
            return true
        }
        if (context != null) {
            val ringtone = CustomRingtoneManager(context).findRingtone(soundName)
            if (ringtone != null) return true
        }
        return false
    }

    /**
     * Previews the selected sound (synthesized preset or custom file) for ~4 seconds.
     */
    fun previewSound(context: Context? = null, soundName: String, volume: Float = 1.0f) {
        stopPreview()

        // Check if custom ringtone audio file
        val customFile = resolveCustomAudioFile(context, soundName)
        if (customFile != null && customFile.exists()) {
            previewCustomAudioFile(customFile, volume)
            return
        }

        previewJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val sampleRate = 44100
                val samples = generateToneSamples(soundName, sampleRate)
                val bufferSize = samples.size * 2

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.setVolume(volume.coerceIn(0f, 1f))
                track.play()

                delay(2200)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            } catch (_: Exception) {}
        }
    }

    private fun previewCustomAudioFile(file: File, volume: Float) {
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(file.absolutePath)
                setVolume(volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f))
                prepare()
                start()
            }
            previewMediaPlayer = mp

            previewJob = CoroutineScope(Dispatchers.IO).launch {
                delay(4000)
                stopPreview()
            }
        } catch (_: Exception) {}
    }

    fun stopPreview() {
        previewJob?.cancel()
        previewJob = null

        try {
            previewMediaPlayer?.stop()
            previewMediaPlayer?.release()
        } catch (_: Exception) {}
        previewMediaPlayer = null
    }

    /**
     * Starts looping alarm sound for continuous ringing in AlarmService.
     * Supports both synthesized presets and custom ringtone files.
     */
    fun startAlarmLoop(context: Context? = null, soundName: String, volume: Float = 1.0f) {
        stopAlarmLoop()
        isAlarmLooping = true

        val customFile = resolveCustomAudioFile(context, soundName)
        if (customFile != null && customFile.exists()) {
            startCustomAlarmLoop(customFile, volume)
            return
        }

        alarmLoopJob = CoroutineScope(Dispatchers.IO).launch {
            val sampleRate = 44100
            val samples = generateToneSamples(soundName, sampleRate)
            val bufferSize = samples.size * 2

            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                activeTrack = track
                track.write(samples, 0, samples.size)
                track.setLoopPoints(0, samples.size, -1)
                track.setVolume(volume.coerceIn(0f, 1f))
                track.play()

                while (isActive && isAlarmLooping) {
                    delay(500)
                }
            } catch (_: Exception) {
            } finally {
                try {
                    activeTrack?.stop()
                    activeTrack?.release()
                } catch (_: Exception) {}
                activeTrack = null
            }
        }
    }

    private fun startCustomAlarmLoop(file: File, volume: Float) {
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(file.absolutePath)
                isLooping = true
                setVolume(volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f))
                prepare()
                start()
            }
            alarmMediaPlayer = mp
        } catch (e: Exception) {
            // Fallback to synthesized gentle chime if playback fails
            startAlarmLoop(null, "Gentle", volume)
        }
    }

    fun stopAlarmLoop() {
        isAlarmLooping = false
        alarmLoopJob?.cancel()
        alarmLoopJob = null

        try {
            activeTrack?.stop()
            activeTrack?.release()
        } catch (_: Exception) {}
        activeTrack = null

        try {
            alarmMediaPlayer?.stop()
            alarmMediaPlayer?.release()
        } catch (_: Exception) {}
        alarmMediaPlayer = null
    }

    private fun resolveCustomAudioFile(context: Context?, soundName: String): File? {
        val directFile = File(soundName)
        if (directFile.exists()) return directFile

        if (context != null) {
            val ringtone = CustomRingtoneManager(context).findRingtone(soundName)
            if (ringtone != null) {
                val f = File(ringtone.filePath)
                if (f.exists()) return f
            }
        }
        return null
    }
}
