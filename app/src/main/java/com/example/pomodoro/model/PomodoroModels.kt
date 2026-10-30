package com.example.pomodoro.model

import kotlin.math.roundToInt

/**
 * 포모도로 세션 유형 정의
 */
enum class SessionType(
    val titleKo: String,
    val titleEn: String,
    val subtitle: String,
    val minMinutes: Int,
    val maxMinutes: Int
) {
    FOCUS(
        titleKo = "집중 시간",
        titleEn = "FOCUS",
        subtitle = "Focusing deeply...",
        minMinutes = 5,
        maxMinutes = 60
    ),
    SHORT_BREAK(
        titleKo = "짧은 휴식",
        titleEn = "SHORT BREAK",
        subtitle = "Resting & Recharging...",
        minMinutes = 1,
        maxMinutes = 30
    ),
    LONG_BREAK(
        titleKo = "긴 휴식",
        titleEn = "LONG BREAK",
        subtitle = "Deep Relaxation...",
        minMinutes = 5,
        maxMinutes = 60
    )
}

/**
 * 타이머 실행 상태
 */
enum class TimerRunState {
    READY,              // 시작 전 대기 상태 (다이얼 직접 드래그로 시간 조절 가능)
    RUNNING,            // 카운트다운 진행 중
    PAUSED,             // 일시정지 상태
    COMPLETED_WAITING   // 타이머 종료(00:00) 후 수동 전환 대기 상태 (종료 진동 및 파동 활성)
}

/**
 * 타이머 종료 시 진동 강도/패턴 옵션
 */
enum class VibrationIntensity(
    val labelKo: String,
    val labelEn: String,
    val amplitude: Int
) {
    SOFT(labelKo = "부드럽게", labelEn = "Soft", amplitude = 110),
    STANDARD(labelKo = "표준", labelEn = "Standard", amplitude = 190),
    STRONG(labelKo = "강하게", labelEn = "Strong", amplitude = 255)
}

/**
 * 사용자 맞춤 설정 모델 (모든 타이머 최대 눈금 60분 기준)
 */
data class PomodoroSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
    val autoStartBreak: Boolean = true,
    val autoStartFocus: Boolean = false,
    val completionVibrationEnabled: Boolean = true,
    val vibrationIntensity: VibrationIntensity = VibrationIntensity.STANDARD,
    val dialHapticsEnabled: Boolean = true
) {
    fun durationMinutesFor(type: SessionType): Int = when (type) {
        SessionType.FOCUS -> focusMinutes.coerceIn(5, 60)
        SessionType.SHORT_BREAK -> shortBreakMinutes.coerceIn(1, 30)
        SessionType.LONG_BREAK -> longBreakMinutes.coerceIn(5, 60)
    }
}

/**
 * UI 상태 모델 및 60분 고정 아날로그 타이머 각도 변환 유틸리티
 */
data class PomodoroUiState(
    val settings: PomodoroSettings = PomodoroSettings(),
    val currentSession: SessionType = SessionType.FOCUS,
    val runState: TimerRunState = TimerRunState.READY,
    val remainingMillis: Long = 25 * 60 * 1000L,
    val totalSessionMillis: Long = 25 * 60 * 1000L,
    val currentCycleIndex: Int = 1, // 1 .. settings.sessionsBeforeLongBreak
    val completedFocusSessions: Int = 0,
    val isVibrationRippleActive: Boolean = false,
    val isSettingsSheetOpen: Boolean = false
) {
    /**
     * 60분 고정 눈금 기준 아날로그 부채꼴 각도 계산 (360도 = 60분, 1분 = 6도)
     */
    val analogSweepAngleDegrees: Float
        get() = Companion.millisToSixtyMinuteAngle(remainingMillis)

    /**
     * 하단 디지털 타이머 표시용 분(MM)과 초(SS)
     */
    val displayMinutes: Int
        get() {
            val totalSeconds = ((remainingMillis + 999L) / 1000L).coerceAtLeast(0L)
            return (totalSeconds / 60L).toInt()
        }

    val displaySeconds: Int
        get() {
            val totalSeconds = ((remainingMillis + 999L) / 1000L).coerceAtLeast(0L)
            return (totalSeconds % 60L).toInt()
        }

    /**
     * 종료 10초 전 텐션/펄스 애니메이션 활성화 여부
     */
    val isFinalTenSeconds: Boolean
        get() = runState == TimerRunState.RUNNING &&
            remainingMillis in 1L..10_000L

    /**
     * 포모도로 집중 사이클 초기화:
     * 현재 진행 상태와 관계없이 첫 번째 집중 세션(FOCUS #1) 대기(READY) 상태로 되돌리고
     * 누적 완료 집중 횟수(completedFocusSessions)를 0으로 초기화합니다.
     */
    fun resetToFirstFocusCycle(): PomodoroUiState {
        val focusMillis = settings.durationMinutesFor(SessionType.FOCUS) * 60 * 1000L
        return copy(
            currentSession = SessionType.FOCUS,
            runState = TimerRunState.READY,
            remainingMillis = focusMillis,
            totalSessionMillis = focusMillis,
            currentCycleIndex = 1,
            completedFocusSessions = 0,
            isVibrationRippleActive = false
        )
    }

    companion object {
        const val MAX_ANALOG_MINUTES = 60
        const val MAX_ANALOG_MILLIS = MAX_ANALOG_MINUTES * 60 * 1000L
        const val DEGREES_PER_MINUTE = 6f // 360° / 60분 = 6°

        /**
         * 남은 밀리초를 60분 고정 다이얼의 각도(0° ~ 360°)로 변환
         */
        fun millisToSixtyMinuteAngle(millis: Long): Float {
            if (millis <= 0L) return 0f
            val clamped = millis.coerceIn(0L, MAX_ANALOG_MILLIS)
            return (clamped.toFloat() / MAX_ANALOG_MILLIS.toFloat()) * 360f
        }

        /**
         * 12시 방향 기준 시계 방향 각도(0° ~ 360°)를 1분 단위(1~60분)로 스냅 변환
         */
        fun angleToSnappedMinutes(
            clockwiseDegreesFromTop: Float,
            minMinutes: Int = 1,
            maxMinutes: Int = 60
        ): Int {
            val normalized = ((clockwiseDegreesFromTop % 360f) + 360f) % 360f
            val rawMinutes = (normalized / DEGREES_PER_MINUTE).roundToInt()
            val minutes = if (rawMinutes == 0 && normalized > 180f) 60 else rawMinutes
            return minutes.coerceIn(minMinutes, maxMinutes)
        }
    }
}
