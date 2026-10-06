package com.example.stopwatch

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class LapData(
    val lapIndex: Int,
    val lapTimeMillis: Long,
    val splitTimeMillis: Long
)

class StopwatchViewModel : ViewModel() {

    private var startTimestamp = 0L
    private var accumulatedMillis = 0L
    private var lastLapSplitMillis = 0L
    private var tickerJob: Job? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private val _laps = MutableStateFlow<List<LapData>>(emptyList())
    val laps: StateFlow<List<LapData>> = _laps.asStateFlow()

    fun start() {
        if (_isRunning.value) return
        startTimestamp = SystemClock.elapsedRealtime()
        _isRunning.value = true

        tickerJob = viewModelScope.launch {
            while (isActive && _isRunning.value) {
                val current = SystemClock.elapsedRealtime()
                _elapsedMillis.value = accumulatedMillis + (current - startTimestamp)
                delay(16) // ~60fps smooth refresh
            }
        }
    }

    fun pause() {
        if (!_isRunning.value) return
        tickerJob?.cancel()
        accumulatedMillis += (SystemClock.elapsedRealtime() - startTimestamp)
        _elapsedMillis.value = accumulatedMillis
        _isRunning.value = false
    }

    fun reset() {
        tickerJob?.cancel()
        _isRunning.value = false
        startTimestamp = 0L
        accumulatedMillis = 0L
        lastLapSplitMillis = 0L
        _elapsedMillis.value = 0L
        _laps.value = emptyList()
    }

    fun lap() {
        val currentSplit = if (_isRunning.value) {
            accumulatedMillis + (SystemClock.elapsedRealtime() - startTimestamp)
        } else {
            accumulatedMillis
        }
        if (currentSplit == 0L) return

        val lapTime = currentSplit - lastLapSplitMillis
        lastLapSplitMillis = currentSplit

        val newLap = LapData(
            lapIndex = _laps.value.size + 1,
            lapTimeMillis = lapTime,
            splitTimeMillis = currentSplit
        )
        // Add to front of list (newest lap on top)
        _laps.value = listOf(newLap) + _laps.value
    }

    companion object {
        fun formatTime(millis: Long): String {
            val minutes = (millis / 60000) % 60
            val seconds = (millis / 1000) % 60
            val hundredths = (millis % 1000) / 10
            return String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
        }
    }
}
