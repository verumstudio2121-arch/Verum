package com.example

import com.example.data.AlarmModel
import com.example.stopwatch.StopwatchViewModel
import com.example.timer.TimerViewModel
import com.example.ui.theme.liquidGlass
import com.example.worldclock.WorldCity
import androidx.compose.ui.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

  @Test
  fun testAlarmModelSerialization() {
    val alarm = AlarmModel(
      id = "test-uuid",
      hour = 7,
      minute = 30,
      label = "School",
      isEnabled = true,
      repeatType = "Monday-Friday",
      soundName = "Gentle",
      vibrate = true,
      snoozeEnabled = true
    )

    val json = alarm.toJson()
    val restored = AlarmModel.fromJson(json)

    assertEquals(alarm.id, restored.id)
    assertEquals(alarm.hour, restored.hour)
    assertEquals(alarm.minute, restored.minute)
    assertEquals(alarm.label, restored.label)
    assertEquals(alarm.repeatType, restored.repeatType)
    assertEquals(alarm.soundName, restored.soundName)
    assertEquals("7:30", restored.getFormattedTime(false))
    assertEquals("07:30", restored.getFormattedTime(true))
    assertEquals("AM", restored.getAmPm())
  }

  @Test
  fun testStopwatchFormatTime() {
    assertEquals("00:00.00", StopwatchViewModel.formatTime(0L))
    assertEquals("01:23.45", StopwatchViewModel.formatTime(83456L))
  }

  @Test
  fun testTimerFormatTime() {
    assertEquals("05:00", TimerViewModel.formatTime(300000L))
    assertEquals("01:00:00", TimerViewModel.formatTime(3600000L))
  }

  @Test
  fun testWorldCityTimezone() {
    val tokyo = WorldCity(
      name = "Tokyo",
      country = "Japan",
      timezoneId = "Asia/Tokyo"
    )
    val formatted = tokyo.getFormattedTime(false)
    assertTrue(formatted.contains(":"))
    val amPm = tokyo.getAmPm()
    assertTrue(amPm == "AM" || amPm == "PM")
  }

  @Test
  fun testLiquidGlassModifierCreation() {
    val modifier = Modifier.liquidGlass()
    org.junit.Assert.assertNotNull(modifier)
  }
}
