package com.example.alarm.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AlarmModel
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.InterfaceStyle
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalInterfaceStyle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.LocalHapticManager

/**
 * FEATURE 1: SMART ALARM DISABLE OPTIONS
 * Premium theme-adaptive bottom sheet prompting the user when disabling an alarm:
 * 1. "Only for tomorrow" (Temporarily skips the next scheduled occurrence)
 * 2. "Until I turn it on again" (Permanently disables until re-enabled)
 * Includes Cancel button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAlarmTurnOffBottomSheet(
    alarm: AlarmModel,
    is24Hour: Boolean = false,
    onSkipTomorrow: () -> Unit,
    onTurnOffPermanently: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticManager.current
    val accentColor = LocalAccentColor.current.primary
    val currentStyle = LocalInterfaceStyle.current

    val containerBg = when (currentStyle) {
        InterfaceStyle.MINIMAL -> Color(0xFF141416)
        InterfaceStyle.PURE_LIQUID_GLASS -> Color(0xFF090B14).copy(alpha = 0.94f)
        InterfaceStyle.MIX_LIQUID -> Color(0xFF12131A).copy(alpha = 0.96f)
    }

    val containerBorder = when (currentStyle) {
        InterfaceStyle.MINIMAL -> Color(0xFF2C2C2E)
        InterfaceStyle.PURE_LIQUID_GLASS -> Color.White.copy(alpha = 0.28f)
        InterfaceStyle.MIX_LIQUID -> GlassBorderBright
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = containerBg,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Alarm Time & Label Preview Pill
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${alarm.getFormattedTime(is24Hour)} ${if (!is24Hour) alarm.getAmPm() else ""} • ${alarm.label}",
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sheet Title
            Text(
                text = "Turn off alarm?",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // OPTION 1: "Only for tomorrow"
            SmartOptionCard(
                title = "Only for tomorrow",
                subtitle = "Your alarm will automatically turn back on.",
                icon = Icons.Default.CalendarToday,
                accentColor = accentColor,
                currentStyle = currentStyle,
                onClick = {
                    haptic.confirm()
                    onSkipTomorrow()
                },
                modifier = Modifier.testTag("option_only_for_tomorrow")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // OPTION 2: "Until I turn it on again"
            SmartOptionCard(
                title = "Until I turn it on again",
                subtitle = "Keep this alarm disabled until you enable it.",
                icon = Icons.Default.PauseCircle,
                accentColor = Color(0xFFA5A5AA),
                currentStyle = currentStyle,
                onClick = {
                    haptic.buttonClick()
                    onTurnOffPermanently()
                },
                modifier = Modifier.testTag("option_until_turn_on_again")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Cancel Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        when (currentStyle) {
                            InterfaceStyle.MINIMAL -> Color(0xFF1E1E22)
                            InterfaceStyle.PURE_LIQUID_GLASS -> Color.White.copy(alpha = 0.08f)
                            InterfaceStyle.MIX_LIQUID -> GlassSurface
                        }
                    )
                    .border(
                        1.dp,
                        if (currentStyle == InterfaceStyle.MINIMAL) Color(0xFF2C2C2E) else GlassBorder,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable {
                        haptic.buttonClick()
                        onDismiss()
                    }
                    .testTag("smart_turn_off_cancel_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    color = TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SmartOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    currentStyle: InterfaceStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .then(
                when (currentStyle) {
                    InterfaceStyle.PURE_LIQUID_GLASS -> Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.16f),
                                    accentColor.copy(alpha = 0.08f),
                                    Color(0xFF10121C).copy(alpha = 0.85f)
                                )
                            )
                        )
                        .border(
                            1.2.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.60f),
                                    accentColor.copy(alpha = 0.35f)
                                )
                            ),
                            cardShape
                        )

                    InterfaceStyle.MINIMAL -> Modifier
                        .background(Color(0xFF1C1C1E))
                        .border(1.dp, Color(0xFF2C2C2E), cardShape)

                    InterfaceStyle.MIX_LIQUID -> Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF22242E).copy(alpha = 0.94f),
                                    Color(0xFF15161E).copy(alpha = 0.96f)
                                )
                            )
                        )
                        .border(1.dp, GlassBorderBright, cardShape)
                }
            )
            .clickable(onClick = onClick)
            .padding(all = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Option Icon Pill
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f))
                    .border(1.dp, accentColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (currentStyle == InterfaceStyle.MINIMAL && accentColor == Color(0xFFA5A5AA)) Color.White else accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
