package com.example.pomodoro.ui

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pomodoro.model.PomodoroUiState
import com.example.pomodoro.model.SessionType
import com.example.pomodoro.model.TimerRunState
import com.example.pomodoro.ui.components.AnalogTimerDial
import com.example.pomodoro.ui.components.DigitalTimerReadout
import com.example.pomodoro.ui.components.SettingsBottomSheet
import com.example.pomodoro.ui.theme.DeepCharcoalBg
import com.example.pomodoro.ui.theme.DynamicSessionColors
import com.example.pomodoro.ui.theme.SurfaceCardBg
import com.example.pomodoro.ui.theme.SurfaceElevatedBg
import com.example.pomodoro.ui.theme.TextPrimary
import com.example.pomodoro.ui.theme.TextSecondary
import com.example.pomodoro.ui.theme.rememberDynamicSessionColors
import com.example.pomodoro.viewmodel.PomodoroViewModel

/**
 * 안드로이드 다이나믹 포모도로 타이머 메인 화면
 * - 세로 모드(Portrait) & 가로 모드(Landscape) 반응형 지원
 * - 두 모드 모두 "큰 60분 고정 아날로그 타이머 바로 밑에 디지털 타이머 배치" 원칙 준수
 */
@Composable
fun PomodoroScreen(
    uiState: PomodoroUiState,
    viewModel: PomodoroViewModel
) {
    val dynamicColors = rememberDynamicSessionColors(
        sessionType = uiState.currentSession,
        runState = uiState.runState
    )
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DeepCharcoalBg,
                        dynamicColors.primaryAccent.copy(alpha = 0.09f),
                        DeepCharcoalBg
                    )
                )
            )
    ) {
        if (isLandscape) {
            LandscapeTwoPaneLayout(
                uiState = uiState,
                colors = dynamicColors,
                onDialMinutesChanged = viewModel::onDialDragMinutesChanged,
                onDismissVibrationAlert = viewModel::stopRepeatingVibrationAlert,
                onToggleStartPause = viewModel::toggleStartPause,
                onReset = viewModel::resetCurrentTimer,
                onSkip = viewModel::skipToNextSession,
                onSelectSession = viewModel::selectSessionType,
                onQuickAdjustMinutes = viewModel::adjustRemainingMinutesBy,
                onToggleAutoStartBreak = viewModel::toggleAutoStartBreak,
                onToggleCompletionVibration = viewModel::toggleCompletionVibration,
                onOpenSettings = { viewModel.setSettingsSheetOpen(true) }
            )
        } else {
            PortraitSingleColumnLayout(
                uiState = uiState,
                colors = dynamicColors,
                onDialMinutesChanged = viewModel::onDialDragMinutesChanged,
                onDismissVibrationAlert = viewModel::stopRepeatingVibrationAlert,
                onToggleStartPause = viewModel::toggleStartPause,
                onReset = viewModel::resetCurrentTimer,
                onSkip = viewModel::skipToNextSession,
                onSelectSession = viewModel::selectSessionType,
                onQuickAdjustMinutes = viewModel::adjustRemainingMinutesBy,
                onToggleAutoStartBreak = viewModel::toggleAutoStartBreak,
                onToggleCompletionVibration = viewModel::toggleCompletionVibration,
                onOpenSettings = { viewModel.setSettingsSheetOpen(true) }
            )
        }

        if (uiState.isSettingsSheetOpen) {
            SettingsBottomSheet(
                settings = uiState.settings,
                accentColor = dynamicColors.primaryAccent,
                onFocusMinutesChange = viewModel::updateFocusMinutes,
                onShortBreakMinutesChange = viewModel::updateShortBreakMinutes,
                onLongBreakMinutesChange = viewModel::updateLongBreakMinutes,
                onSessionsBeforeLongBreakChange = viewModel::updateSessionsBeforeLongBreak,
                onToggleAutoStartBreak = viewModel::toggleAutoStartBreak,
                onToggleAutoStartFocus = viewModel::toggleAutoStartFocus,
                onToggleCompletionVibration = viewModel::toggleCompletionVibration,
                onSelectVibrationIntensity = viewModel::updateVibrationIntensity,
                onToggleDialHaptics = viewModel::toggleDialHaptics,
                onDismiss = { viewModel.setSettingsSheetOpen(false) }
            )
        }
    }
}

