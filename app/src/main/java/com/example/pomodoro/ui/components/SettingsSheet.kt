package com.example.pomodoro.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pomodoro.model.PomodoroSettings
import com.example.pomodoro.model.VibrationIntensity
import com.example.pomodoro.ui.theme.SurfaceCardBg
import com.example.pomodoro.ui.theme.SurfaceElevatedBg
import com.example.pomodoro.ui.theme.TextPrimary
import com.example.pomodoro.ui.theme.TextSecondary
import kotlin.math.roundToInt

/**
 * 타이머 시간 설정 (60분 최대 눈금 연동), 자동 휴식 전환, 타이머 종료 진동 설정 바텀시트
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settings: PomodoroSettings,
    currentCycleIndex: Int,
    completedFocusSessions: Int,
    accentColor: Color,
    onFocusMinutesChange: (Int) -> Unit,
    onShortBreakMinutesChange: (Int) -> Unit,
    onLongBreakMinutesChange: (Int) -> Unit,
    onSessionsBeforeLongBreakChange: (Int) -> Unit,
    onResetFocusCycle: () -> Unit,
    onToggleAutoStartBreak: () -> Unit,
    onToggleAutoStartFocus: () -> Unit,
    onToggleCompletionVibration: () -> Unit,
    onSelectVibrationIntensity: (VibrationIntensity) -> Unit,
    onToggleDialHaptics: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCardBg.copy(alpha = 0.96f),
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Timer Settings • 타이머 맞춤 설정",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "슬라이더를 조절하면 60분 아날로그 다이얼과 디지털 타이머에 즉시 반영됩니다.",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // 1. 각 타이머 시간 설정 섹션
            DurationSliderItem(
                label = "Focus (집중 시간)",
                currentMinutes = settings.focusMinutes,
                valueRange = 5f..60f,
                presets = listOf(15, 25, 45, 60),
                accentColor = accentColor,
                onMinutesChange = onFocusMinutesChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            DurationSliderItem(
                label = "Short Break (짧은 휴식)",
                currentMinutes = settings.shortBreakMinutes,
                valueRange = 1f..30f,
                presets = listOf(5, 10, 15),
                accentColor = accentColor,
                onMinutesChange = onShortBreakMinutesChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            DurationSliderItem(
                label = "Long Break (긴 휴식)",
                currentMinutes = settings.longBreakMinutes,
                valueRange = 5f..60f,
                presets = listOf(15, 20, 30, 45),
                accentColor = accentColor,
                onMinutesChange = onLongBreakMinutesChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 긴 휴식 주기 스테퍼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "긴 휴식 주기 (Long Break Interval)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${settings.sessionsBeforeLongBreak}회 집중 완료마다 긴 휴식 진행",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSessionsBeforeLongBreakChange(settings.sessionsBeforeLongBreak - 1) },
                        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f))
                    ) {
                        Text("-", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "${settings.sessionsBeforeLongBreak}회",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    OutlinedButton(
                        onClick = { onSessionsBeforeLongBreakChange(settings.sessionsBeforeLongBreak + 1) },
                        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f))
                    ) {
                        Text("+", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 포모도로 집중(Focus) 사이클 초기화 액션 행
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Focus 사이클 초기화 (Reset Focus Cycle)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "현재 #${currentCycleIndex}/${settings.sessionsBeforeLongBreak} (누적 완료 ${completedFocusSessions}회) → Focus #1 대기 상태로 초기화",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                OutlinedButton(
                    onClick = onResetFocusCycle,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Focus 초기화",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            // 2. 자동 전환 설정 섹션
            Text(
                text = "자동 전환 설정 (Auto-Transition)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SettingSwitchRow(
                title = "집중 완료 후 자동으로 휴식 시작 (Auto-start Break)",
                subtitle = "집중 시간이 끝나면 종료 진동 후 자동으로 휴식 타이머를 시작합니다.",
                checked = settings.autoStartBreak,
                accentColor = accentColor,
                onCheckedChange = { onToggleAutoStartBreak() }
            )

            SettingSwitchRow(
                title = "휴식 완료 후 자동으로 집중 시작 (Auto-start Focus)",
                subtitle = "휴식 시간이 끝나면 다음 집중 타이머를 연속으로 자동 시작합니다.",
                checked = settings.autoStartFocus,
                accentColor = accentColor,
                onCheckedChange = { onToggleAutoStartFocus() }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            // 3. 타이머 종료 진동 알림 & 햅틱 설정 섹션
            Text(
                text = "타이머 종료 진동 알림 (Completion Vibration)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SettingSwitchRow(
                title = "타이머 종료 시 진동 알림 (Completion Vibration)",
                subtitle = "타이머가 00:00에 도달하면 시각적 파동과 함께 진동이 울립니다.",
                checked = settings.completionVibrationEnabled,
                accentColor = accentColor,
                onCheckedChange = { onToggleCompletionVibration() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "종료 진동 강도 선택 (탭하여 진동 미리보기)",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VibrationIntensity.entries.forEach { intensity ->
                    val selected = settings.vibrationIntensity == intensity && settings.completionVibrationEnabled
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectVibrationIntensity(intensity) },
                        label = {
                            Text(
                                text = "${intensity.labelEn} (${intensity.labelKo})",
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.25f),
                            selectedLabelColor = TextPrimary,
                            containerColor = SurfaceElevatedBg,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = Color.White.copy(alpha = 0.12f),
                            selectedBorderColor = accentColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            SettingSwitchRow(
                title = "60분 아날로그 다이얼 드래그 햅틱 (Dial Tick Haptics)",
                subtitle = "다이얼을 돌려 시간을 맞출 때 1분(6°) 눈금마다 미세 진동을 제공합니다.",
                checked = settings.dialHapticsEnabled,
                accentColor = accentColor,
                onCheckedChange = { onToggleDialHaptics() }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DurationSliderItem(
    label: String,
    currentMinutes: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    presets: List<Int>,
    accentColor: Color,
    onMinutesChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "${currentMinutes}분 (${currentMinutes * 6}°)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }

        Slider(
            value = currentMinutes.toFloat(),
            onValueChange = { onMinutesChange(it.roundToInt()) },
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = SurfaceElevatedBg
            )
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = currentMinutes == preset
                FilterChip(
                    selected = isSelected,
                    onClick = { onMinutesChange(preset) },
                    label = { Text("${preset}m", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.22f),
                        selectedLabelColor = accentColor,
                        containerColor = SurfaceElevatedBg,
                        labelColor = TextSecondary
                    )
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceElevatedBg
            )
        )
    }
}
