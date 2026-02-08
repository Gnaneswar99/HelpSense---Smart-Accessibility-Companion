package com.accessai.android.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.accessai.core.util.HapticService

/**
 * Android implementation of HapticService.
 * Uses the Vibrator API with VibrationEffect for precise haptic patterns.
 *
 * Haptic patterns are designed for accessibility:
 * - Light tap: UI confirmation (button press, selection)
 * - Medium impact: Object/sound detected
 * - Heavy impact: Critical alert (fire alarm, siren, obstacle ahead)
 * - Custom patterns: Configurable for user preferences
 */
class AndroidHapticService(
    context: Context
) : HapticService {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override fun lightTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    override fun mediumImpact() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(80, 150)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(80)
        }
    }

    override fun heavyImpact() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Three strong pulses for critical alerts
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 150, 100, 150, 100, 200),  // timings
                    intArrayOf(0, 255, 0, 255, 0, 255),        // amplitudes
                    -1  // no repeat
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 150, 100, 150, 100, 200), -1)
        }
    }

    override fun customPattern(pattern: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(pattern, -1)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    override fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Short double tap — "confirmed"
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 40, 60, 40),
                    intArrayOf(0, 180, 0, 180),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 40, 60, 40), -1)
        }
    }

    override fun warning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Two medium pulses — "attention"
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 100, 80, 100),
                    intArrayOf(0, 200, 0, 200),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 100, 80, 100), -1)
        }
    }

    override fun error() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Long strong buzz — "critical"
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 200, 100, 200, 100, 300),
                    intArrayOf(0, 255, 0, 255, 0, 255),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 200, 100, 200, 100, 300), -1)
        }
    }
}