/**
 * 세로 모드 (Portrait Layout):
 * 상단 앱 바 -> 큰 60분 아날로그 타이머 -> 바로 밑에 디지털 타이머 -> 빠른 조절 칩 -> 하단 컨트롤 버튼
 */
@Composable
private fun PortraitSingleColumnLayout(
    uiState: PomodoroUiState,
    colors: DynamicSessionColors,
    onDialMinutesChanged: (Int) -> Unit,
    onDismissVibrationAlert: () -> Unit,
    onToggleStartPause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onSelectSession: (SessionType) -> Unit,
    onQuickAdjustMinutes: (Int) -> Unit,
    onToggleAutoStartBreak: () -> Unit,
    onToggleCompletionVibration: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. 상단 바 (세션 타이틀, 자동 휴식 퀵 배지, 진동 토글 아이콘, 설정 아이콘)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${uiState.currentSession.titleEn} #${uiState.currentCycleIndex}/${uiState.settings.sessionsBeforeLongBreak}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    SessionCycleDots(
                        currentCycle = uiState.currentCycleIndex,
                        totalCycles = uiState.settings.sessionsBeforeLongBreak,
                        accentColor = colors.primaryAccent
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleCompletionVibration) {
                        Icon(
                            imageVector = if (uiState.settings.completionVibrationEnabled) {
                                Icons.Default.Vibration
                            } else {
                                Icons.Default.NotificationsOff
                            },
                            contentDescription = "타이머 종료 진동 토글",
                            tint = if (uiState.settings.completionVibrationEnabled) {
                                colors.primaryAccent
                            } else {
                                TextSecondary
                            }
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "타이머 설정 열기",
                            tint = TextPrimary
                        )
                    }
                }
            }

            // 자동 휴식 전환 원탭 상태 캡슐 배지 + 세션 전환 탭
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AutoBreakQuickBadge(
                    autoStartBreak = uiState.settings.autoStartBreak,
                    accentColor = colors.primaryAccent,
                    onClick = onToggleAutoStartBreak
                )

                SessionSelectorPills(
                    selectedSession = uiState.currentSession,
                    accentColor = colors.primaryAccent,
                    onSelectSession = onSelectSession
                )
            }
        }

        // 2. 핵심 타이머 코어 유닛: 상단 대형 60분 아날로그 타이머 + 그 바로 밑에 디지털 타이머 수직 배치
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnalogTimerDial(
                sweepAngleDegrees = uiState.analogSweepAngleDegrees,
                runState = uiState.runState,
                isFinalTenSeconds = uiState.isFinalTenSeconds,
                isVibrationRippleActive = uiState.isVibrationRippleActive,
                colors = colors,
                onDialMinutesChanged = onDialMinutesChanged,
                onDismissVibrationAlert = onDismissVibrationAlert,
                modifier = Modifier
                    .fillMaxWidth(0.80f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 큰 아날로그 타이머 바로 밑에 배치되는 디지털 타이머
            DigitalTimerReadout(
                minutes = uiState.displayMinutes,
                seconds = uiState.displaySeconds,
                sessionType = uiState.currentSession,
                runState = uiState.runState,
                isFinalTenSeconds = uiState.isFinalTenSeconds,
                accentColor = colors.primaryAccent,
                fontSize = 54.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            QuickTimeAdjustRow(
                onMinusFive = { onQuickAdjustMinutes(-5) },
                onPlusFive = { onQuickAdjustMinutes(5) }
            )
        }

        // 3. 하단 다이나믹 컨트롤 액션 바
        TimerControlActionBar(
            runState = uiState.runState,
            currentSession = uiState.currentSession,
            accentColor = colors.primaryAccent,
            onReset = onReset,
            onToggleStartPause = onToggleStartPause,
            onSkip = onSkip,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

/**
 * 가로 모드 (Landscape Two-Pane Layout):
 * - 좌측 영역 (55%): 큰 60분 아날로그 타이머 + 바로 밑에 디지털 타이머 수직 배치 (위계 일관성 유지)
 * - 우측 영역 (45%): 세션 정보, 자동 휴식 전환 & 종료 진동 토글 카드, 빠른 시간 조절, 컨트롤 버튼
 */
@Composable
private fun LandscapeTwoPaneLayout(
    uiState: PomodoroUiState,
    colors: DynamicSessionColors,
    onDialMinutesChanged: (Int) -> Unit,
    onDismissVibrationAlert: () -> Unit,
    onToggleStartPause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onSelectSession: (SessionType) -> Unit,
    onQuickAdjustMinutes: (Int) -> Unit,
    onToggleAutoStartBreak: () -> Unit,
    onToggleCompletionVibration: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 좌측 패널 (55%): 큰 60분 아날로그 타이머 + 바로 밑에 디지털 타이머 배치
        Surface(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceCardBg.copy(alpha = 0.65f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnalogTimerDial(
                    sweepAngleDegrees = uiState.analogSweepAngleDegrees,
                    runState = uiState.runState,
                    isFinalTenSeconds = uiState.isFinalTenSeconds,
                    isVibrationRippleActive = uiState.isVibrationRippleActive,
                    colors = colors,
                    onDialMinutesChanged = onDialMinutesChanged,
                    onDismissVibrationAlert = onDismissVibrationAlert,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 아날로그 타이머 바로 밑에 배치되는 디지털 타이머
                DigitalTimerReadout(
                    minutes = uiState.displayMinutes,
                    seconds = uiState.displaySeconds,
                    sessionType = uiState.currentSession,
                    runState = uiState.runState,
                    isFinalTenSeconds = uiState.isFinalTenSeconds,
                    accentColor = colors.primaryAccent,
                    fontSize = 42.sp
                )
            }
        }

        // 우측 패널 (45%): 세션 상태, 자동 휴식 전환 & 종료 진동 카드, 빠른 시간 조절, 컨트롤 버튼
        Surface(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceCardBg.copy(alpha = 0.65f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 우측 상단 헤더
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = uiState.currentSession.titleEn,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            SessionCycleDots(
                                currentCycle = uiState.currentCycleIndex,
                                totalCycles = uiState.settings.sessionsBeforeLongBreak,
                                accentColor = colors.primaryAccent
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SessionSelectorPills(
                                selectedSession = uiState.currentSession,
                                accentColor = colors.primaryAccent,
                                onSelectSession = onSelectSession
                            )
                            IconButton(onClick = onOpenSettings) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "설정 열기",
                                    tint = TextPrimary
                                )
                            }
                        }
                    }
                }

                // 자동 휴식 전환 & 타이머 종료 진동 토글 카드
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceElevatedBg.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CompactToggleRow(
                            label = "Auto-Transition to Break (자동 휴식)",
                            checked = uiState.settings.autoStartBreak,
                            accentColor = colors.primaryAccent,
                            onToggle = onToggleAutoStartBreak
                        )
                        CompactToggleRow(
                            label = "Completion Vibration (종료 진동: ${uiState.settings.vibrationIntensity.labelKo})",
                            checked = uiState.settings.completionVibrationEnabled,
                            accentColor = colors.primaryAccent,
                            onToggle = onToggleCompletionVibration
                        )
                    }
                }

                // 빠른 시간 조절 (-5m / +5m)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Quick time adjust",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    QuickTimeAdjustRow(
                        onMinusFive = { onQuickAdjustMinutes(-5) },
                        onPlusFive = { onQuickAdjustMinutes(5) }
                    )
                }

                // 하단 컨트롤 액션 바
                TimerControlActionBar(
                    runState = uiState.runState,
                    currentSession = uiState.currentSession,
                    accentColor = colors.primaryAccent,
                    onReset = onReset,
                    onToggleStartPause = onToggleStartPause,
                    onSkip = onSkip
                )
            }
        }
    }
}

