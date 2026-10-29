package com.example.pomodoro.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.example.pomodoro.model.SessionType
import com.example.pomodoro.model.TimerRunState

// 다크 모드 퍼스트 기본 컬러 (#121318)
val DeepCharcoalBg = Color(0xFF121318)
val SurfaceCardBg = Color(0xFF1C1E26)
val SurfaceElevatedBg = Color(0xFF252834)
val DialFaceBg = Color(0xFF181A22)
val TextPrimary = Color(0xFFF5F7FA)
val TextSecondary = Color(0xFF9EA4B8)

// 상태별 다이나믹 포인트 컬러
val FocusCoralPrimary = Color(0xFFFF5252)
val FocusCoralSecondary = Color(0xFFFF7043)

val ShortBreakMintPrimary = Color(0xFF00E676)
val ShortBreakTealSecondary = Color(0xFF26A69A)

val LongBreakBluePrimary = Color(0xFF448AFF)
val LongBreakIndigoSecondary = Color(0xFF7C4DFF)

val PausedAmberAccent = Color(0xFFFFD740)

data class DynamicSessionColors(
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val ambientGlow: Color
)

@Composable
fun rememberDynamicSessionColors(
    sessionType: SessionType,
    runState: TimerRunState
): DynamicSessionColors {
    val targetPrimary = when {
        runState == TimerRunState.PAUSED -> PausedAmberAccent
        sessionType == SessionType.FOCUS -> FocusCoralPrimary
        sessionType == SessionType.SHORT_BREAK -> ShortBreakMintPrimary
        sessionType == SessionType.LONG_BREAK -> LongBreakBluePrimary
        else -> FocusCoralPrimary
    }

    val targetSecondary = when {
        runState == TimerRunState.PAUSED -> Color(0xFFFFAB00)
        sessionType == SessionType.FOCUS -> FocusCoralSecondary
        sessionType == SessionType.SHORT_BREAK -> ShortBreakTealSecondary
        sessionType == SessionType.LONG_BREAK -> LongBreakIndigoSecondary
        else -> FocusCoralSecondary
    }

    val animatedPrimary by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "primaryAccentTransition"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = targetSecondary,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "secondaryAccentTransition"
    )

    return DynamicSessionColors(
        primaryAccent = animatedPrimary,
        secondaryAccent = animatedSecondary,
        ambientGlow = animatedPrimary.copy(alpha = 0.24f)
    )
}

private val PomodoroDarkColorScheme = darkColorScheme(
    primary = FocusCoralPrimary,
    secondary = ShortBreakMintPrimary,
    tertiary = LongBreakBluePrimary,
    background = DeepCharcoalBg,
    surface = SurfaceCardBg,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun PomodoroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PomodoroDarkColorScheme,
        content = content
    )
}
