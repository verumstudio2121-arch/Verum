package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
        // High-tech dual-tone beep beep (0.75 second pattern)
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
        // Warm harmonic ascending arpeggio C5 (523Hz), E5 (659Hz), G5 (784Hz), B5 (987Hz)
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
                    // Add subtle 2nd harmonic
                    sample += sin(2.0 * PI * freq * 2.0 * dt) * decay * 0.15
                }
            }
            buffer[i] = (sample * 26000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateDigitalPulse(sampleRate: Int): ShortArray {
        // Futuristic cyber pulse sequence (1.0 sec)
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
        // Uplifting melody (1.5 sec)
        val duration = (1.6 * sampleRate).toInt()
        val buffer = ShortArray(duration)
        val notes = doubleArrayOf(587.33, 739.99, 880.0, 1174.66) // D5, F#5, A5, D6
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
        // Resonant acoustic bell (1.8 sec)
        val duration = (1.8 * sampleRate).toInt()
        val buffer = ShortArray(duration)
        val baseFreq = 659.25 // E5

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
        // Rhythmic pulsing heartbeat alert (1.0 sec)
        val duration = (1.0 * sampleRate).toInt()
        val buffer = ShortArray(duration)
        val freq = 480.0

        for (i in 0 until duration) {
            val t = i.toDouble() / sampleRate
            val mod = (0.5 * (1.0 + sin(2.0 * PI * 4.0 * t))) // 4Hz pulse modulation
            val wave = sin(2.0 * PI * freq * t)
            buffer[i] = (wave * mod * 25000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    /**
     * Previews the selected sound for ~2 seconds then stops.
     */
    fun previewSound(soundName: String, volume: Float = 1.0f) {
        stopPreview()
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

    fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
    }

    /**
     * Starts looping alarm sound for continuous ringing in AlarmService.
     */
    fun startAlarmLoop(soundName: String, volume: Float = 1.0f) {
        stopAlarmLoop()
        isAlarmLooping = true

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
                track.setLoopPoints(0, samples.size, -1) // Loop infinitely
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

    fun stopAlarmLoop() {
        isAlarmLooping = false
        alarmLoopJob?.cancel()
        alarmLoopJob = null
        try {
            activeTrack?.stop()
            activeTrack?.release()
        } catch (_: Exception) {}
        activeTrack = null
    }
}