@Composable
private fun AutoBreakQuickBadge(
    autoStartBreak: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (autoStartBreak) accentColor.copy(alpha = 0.18f) else SurfaceElevatedBg,
        label = "autoBreakBadgeBg"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (autoStartBreak) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = if (autoStartBreak) accentColor else TextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = if (autoStartBreak) "Auto-Break ON" else "Auto-Break OFF",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (autoStartBreak) accentColor else TextSecondary
        )
    }
}

@Composable
private fun SessionSelectorPills(
    selectedSession: SessionType,
    accentColor: Color,
    onSelectSession: (SessionType) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(SurfaceElevatedBg)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SessionType.entries.forEach { session ->
            val isSelected = session == selectedSession
            val label = when (session) {
                SessionType.FOCUS -> "집중"
                SessionType.SHORT_BREAK -> "짧은휴식"
                SessionType.LONG_BREAK -> "긴휴식"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) accentColor else Color.Transparent)
                    .clickable { onSelectSession(session) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.Black else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SessionCycleDots(
    currentCycle: Int,
    totalCycles: Int,
    accentColor: Color
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 1..totalCycles) {
            val filled = i <= currentCycle
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (filled) accentColor else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (filled) accentColor else TextSecondary.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun QuickTimeAdjustRow(
    onMinusFive: () -> Unit,
    onPlusFive: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = SurfaceElevatedBg,
            modifier = Modifier.clickable(onClick = onMinusFive)
        ) {
            Text(
                text = "-5m",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = SurfaceElevatedBg,
            modifier = Modifier.clickable(onClick = onPlusFive)
        ) {
            Text(
                text = "+5m",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun CompactToggleRow(
    label: String,
    checked: Boolean,
    accentColor: Color,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DeepCharcoalBg
            )
        )
    }
}

@Composable
private fun TimerControlActionBar(
    runState: TimerRunState,
    currentSession: SessionType,
    accentColor: Color,
    onReset: () -> Unit,
    onToggleStartPause: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 상태에 따라 원형(36.dp) <-> 둥근 사각형(22.dp)으로 모핑되는 다이나믹 아이콘 전용 버튼
    val mainButtonCornerRadius by animateDpAsState(
        targetValue = if (runState == TimerRunState.RUNNING) 22.dp else 36.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mainCtaCornerRadius"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 초기화 (Reset) 보조 아이콘 버튼
        Surface(
            shape = CircleShape,
            color = SurfaceElevatedBg,
            modifier = Modifier
                .size(56.dp)
                .clickable(onClick = onReset)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = TextPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        // 메인 다이나믹 모핑 CTA 버튼 (텍스트 없이 아이콘으로만 깔끔하게 표시)
        Surface(
            shape = RoundedCornerShape(mainButtonCornerRadius),
            color = accentColor,
            shadowElevation = 8.dp,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(mainButtonCornerRadius))
                .clickable(onClick = onToggleStartPause)
        ) {
            Box(contentAlignment = Alignment.Center) {
                val iconVector = if (runState == TimerRunState.RUNNING) {
                    Icons.Default.Pause
                } else {
                    Icons.Default.PlayArrow
                }
                val contentDesc = when (runState) {
                    TimerRunState.RUNNING -> "Pause"
                    TimerRunState.PAUSED -> "Resume"
                    TimerRunState.COMPLETED_WAITING -> {
                        if (currentSession == SessionType.FOCUS) "Start Break" else "Start Focus"
                    }
                    TimerRunState.READY -> "Start"
                }
                Icon(
                    imageVector = iconVector,
                    contentDescription = contentDesc,
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        // 다음 세션 스킵 (Skip) 보조 아이콘 버튼
        Surface(
            shape = CircleShape,
            color = SurfaceElevatedBg,
            modifier = Modifier
                .size(56.dp)
                .clickable(onClick = onSkip)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Skip",
                    tint = TextPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
