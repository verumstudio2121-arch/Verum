package com.example.alarm

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.alarm.ui.MathChallengeDismissCard
import com.example.data.AlarmRepository
import com.example.data.PreferencesManager
import com.example.ui.background.LoopingVideoBackground
import com.example.ui.theme.AccentColorTheme
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.HiTechClockTheme
import com.example.ui.theme.InterfaceStyle
import com.example.ui.theme.LiquidDismissSlider
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.LocalHapticManager
import com.example.util.rememberHapticManager
import kotlinx.coroutines.delay
import java.io.File
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

        val repo = AlarmRepository(this)
        val alarm = repo.getAlarm(alarmId) ?: repo.alarms.value.firstOrNull { it.backgroundType != "DEFAULT" }
        val backgroundType = intent.getStringExtra("alarm_bg_type") ?: alarm?.backgroundType ?: "DEFAULT"
        val backgroundMediaUri = intent.getStringExtra("alarm_bg_uri") ?: alarm?.backgroundMediaUri

        val prefs = PreferencesManager(this)
        val currentSettings = prefs.settings.value
        val initialSnoozeMinutes = currentSettings.snoozeDurationMinutes.coerceAtLeast(5)
        val accentTheme = AccentColorTheme.fromId(currentSettings.accentColor)
        val interfaceStyle = InterfaceStyle.fromId(currentSettings.interfaceStyle)
        val is24Hour = currentSettings.clockFormat == "24 hour"
        val mathChallengeEnabled = currentSettings.mathChallengeEnabled
        val mathChallengeDifficulty = currentSettings.mathChallengeDifficulty
        val mathChallengeCount = currentSettings.mathChallengeCount

        setContent {
            HiTechClockTheme(
                darkTheme = true,
                accentTheme = accentTheme,
                interfaceStyle = interfaceStyle
            ) {
                val hapticManager = rememberHapticManager()
                CompositionLocalProvider(LocalHapticManager provides hapticManager) {
                    AlarmRingingScreen(
                        label = label,
                        snoozeEnabled = snoozeEnabled,
                        initialSnoozeMinutes = initialSnoozeMinutes,
                        is24Hour = is24Hour,
                        backgroundType = backgroundType,
                        backgroundMediaUri = backgroundMediaUri,
                        mathChallengeEnabled = mathChallengeEnabled,
                        mathChallengeDifficulty = mathChallengeDifficulty,
                        mathChallengeCount = mathChallengeCount,
                        onSnooze = { minutes ->
                            AlarmService.stopAlarm(this)
                            AlarmScheduler(this).scheduleSnooze(alarmId, label, sound, vibrate, minutes)
                            finish()
                        },
                        onDismiss = {
                            AlarmService.stopAlarm(this)
                            val currentAlarm = repo.getAlarm(alarmId)
                            if (currentAlarm != null) {
                                if (currentAlarm.repeatType == "Once") {
                                    repo.toggleAlarm(alarmId, false)
                                } else {
                                    repo.updateAlarm(currentAlarm)
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
    is24Hour: Boolean = false,
    backgroundType: String = "DEFAULT",
    backgroundMediaUri: String? = null,
    mathChallengeEnabled: Boolean = false,
    mathChallengeDifficulty: String = "Medium",
    mathChallengeCount: Int = 1,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    var snoozeMinutes by remember { mutableIntStateOf(initialSnoozeMinutes) }
    val haptic = LocalHapticManager.current
    val accentColor = LocalAccentColor.current.primary

    // Prevent bypassing the Math Challenge via system Back gesture/button
    BackHandler(enabled = mathChallengeEnabled) {
        haptic.reject()
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1000)
        }
    }

    val timeString = currentTime.format(
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm")
    )
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
        // FEATURE 2: VIDEO / PHOTO BACKGROUND
        val mediaFile = remember(backgroundMediaUri) {
            backgroundMediaUri?.let { File(it) }
        }
        val hasValidMedia = remember(backgroundMediaUri) {
            if (backgroundMediaUri.isNullOrEmpty()) false
            else if (backgroundMediaUri.startsWith("content://")) true
            else File(backgroundMediaUri).exists()
        }

        if (backgroundType == "VIDEO" && hasValidMedia) {
            // Looping, muted video covering entire screen with Center-Crop
            LoopingVideoBackground(
                videoPathOrUri = backgroundMediaUri!!,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle dark overlay behind text for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.52f))
            )
        } else if (backgroundType == "PHOTO" && hasValidMedia) {
            // Custom photo background
            AsyncImage(
                model = if (backgroundMediaUri!!.startsWith("content://")) android.net.Uri.parse(backgroundMediaUri) else mediaFile,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle dark overlay behind text for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.52f))
            )
        } else {
            // Default ambient glass radial glow
            Box(
                modifier = Modifier
                    .size(360.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                accentColor.copy(alpha = ambientPulse),
                                Color(0xFFA78BFA).copy(alpha = ambientPulse * 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    horizontal = 20.dp,
                    vertical = if (mathChallengeEnabled) 12.dp else 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top minimal indicator
            Text(
                text = "ALARM",
                color = TextSecondary.copy(alpha = 0.65f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(top = if (mathChallengeEnabled) 6.dp else 24.dp)
            )

            // Center: Large Time, AM/PM, and Label (compact when Math Challenge keypad is visible)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (mathChallengeEnabled) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = timeString,
                            color = TextPrimary,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-1.5).sp
                        )
                        if (!is24Hour) {
                            Text(
                                text = " $amPmString",
                                color = TextSecondary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    Text(
                        text = timeString,
                        color = TextPrimary,
                        fontSize = 82.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-2).sp
                    )
                    if (!is24Hour) {
                        Text(
                            text = amPmString,
                            color = TextSecondary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 2.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // Label capsule
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(GlassSurfaceElevated)
                        .border(1.dp, GlassBorderBright, CircleShape)
                        .padding(
                            horizontal = if (mathChallengeEnabled) 20.dp else 28.dp,
                            vertical = if (mathChallengeEnabled) 6.dp else 10.dp
                        )
                ) {
                    Text(
                        text = label,
                        color = TextPrimary,
                        fontSize = if (mathChallengeEnabled) 16.sp else 20.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Bottom controls: Free SNOOZE capsule + (Math Challenge Card OR Slide to Dismiss)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (snoozeEnabled) {
                    // Tactile Liquid Glass Snooze Capsule with Minus, Snooze Action, and Plus (Freely usable)
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, accentColor.copy(alpha = 0.45f), CircleShape)
                            .padding(horizontal = 6.dp, vertical = 5.dp),
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
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (snoozeMinutes > 5) Color.White.copy(alpha = 0.12f) else Color.Transparent
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease Snooze",
                                tint = if (snoozeMinutes > 5) accentColor else Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(20.dp)
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
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("alarm_snooze_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "SNOOZE",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.5.sp
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.25f))
                                        .border(1.dp, accentColor.copy(alpha = 0.60f), CircleShape)
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
                                            color = accentColor,
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
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (snoozeMinutes < 60) Color.White.copy(alpha = 0.12f) else Color.Transparent
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase Snooze",
                                tint = if (snoozeMinutes < 60) accentColor else Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (mathChallengeEnabled) 14.dp else 24.dp))
                }

                if (mathChallengeEnabled) {
                    MathChallengeDismissCard(
                        difficulty = mathChallengeDifficulty,
                        problemCount = mathChallengeCount,
                        onSolved = onDismiss
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
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
}
