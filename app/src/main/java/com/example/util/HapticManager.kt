package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Centralized HapticManager that leverages Android's [HapticFeedbackConstants]
 * to deliver subtle, tactile, and premium vibrations across UI interactions
 * such as button clicks, slider movements, switches, time pickers, and gestures.
 */
class HapticManager(
    private val view: View? = null,
    private val context: Context? = null
) {

    /**
     * Subtle, crisp click feedback for standard buttons, navigation tabs,
     * chips, and clickable cards.
     */
    fun buttonClick(): Boolean {
        return perform(HapticFeedbackConstants.KEYBOARD_TAP, fallbackVibrateMs = 8L)
    }

    /**
     * Ultra-subtle tick for smooth continuous slider dragging, time dial
     * scrolling, and wheel pickers.
     */
    fun sliderTick(): Boolean {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        return perform(constant, fallbackVibrateMs = 4L)
    }

    /**
     * Distinct milestone tick for slider snap points, step increments,
     * or reaching slider bounds.
     */
    fun sliderSnap(): Boolean {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        return perform(constant, fallbackVibrateMs = 12L)
    }

    /**
     * Tactile cue when starting a drag gesture (e.g. Slide to Dismiss).
     */
    fun sliderDragStart(): Boolean {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.GESTURE_START
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        return perform(constant, fallbackVibrateMs = 6L)
    }

    /**
     * Tactile cue when ending a drag gesture.
     */
    fun sliderDragEnd(): Boolean {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.GESTURE_END
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        return perform(constant, fallbackVibrateMs = 8L)
    }

    /**
     * Tactile switch feedback based on state.
     */
    fun toggle(isOn: Boolean): Boolean {
        return if (isOn) toggleOn() else toggleOff()
    }

    /**
     * Crisp tactile click when toggling a switch or option ON.
     */
    fun toggleOn(): Boolean {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.TOGGLE_ON
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        return perform(constant, fallbackVibrateMs = 10L)
    }

    /**
     * Soft tactile click when toggling a switch or option OFF.
     */
    fun toggleOff(): Boolean {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.TOGGLE_OFF
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        return perform(constant, fallbackVibrateMs = 8L)
    }

    /**
     * Premium confirmation haptic bump for successful actions
     * (e.g., dismissed alarm, saved alarm, started timer).
     */
    fun confirm(): Boolean {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        return perform(constant, fallbackVibrateMs = 16L)
    }

    /**
     * Tactile rejection buzz for invalid input, cancellation, or error.
     */
    fun reject(): Boolean {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        return perform(constant, fallbackVibrateMs = 35L)
    }

    /**
     * Long-press feedback for context actions, reordering, and item removal.
     */
    fun longPress(): Boolean {
        return perform(HapticFeedbackConstants.LONG_PRESS, fallbackVibrateMs = 30L)
    }

    /**
     * Distinct heavy click for prominent primary actions.
     */
    fun heavyClick(): Boolean {
        return perform(HapticFeedbackConstants.CONTEXT_CLICK, fallbackVibrateMs = 18L)
    }

    /**
     * Subtle clock tick for timepiece hand movement and second indicators.
     */
    fun clockTick(): Boolean {
        return perform(HapticFeedbackConstants.CLOCK_TICK, fallbackVibrateMs = 4L)
    }

    /**
     * Dispatches the specified [HapticFeedbackConstants] code with flags.
     */
    fun perform(feedbackConstant: Int, fallbackVibrateMs: Long = 10L): Boolean {
        var handled = false
        try {
            if (view != null) {
                handled = view.performHapticFeedback(
                    feedbackConstant,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                )
            }
        } catch (_: Exception) {
            handled = false
        }

        // Fallback to vibrator if View was unavailable or unhandled
        if (!handled && context != null) {
            handled = performVibratorFallback(feedbackConstant, fallbackVibrateMs)
        }

        return handled
    }

    private fun performVibratorFallback(feedbackConstant: Int, fallbackMs: Long): Boolean {
        return try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context?.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val effectId = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && feedbackConstant == HapticFeedbackConstants.CONFIRM ->
                            VibrationEffect.EFFECT_CLICK
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && feedbackConstant == HapticFeedbackConstants.REJECT ->
                            VibrationEffect.EFFECT_DOUBLE_CLICK
                        feedbackConstant == HapticFeedbackConstants.CLOCK_TICK ->
                            VibrationEffect.EFFECT_TICK
                        feedbackConstant == HapticFeedbackConstants.LONG_PRESS ->
                            VibrationEffect.EFFECT_HEAVY_CLICK
                        else ->
                            VibrationEffect.EFFECT_CLICK
                    }
                    vibrator.vibrate(VibrationEffect.createPredefined(effectId))
                    true
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(fallbackMs)
                    true
                }
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        /**
         * Convenience factory to create a [HapticManager] instance from an Android [View].
         */
        fun from(view: View): HapticManager {
            return HapticManager(view = view, context = view.context)
        }

        /**
         * Convenience factory to create a [HapticManager] instance from an Android [Context].
         */
        fun from(context: Context): HapticManager {
            return HapticManager(view = null, context = context)
        }
    }
}

/**
 * CompositionLocal providing access to the centralized [HapticManager].
 */
val LocalHapticManager = staticCompositionLocalOf<HapticManager> {
    HapticManager()
}

/**
 * Composable helper that creates and remembers a [HapticManager] instance
 * bound to the current Compose [LocalView] and [LocalContext].
 */
@Composable
fun rememberHapticManager(): HapticManager {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view, context) {
        HapticManager(view = view, context = context)
    }
}
