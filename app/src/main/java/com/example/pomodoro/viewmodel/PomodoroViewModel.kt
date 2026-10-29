package com.example.pomodoro.viewmodel

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodoro.model.PomodoroSettings
import com.example.pomodoro.model.PomodoroUiState
import com.example.pomodoro.model.SessionType
import com.example.pomodoro.model.TimerRunState
import com.example.pomodoro.model.VibrationIntensity
import com.example.pomodoro.util.VibrationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)
    private val vibrationHelper = VibrationHelper(application.applicationContext)

    private val _uiState = MutableStateFlow( createInitialState() )
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var repeatingVibrationJob: Job? = null
    private var lastTickedDialMinute: Int = -1

    private fun createInitialState(): PomodoroUiState {
        val loadedSettings = loadSettings()
        val initialMillis = loadedSettings.focusMinutes * 60 * 1000L
        return PomodoroUiState(
            settings = loadedSettings,
            currentSession = SessionType.FOCUS,
            runState = TimerRunState.READY,
            remainingMillis = initialMillis,
            totalSessionMillis = initialMillis,
            currentCycleIndex = 1,
            completedFocusSessions = 0
        )
    }

    private fun loadSettings(): PomodoroSettings {
        val intensityName = prefs.getString("vibration_intensity", VibrationIntensity.STANDARD.name)
            ?: VibrationIntensity.STANDARD.name
        val intensity = runCatching { VibrationIntensity.valueOf(intensityName) }
            .getOrDefault(VibrationIntensity.STANDARD)

        return PomodoroSettings(
            focusMinutes = prefs.getInt("focus_minutes", 25).coerceIn(5, 60),
            shortBreakMinutes = prefs.getInt("short_break_minutes", 5).coerceIn(1, 30),
            longBreakMinutes = prefs.getInt("long_break_minutes", 15).coerceIn(5, 60),
            sessionsBeforeLongBreak = prefs.getInt("sessions_before_long_break", 4).coerceIn(2, 10),
            autoStartBreak = prefs.getBoolean("auto_start_break", true),
            autoStartFocus = prefs.getBoolean("auto_start_focus", false),
            completionVibrationEnabled = prefs.getBoolean("completion_vibration_enabled", true),
            vibrationIntensity = intensity,
            dialHapticsEnabled = prefs.getBoolean("dial_haptics_enabled", true)
        )
    }

    private fun saveSettings(settings: PomodoroSettings) {
        prefs.edit()
            .putInt("focus_minutes", settings.focusMinutes)
            .putInt("short_break_minutes", settings.shortBreakMinutes)
            .putInt("long_break_minutes", settings.longBreakMinutes)
            .putInt("sessions_before_long_break", settings.sessionsBeforeLongBreak)
            .putBoolean("auto_start_break", settings.autoStartBreak)
            .putBoolean("auto_start_focus", settings.autoStartFocus)
            .putBoolean("completion_vibration_enabled", settings.completionVibrationEnabled)
            .putString("vibration_intensity", settings.vibrationIntensity.name)
            .putBoolean("dial_haptics_enabled", settings.dialHapticsEnabled)
            .apply()
    }

    /**
     * 시작 / 일시정지 / 완료 후 다음 세션 수동 시작 토글
     */
    fun toggleStartPause() {
        vibrationHelper.vibrateButtonClick()
        stopRepeatingVibrationAlert()

        when (_uiState.value.runState) {
            TimerRunState.READY, TimerRunState.PAUSED -> startCountdown()
            TimerRunState.RUNNING -> pauseCountdown()
            TimerRunState.COMPLETED_WAITING -> advanceToNextSession(autoStart = true)
        }
    }

    private fun startCountdown() {
        timerJob?.cancel()
        val startRemaining = _uiState.value.remainingMillis.coerceAtLeast(1000L)
        val targetEndRealtime = SystemClock.elapsedRealtime() + startRemaining

        _uiState.update {
            it.copy(
                runState = TimerRunState.RUNNING,
                isVibrationRippleActive = false
            )
        }

        timerJob = viewModelScope.launch {
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val left = (targetEndRealtime - now).coerceAtLeast(0L)
                _uiState.update { it.copy(remainingMillis = left) }

                if (left <= 0L) {
                    onTimerReachedZero()
                    break
                }
                delay(40L) // 약 25fps~60fps 부드러운 아날로그 부채꼴 감소
            }
        }
    }

    private fun pauseCountdown() {
        timerJob?.cancel()
        _uiState.update { it.copy(runState = TimerRunState.PAUSED) }
    }

    /**
     * 현재 세션 타이머 초기화
     */
    fun resetCurrentTimer() {
        vibrationHelper.vibrateButtonClick()
        timerJob?.cancel()
        stopRepeatingVibrationAlert()

        val state = _uiState.value
        val fullMillis = state.settings.durationMinutesFor(state.currentSession) * 60 * 1000L
        _uiState.update {
            it.copy(
                runState = TimerRunState.READY,
                remainingMillis = fullMillis,
                totalSessionMillis = fullMillis,
                isVibrationRippleActive = false
            )
        }
    }

    /**
     * 다음 세션으로 건너뛰기 (Skip)
     */
    fun skipToNextSession() {
        vibrationHelper.vibrateButtonClick()
        timerJob?.cancel()
        stopRepeatingVibrationAlert()
        advanceToNextSession(autoStart = false)
    }

    /**
     * 특정 세션 탭 직접 선택 (집중 / 짧은 휴식 / 긴 휴식)
     */
    fun selectSessionType(sessionType: SessionType) {
        vibrationHelper.vibrateButtonClick()
        timerJob?.cancel()
        stopRepeatingVibrationAlert()

        val durationMillis = _uiState.value.settings.durationMinutesFor(sessionType) * 60 * 1000L
        _uiState.update {
            it.copy(
                currentSession = sessionType,
                runState = TimerRunState.READY,
                remainingMillis = durationMillis,
                totalSessionMillis = durationMillis,
                isVibrationRippleActive = false
            )
        }
    }

    /**
     * 60분 고정 아날로그 다이얼을 드래그하여 직접 시간(분)을 조절할 때 호출
     */
    fun onDialDragMinutesChanged(rawMinutes: Int) {
        val state = _uiState.value
        if (state.runState == TimerRunState.RUNNING) return

        val session = state.currentSession
        val clampedMinutes = rawMinutes.coerceIn(session.minMinutes, session.maxMinutes)

        if (clampedMinutes != lastTickedDialMinute) {
            lastTickedDialMinute = clampedMinutes
            if (state.settings.dialHapticsEnabled) {
                vibrationHelper.vibrateDialTick(isFiveMinuteMajorTick = (clampedMinutes % 5 == 0))
            }
        }

        val updatedSettings = when (session) {
            SessionType.FOCUS -> state.settings.copy(focusMinutes = clampedMinutes)
            SessionType.SHORT_BREAK -> state.settings.copy(shortBreakMinutes = clampedMinutes)
            SessionType.LONG_BREAK -> state.settings.copy(longBreakMinutes = clampedMinutes)
        }
        saveSettings(updatedSettings)

        val newMillis = clampedMinutes * 60 * 1000L
        _uiState.update {
            it.copy(
                settings = updatedSettings,
                runState = TimerRunState.READY,
                remainingMillis = newMillis,
                totalSessionMillis = newMillis
            )
        }
    }

    /**
     * 빠른 시간 조절 (-5분 / +5분 또는 완료 상태에서 +5분 연장)
     */
    fun adjustRemainingMinutesBy(deltaMinutes: Int) {
        vibrationHelper.vibrateButtonClick()
        stopRepeatingVibrationAlert()

        val state = _uiState.value
        val currentMinutes = ((state.remainingMillis + 59_999L) / 60_000L).toInt()
        val newMinutes = (currentMinutes + deltaMinutes).coerceIn(1, 60)
        val newMillis = newMinutes * 60 * 1000L

        val wasRunning = state.runState == TimerRunState.RUNNING
        timerJob?.cancel()

        _uiState.update {
            it.copy(
                remainingMillis = newMillis,
                totalSessionMillis = maxOf(it.totalSessionMillis, newMillis),
                runState = if (wasRunning) TimerRunState.READY else it.runState
            )
        }

        if (wasRunning) {
            startCountdown()
        }
    }

    /**
     * 타이머가 00:00에 도달했을 때의 처리:
     * 1) 종료 진동 알림 및 동심원 파동(Ripple) 실행
     * 2) 자동 휴식/집중 진행 설정(autoStartBreak / autoStartFocus) 여부에 따라 자동 전환 또는 수동 대기
     */
    private fun onTimerReachedZero() {
        val state = _uiState.value
        val completedSession = state.currentSession
        val shouldAutoAdvance = when (completedSession) {
            SessionType.FOCUS -> state.settings.autoStartBreak
            SessionType.SHORT_BREAK, SessionType.LONG_BREAK -> state.settings.autoStartFocus
        }

        if (shouldAutoAdvance) {
            // 자동 전환 ON: 종료 진동 1사이클 + 파동 애니메이션 후 자동으로 다음 세션 시작
            _uiState.update { it.copy(isVibrationRippleActive = true) }
            if (state.settings.completionVibrationEnabled) {
                vibrationHelper.vibrateTimerCompletion(
                    completedSession = completedSession,
                    intensity = state.settings.vibrationIntensity
                )
            }

            viewModelScope.launch {
                delay(1300L)
                advanceToNextSession(autoStart = true)
            }
        } else {
            // 자동 전환 OFF: 완료 대기 상태로 진입하며 진동 & 파동 알림 반복 (최대 5회)
            _uiState.update {
                it.copy(
                    runState = TimerRunState.COMPLETED_WAITING,
                    remainingMillis = 0L,
                    isVibrationRippleActive = true
                )
            }
            startRepeatingCompletionVibration(completedSession)
        }
    }

    private fun startRepeatingCompletionVibration(completedSession: SessionType) {
        repeatingVibrationJob?.cancel()
        repeatingVibrationJob = viewModelScope.launch {
            val settings = _uiState.value.settings
            repeat(5) {
                if (!isActive || _uiState.value.runState != TimerRunState.COMPLETED_WAITING) return@launch
                if (settings.completionVibrationEnabled) {
                    vibrationHelper.vibrateTimerCompletion(
                        completedSession = completedSession,
                        intensity = settings.vibrationIntensity
                    )
                }
                delay(2200L)
            }
            _uiState.update { it.copy(isVibrationRippleActive = false) }
        }
    }

    fun stopRepeatingVibrationAlert() {
        repeatingVibrationJob?.cancel()
        vibrationHelper.cancel()
        _uiState.update { it.copy(isVibrationRippleActive = false) }
    }

    private fun advanceToNextSession(autoStart: Boolean) {
        val state = _uiState.value
        val settings = state.settings

        val nextSession: SessionType
        var nextCycleIndex = state.currentCycleIndex
        var nextCompletedFocus = state.completedFocusSessions

        if (state.currentSession == SessionType.FOCUS) {
            nextCompletedFocus += 1
            if (state.currentCycleIndex >= settings.sessionsBeforeLongBreak) {
                nextSession = SessionType.LONG_BREAK
            } else {
                nextSession = SessionType.SHORT_BREAK
            }
        } else {
            nextSession = SessionType.FOCUS
            if (state.currentSession == SessionType.LONG_BREAK) {
                nextCycleIndex = 1
            } else {
                nextCycleIndex = (state.currentCycleIndex % settings.sessionsBeforeLongBreak) + 1
            }
        }

        val nextDurationMillis = settings.durationMinutesFor(nextSession) * 60 * 1000L
        _uiState.update {
            it.copy(
                currentSession = nextSession,
                runState = TimerRunState.READY,
                remainingMillis = nextDurationMillis,
                totalSessionMillis = nextDurationMillis,
                currentCycleIndex = nextCycleIndex,
                completedFocusSessions = nextCompletedFocus,
                isVibrationRippleActive = false
            )
        }

        if (autoStart) {
            startCountdown()
        }
    }

    // --- 설정 변경 메서드 ---

    fun setSettingsSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isSettingsSheetOpen = open) }
    }

    fun updateFocusMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(5, 60)
        updateSettingsAndSyncIfIdle { it.copy(focusMinutes = clamped) }
    }

    fun updateShortBreakMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 30)
        updateSettingsAndSyncIfIdle { it.copy(shortBreakMinutes = clamped) }
    }

    fun updateLongBreakMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(5, 60)
        updateSettingsAndSyncIfIdle { it.copy(longBreakMinutes = clamped) }
    }

    fun updateSessionsBeforeLongBreak(count: Int) {
        val clamped = count.coerceIn(2, 10)
        updateSettingsAndSyncIfIdle { it.copy(sessionsBeforeLongBreak = clamped) }
    }

    fun toggleAutoStartBreak() {
        vibrationHelper.vibrateButtonClick()
        updateSettingsAndSyncIfIdle { it.copy(autoStartBreak = !it.autoStartBreak) }
    }

    fun toggleAutoStartFocus() {
        vibrationHelper.vibrateButtonClick()
        updateSettingsAndSyncIfIdle { it.copy(autoStartFocus = !it.autoStartFocus) }
    }

    fun toggleCompletionVibration() {
        val newEnabled = !_uiState.value.settings.completionVibrationEnabled
        updateSettingsAndSyncIfIdle { it.copy(completionVibrationEnabled = newEnabled) }
        if (newEnabled) {
            previewCompletionVibration(_uiState.value.settings.vibrationIntensity)
        } else {
            stopRepeatingVibrationAlert()
        }
    }

    fun updateVibrationIntensity(intensity: VibrationIntensity) {
        updateSettingsAndSyncIfIdle {
            it.copy(
                completionVibrationEnabled = true,
                vibrationIntensity = intensity
            )
        }
        previewCompletionVibration(intensity)
    }

    fun toggleDialHaptics() {
        vibrationHelper.vibrateButtonClick()
        updateSettingsAndSyncIfIdle { it.copy(dialHapticsEnabled = !it.dialHapticsEnabled) }
    }

    private fun previewCompletionVibration(intensity: VibrationIntensity) {
        vibrationHelper.vibrateTimerCompletion(
            completedSession = _uiState.value.currentSession,
            intensity = intensity
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isVibrationRippleActive = true) }
            delay(1200L)
            if (_uiState.value.runState != TimerRunState.COMPLETED_WAITING) {
                _uiState.update { it.copy(isVibrationRippleActive = false) }
            }
        }
    }

    private fun updateSettingsAndSyncIfIdle(reducer: (PomodoroSettings) -> PomodoroSettings) {
        val current = _uiState.value
        val updated = reducer(current.settings)
        saveSettings(updated)

        val shouldSyncRemaining = current.runState == TimerRunState.READY
        val syncedMillis = if (shouldSyncRemaining) {
            updated.durationMinutesFor(current.currentSession) * 60 * 1000L
        } else {
            current.remainingMillis
        }

        _uiState.update {
            it.copy(
                settings = updated,
                remainingMillis = syncedMillis,
                totalSessionMillis = if (shouldSyncRemaining) syncedMillis else it.totalSessionMillis
            )
        }
    }
}
