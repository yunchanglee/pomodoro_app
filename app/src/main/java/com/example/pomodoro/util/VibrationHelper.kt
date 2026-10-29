package com.example.pomodoro.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.pomodoro.model.SessionType
import com.example.pomodoro.model.VibrationIntensity

/**
 * 타이머 종료 시 진동 알림 및 60분 아날로그 다이얼 햅틱 피드백 제어 헬퍼
 */
class VibrationHelper(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * 타이머 종료(00:00) 시 세션 종류와 진동 강도에 맞춘 맞춤형 파형 진동 실행
     * - 집중 종료(휴식 진입): 길고 부드러운 2회 릴랙스 펄스 (징~ 징~)
     * - 휴식 종료(집중 진입): 짧고 경쾌한 3회 에너제틱 비트 (톡-톡-징!)
     */
    fun vibrateTimerCompletion(
        completedSession: SessionType,
        intensity: VibrationIntensity
    ) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        val amp = intensity.amplitude
        val softAmp = (amp * 0.7f).toInt().coerceIn(1, 255)

        val timings: LongArray
        val amplitudes: IntArray

        when (completedSession) {
            SessionType.FOCUS -> {
                // 집중 완료 -> 휴식 전환: 릴랙스 더블 펄스 [대기 0, 진동 400, 휴지 200, 진동 600]
                timings = longArrayOf(0L, 400L, 200L, 600L)
                amplitudes = intArrayOf(0, softAmp, 0, amp)
            }
            SessionType.SHORT_BREAK, SessionType.LONG_BREAK -> {
                // 휴식 완료 -> 집중 전환: 에너제틱 트리플 비트 [대기 0, 진동 150, 휴지 100, 진동 150, 휴지 100, 진동 400]
                timings = longArrayOf(0L, 150L, 100L, 150L, 100L, 400L)
                amplitudes = intArrayOf(0, amp, 0, amp, 0, amp)
            }
        }

        if (vib.hasAmplitudeControl()) {
            vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            vib.vibrate(VibrationEffect.createWaveform(timings, -1))
        }
    }

    /**
     * 60분 아날로그 다이얼을 드래그하여 1분(6°) 눈금을 지날 때 발생하는 시계 태엽 햅틱
     */
    fun vibrateDialTick(isFiveMinuteMajorTick: Boolean) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effectId = if (isFiveMinuteMajorTick) {
                VibrationEffect.EFFECT_CLICK
            } else {
                VibrationEffect.EFFECT_TICK
            }
            vib.vibrate(VibrationEffect.createPredefined(effectId))
        } else {
            val duration = if (isFiveMinuteMajorTick) 18L else 10L
            val amplitude = if (isFiveMinuteMajorTick) 160 else 90
            vib.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        }
    }

    /**
     * 시작/일시정지 등 주요 컨트롤 버튼 클릭 시 햅틱 피드백
     */
    fun vibrateButtonClick() {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            vib.vibrate(VibrationEffect.createOneShot(20L, 150))
        }
    }

    /**
     * 진행 중인 진동 즉시 중지
     */
    fun cancel() {
        vibrator?.cancel()
    }
}
