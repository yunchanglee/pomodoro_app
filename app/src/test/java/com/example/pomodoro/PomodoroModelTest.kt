package com.example.pomodoro

import com.example.pomodoro.model.PomodoroSettings
import com.example.pomodoro.model.PomodoroUiState
import com.example.pomodoro.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroModelTest {

    @Test
    fun sixtyMinuteFixedScale_convertsMinutesToExactAngles() {
        // 60분 고정 눈금: 25분 -> 150도, 5분 -> 30도, 15분 -> 90도, 60분 -> 360도
        assertEquals(150f, PomodoroUiState.millisToSixtyMinuteAngle(25 * 60 * 1000L), 0.01f)
        assertEquals(30f, PomodoroUiState.millisToSixtyMinuteAngle(5 * 60 * 1000L), 0.01f)
        assertEquals(90f, PomodoroUiState.millisToSixtyMinuteAngle(15 * 60 * 1000L), 0.01f)
        assertEquals(360f, PomodoroUiState.millisToSixtyMinuteAngle(60 * 60 * 1000L), 0.01f)
        assertEquals(0f, PomodoroUiState.millisToSixtyMinuteAngle(0L), 0.01f)
    }

    @Test
    fun angleToSnappedMinutes_snapsEverySixDegrees() {
        assertEquals(25, PomodoroUiState.angleToSnappedMinutes(150f, minMinutes = 5, maxMinutes = 60))
        assertEquals(5, PomodoroUiState.angleToSnappedMinutes(30f, minMinutes = 1, maxMinutes = 30))
        assertEquals(60, PomodoroUiState.angleToSnappedMinutes(358f, minMinutes = 5, maxMinutes = 60))
        assertEquals(5, PomodoroUiState.angleToSnappedMinutes(6f, minMinutes = 5, maxMinutes = 60))
    }

    @Test
    fun displayMinutesAndSeconds_roundsUpRemainingMillisCleanly() {
        val state = PomodoroUiState(
            settings = PomodoroSettings(focusMinutes = 25),
            currentSession = SessionType.FOCUS,
            remainingMillis = (24 * 60 + 59) * 1000L
        )
        assertEquals(24, state.displayMinutes)
        assertEquals(59, state.displaySeconds)
    }
}
