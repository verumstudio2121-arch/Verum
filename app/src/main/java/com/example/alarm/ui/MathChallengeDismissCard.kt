package com.example.alarm.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.MathChallengeGenerator
import com.example.alarm.MathDifficulty
import com.example.ui.theme.AccentRed
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.InterfaceStyle
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalInterfaceStyle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.LocalHapticManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Themed Math Challenge Dismissal Card with inline tactile numeric keypad.
 * Adapts to Pure Liquid Glass, Pure Minimal, and Mix Liquid Glass themes.
 */
@Composable
fun MathChallengeDismissCard(
    difficulty: String,
    problemCount: Int,
    onSolved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticManager.current
    val accentColor = LocalAccentColor.current.primary
    val currentStyle = LocalInterfaceStyle.current
    val scope = rememberCoroutineScope()

    val safeCount = problemCount.coerceIn(1, 3)
    val problems = remember(difficulty, safeCount) {
        MathChallengeGenerator.generateProblems(difficulty, safeCount)
    }

    var currentIndex by remember(problems) { mutableIntStateOf(0) }
    var userInput by remember(problems) { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(isError) {
        if (isError) {
            delay(900)
            isError = false
        }
    }

    val currentProblem = problems[currentIndex.coerceIn(0, problems.lastIndex)]
    val difficultyLabel = MathDifficulty.fromId(difficulty).label

    fun triggerWrongAnswer() {
        haptic.reject()
        isError = true
        userInput = ""
        scope.launch {
            shakeOffset.snapTo(0f)
            val offsets = listOf(-26f, 26f, -18f, 18f, -10f, 10f, 0f)
            for (target in offsets) {
                shakeOffset.animateTo(target, tween(durationMillis = 42))
            }
        }
    }

    fun submitAnswer() {
        if (userInput.isEmpty()) {
            triggerWrongAnswer()
            return
        }
        val parsed = userInput.toIntOrNull()
        if (parsed == currentProblem.answer) {
            haptic.confirm()
            isError = false
            if (currentIndex + 1 >= problems.size) {
                onSolved()
            } else {
                userInput = ""
                currentIndex += 1
            }
        } else {
            triggerWrongAnswer()
        }
    }

    fun appendDigit(digit: Int) {
        if (userInput.length < 5) {
            haptic.buttonClick()
            isError = false
            userInput += digit.toString()
        }
    }

    fun removeLastDigit() {
        if (userInput.isNotEmpty()) {
            haptic.buttonClick()
            isError = false
            userInput = userInput.dropLast(1)
        }
    }

    val cardShape = RoundedCornerShape(28.dp)
    val cardModifier = modifier
        .fillMaxWidth()
        .widthIn(max = 460.dp)
        .clip(cardShape)
        .then(
            when (currentStyle) {
                InterfaceStyle.PURE_LIQUID_GLASS -> Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.16f),
                                accentColor.copy(alpha = 0.08f),
                                Color(0xFF0C0F1A).copy(alpha = 0.86f)
                            )
                        )
                    )
                    .border(
                        1.2.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.65f),
                                accentColor.copy(alpha = 0.40f)
                            )
                        ),
                        cardShape
                    )
                InterfaceStyle.MINIMAL -> Modifier
                    .background(Color(0xFF18181B).copy(alpha = 0.94f))
                    .border(1.dp, Color(0xFF2E2E32), cardShape)
                InterfaceStyle.MIX_LIQUID -> Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1C1E27).copy(alpha = 0.92f),
                                Color(0xFF101218).copy(alpha = 0.95f)
                            )
                        )
                    )
                    .border(1.dp, GlassBorderBright, cardShape)
            }
        )
        .padding(horizontal = 18.dp, vertical = 16.dp)
        .testTag("math_challenge_dismiss_card")

    val answerBorderColor by animateColorAsState(
        targetValue = when {
            isError -> AccentRed
            userInput.isNotEmpty() -> accentColor
            currentStyle == InterfaceStyle.MINIMAL -> Color(0xFF3A3A3C)
            else -> GlassBorderBright
        },
        label = "math_answer_border"
    )

    Column(
        modifier = cardModifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: Title + Progress Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SOLVE TO DISMISS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.6.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.18f))
                        .border(1.dp, accentColor.copy(alpha = 0.45f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (problems.size > 1) {
                            "${currentIndex + 1} / ${problems.size} · $difficultyLabel"
                        } else {
                            difficultyLabel
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Equation + Answer Display Row (with shake animation on wrong answer)
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                (slideInHorizontally { it / 2 } + fadeIn(tween(200))).togetherWith(
                    slideOutHorizontally { -it / 2 } + fadeOut(tween(150))
                )
            },
            label = "math_problem_transition"
        ) { problemIdx ->
            val shownProblem = problems[problemIdx.coerceIn(0, problems.lastIndex)]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${shownProblem.expression} =",
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.testTag("math_challenge_equation")
                )

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .widthIn(min = 84.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when {
                                isError -> AccentRed.copy(alpha = 0.20f)
                                currentStyle == InterfaceStyle.MINIMAL -> Color(0xFF27272A)
                                else -> Color.White.copy(alpha = 0.12f)
                            }
                        )
                        .border(1.5.dp, answerBorderColor, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("math_challenge_answer_display"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInput.ifEmpty { "?" },
                        color = when {
                            isError -> AccentRed
                            userInput.isEmpty() -> TextSecondary.copy(alpha = 0.55f)
                            else -> Color.White
                        },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Fixed-height subtle hint / error feedback slot so keypad never jumps vertically
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isError) {
                Text(
                    text = "Incorrect answer — try again",
                    color = AccentRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Inline Tactile 3x4 Numeric Keypad
        val digitRows = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            digitRows.forEach { rowDigits ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowDigits.forEach { digit ->
                        MathKeyButton(
                            label = digit.toString(),
                            style = currentStyle,
                            accentColor = accentColor,
                            onClick = { appendDigit(digit) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("math_key_$digit")
                        )
                    }
                }
            }

            // Bottom Row: [Backspace] [0] [Submit ✓]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val actionKeyBg = when (currentStyle) {
                    InterfaceStyle.MINIMAL -> Color(0xFF27272A)
                    InterfaceStyle.PURE_LIQUID_GLASS -> Color.White.copy(alpha = 0.12f)
                    InterfaceStyle.MIX_LIQUID -> Color.White.copy(alpha = 0.09f)
                }
                val actionKeyBorder = when (currentStyle) {
                    InterfaceStyle.MINIMAL -> Color(0xFF3A3A3C)
                    else -> GlassBorder
                }

                // Backspace
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(actionKeyBg)
                        .border(1.dp, actionKeyBorder, RoundedCornerShape(15.dp))
                        .clickable { removeLastDigit() }
                        .testTag("math_key_backspace"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Zero
                MathKeyButton(
                    label = "0",
                    style = currentStyle,
                    accentColor = accentColor,
                    onClick = { appendDigit(0) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("math_key_0")
                )

                // Submit
                val hasInput = userInput.isNotEmpty()
                val submitBg by animateColorAsState(
                    targetValue = when {
                        hasInput && currentStyle == InterfaceStyle.MINIMAL -> accentColor
                        hasInput -> accentColor.copy(alpha = 0.85f)
                        else -> actionKeyBg
                    },
                    label = "math_submit_bg"
                )
                val submitBorder = if (hasInput) accentColor else actionKeyBorder

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(submitBg)
                        .border(1.dp, submitBorder, RoundedCornerShape(15.dp))
                        .clickable { submitAnswer() }
                        .testTag("math_key_submit"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Submit Answer",
                        tint = if (hasInput) Color.White else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MathKeyButton(
    label: String,
    style: InterfaceStyle,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(15.dp)
    val bgModifier = when (style) {
        InterfaceStyle.PURE_LIQUID_GLASS -> Modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.08f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
        InterfaceStyle.MINIMAL -> Modifier
            .background(Color(0xFF27272A))
            .border(1.dp, Color(0xFF3A3A3C), shape)
        InterfaceStyle.MIX_LIQUID -> Modifier
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, GlassBorder, shape)
    }

    Box(
        modifier = modifier
            .height(50.dp)
            .clip(shape)
            .then(bgModifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif
        )
    }
}
