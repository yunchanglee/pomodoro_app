package com.example.pomodoro.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pomodoro.model.SessionType
import com.example.pomodoro.model.TimerRunState
import com.example.pomodoro.ui.theme.TextPrimary
import com.example.pomodoro.ui.theme.TextSecondary
import java.util.Locale

/**
 * 큰 아날로그 타이머 바로 밑에 배치되는 고정폭(Monospace) 디지털 타이머 컴포넌트
 * - 숫자가 바뀔 때 위아래로 회전하며 넘어가는 오도미터(Odometer) 롤링 모션 적용
 * - 실행 중 콜론(:) 호흡 깜빡임 및 종료 10초 전 텐션 팝업 애니메이션 지원
 */
@Composable
fun DigitalTimerReadout(
    minutes: Int,
    seconds: Int,
    sessionType: SessionType,
    runState: TimerRunState,
    isFinalTenSeconds: Boolean,
    accentColor: Color,
    fontSize: TextUnit = 52.sp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "digitalTimerInfinite")

    // 실행 중 콜론(:) 1초 주기 호흡 페이드 효과
    val colonAlpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (runState == TimerRunState.RUNNING) 0.28f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colonPulseAlpha"
    )

    // 일시정지 상태에서 숫자 전체가 천천히 숨쉬듯 깜빡이는 효과
    val pausedAlpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (runState == TimerRunState.PAUSED) 0.55f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pausedReadoutAlpha"
    )

    // 마감 10초 전 또는 타이머 종료(00:00) 진동 시 스케일 텐션 효과
    val tensionScale by animateFloatAsState(
        targetValue = when {
            runState == TimerRunState.COMPLETED_WAITING -> 1.08f
            isFinalTenSeconds && (seconds % 2 == 1) -> 1.07f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "finalTenSecScale"
    )

    val formattedMin = String.format(Locale.US, "%02d", minutes)
    val formattedSec = String.format(Locale.US, "%02d", seconds)

    val digitColor = when {
        runState == TimerRunState.COMPLETED_WAITING -> accentColor
        isFinalTenSeconds -> accentColor
        else -> TextPrimary
    }

    val statusSubtitle = when (runState) {
        TimerRunState.READY -> "${sessionType.titleKo} 준비 완료 (다이얼을 돌려 시간 조절)"
        TimerRunState.RUNNING -> sessionType.subtitle
        TimerRunState.PAUSED -> "일시정지됨 (다이얼 드래그 또는 재개 가능)"
        TimerRunState.COMPLETED_WAITING -> "타이머 종료! 진동 알림 중 • 터치하여 다음 세션 시작"
    }

    Column(
        modifier = modifier
            .scale(tensionScale)
            .alpha(pausedAlpha),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            OdometerChar(char = formattedMin[0], fontSize = fontSize, color = digitColor)
            OdometerChar(char = formattedMin[1], fontSize = fontSize, color = digitColor)

            Text(
                text = " : ",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    color = accentColor.copy(alpha = colonAlpha)
                )
            )

            OdometerChar(char = formattedSec[0], fontSize = fontSize, color = digitColor)
            OdometerChar(char = formattedSec[1], fontSize = fontSize, color = digitColor)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = statusSubtitle,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (runState == TimerRunState.COMPLETED_WAITING) accentColor else TextSecondary
            )
        )
    }
}

@Composable
private fun OdometerChar(
    char: Char,
    fontSize: TextUnit,
    color: Color
) {
    AnimatedContent(
        targetState = char,
        transitionSpec = {
            (slideInVertically { height -> height / 2 } + fadeIn(tween(140)))
                .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut(tween(140)))
        },
        label = "odometerDigit"
    ) { targetChar ->
        Text(
            text = targetChar.toString(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = color,
                letterSpacing = (-1).sp
            )
        )
    }
}
