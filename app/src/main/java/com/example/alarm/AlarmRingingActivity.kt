package com.example.alarm

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AlarmRepository
import com.example.data.PreferencesManager
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.HiTechClockTheme
import com.example.ui.theme.LiquidDismissSlider
import com.example.ui.theme.LiquidGlassButton
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.LocalHapticManager
import com.example.util.rememberHapticManager
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmRingingActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_ALARM_SOUND = "alarm_sound"
        const val EXTRA_ALARM_VIBRATE = "alarm_vibrate"
        const val EXTRA_ALARM_SNOOZE_ENABLED = "alarm_snooze_enabled"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenWindow()

        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: "default_alarm"
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Wake Up"
        val sound = intent.getStringExtra(EXTRA_ALARM_SOUND) ?: "Gentle"
        val vibrate = intent.getBooleanExtra(EXTRA_ALARM_VIBRATE, true)
        val snoozeEnabled = intent.getBooleanExtra(EXTRA_ALARM_SNOOZE_ENABLED, true)

        val prefs = PreferencesManager(this)
        val initialSnoozeMinutes = prefs.settings.value.snoozeDurationMinutes.coerceAtLeast(5)

        setContent {
            HiTechClockTheme(darkTheme = true) {
                val hapticManager = rememberHapticManager()
                CompositionLocalProvider(LocalHapticManager provides hapticManager) {
                    AlarmRingingScreen(
                        label = label,
                        snoozeEnabled = snoozeEnabled,
                        initialSnoozeMinutes = initialSnoozeMinutes,
                        onSnooze = { minutes ->
                            AlarmService.stopAlarm(this)
                            AlarmScheduler(this).scheduleSnooze(alarmId, label, sound, vibrate, minutes)
                            finish()
                        },
                        onDismiss = {
                            AlarmService.stopAlarm(this)
                            val repo = AlarmRepository(this)
                            val alarm = repo.getAlarm(alarmId)
                            if (alarm != null) {
                                if (alarm.repeatType == "Once") {
                                    repo.toggleAlarm(alarmId, false)
                                } else {
                                    repo.updateAlarm(alarm)
                                }
                            }
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun configureLockScreenWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        AlarmService.stopAlarm(this)
        super.onDestroy()
    }
}

@Composable
fun AlarmRingingScreen(
    label: String,
    snoozeEnabled: Boolean,
    initialSnoozeMinutes: Int = 5,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    var snoozeMinutes by remember { mutableIntStateOf(initialSnoozeMinutes) }
    val haptic = LocalHapticManager.current

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1000)
        }
    }

    val timeString = currentTime.format(DateTimeFormatter.ofPattern("h:mm"))
    val amPmString = currentTime.format(DateTimeFormatter.ofPattern("a")).uppercase()

    // Subtle pulsing ambient lighting
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glass radial glow
        Box(
            modifier = Modifier
                .size(360.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF38BDF8).copy(alpha = ambientPulse),
                            Color(0xFFA78BFA).copy(alpha = ambientPulse * 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top minimal indicator
            Text(
                text = "ALARM",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(top = 24.dp)
            )

            // Center: Large Time, AM/PM, and Label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = timeString,
                    color = TextPrimary,
                    fontSize = 82.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = amPmString,
                    color = TextSecondary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Label capsule
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(GlassSurfaceElevated)
                        .border(1.dp, GlassBorderBright, CircleShape)
                        .padding(horizontal = 28.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = label,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Bottom controls: SNOOZE with [-] and [+] and Slide to Dismiss
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (snoozeEnabled) {
                    // Tactile Liquid Glass Snooze Capsule with Minus, Snooze Action, and Plus
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.40f), CircleShape)
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Minus Button [-]
                        IconButton(
                            onClick = {
                                if (snoozeMinutes > 5) {
                                    haptic.sliderTick()
                                    snoozeMinutes -= 5
                                }
                            },
                            enabled = snoozeMinutes > 5,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (snoozeMinutes > 5) Color.White.copy(alpha = 0.12f) else Color.Transparent
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease Snooze",
                                tint = if (snoozeMinutes > 5) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Center Snooze Button
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    haptic.confirm()
                                    onSnooze(snoozeMinutes)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "SNOOZE",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.5.sp
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.25f))
                                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.60f), CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    AnimatedContent(
                                        targetState = snoozeMinutes,
                                        transitionSpec = {
                                            if (targetState > initialState) {
                                                (slideInVertically { height -> height } + fadeIn()).togetherWith(
                                                    slideOutVertically { height -> -height } + fadeOut()
                                                )
                                            } else {
                                                (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                                                    slideOutVertically { height -> height } + fadeOut()
                                                )
                                            }
                                        },
                                        label = "snooze_mins_anim"
                                    ) { mins ->
                                        Text(
                                            text = "$mins min",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Plus Button [+]
                        IconButton(
                            onClick = {
                                if (snoozeMinutes < 60) {
                                    haptic.sliderTick()
                                    snoozeMinutes += 5
                                }
                            },
                            enabled = snoozeMinutes < 60,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (snoozeMinutes < 60) Color.White.copy(alpha = 0.12f) else Color.Transparent
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase Snooze",
                                tint = if (snoozeMinutes < 60) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Physics-based Slide to Dismiss Slider
                LiquidDismissSlider(
                    modifier = Modifier.fillMaxWidth(),
                    onDismiss = onDismiss
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
