package com.example.pomodoro.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.example.pomodoro.model.PomodoroUiState
import com.example.pomodoro.model.TimerRunState
import com.example.pomodoro.ui.theme.DialFaceBg
import com.example.pomodoro.ui.theme.DynamicSessionColors
import com.example.pomodoro.ui.theme.TextPrimary
import com.example.pomodoro.ui.theme.TextSecondary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * 60분 고정 눈금(360° = 60분, 1분 = 6°) 대형 아날로그 타이머 다이얼 컴포넌트
 * - 대기/일시정지 상태에서 원형 다이얼을 드래그하거나 탭하여 즉시 시간(분) 설정 가능
 * - 실행 중 4초 주기 오로라 호흡 광원 및 종료 시 진동 동기화 동심원 파동(Shockwave Ripple) 렌더링
 */
@Composable
fun AnalogTimerDial(
    sweepAngleDegrees: Float,
    runState: TimerRunState,
    isFinalTenSeconds: Boolean,
    isVibrationRippleActive: Boolean,
    colors: DynamicSessionColors,
    onDialMinutesChanged: (Int) -> Unit,
    onDismissVibrationAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "analogDialInfinite")

    // 4초 주기 배경 오로라 호흡(Breathing) 애니메이션
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingGlowScale"
    )

    // 타이머 종료 진동 시 확산되는 동심원 충격파(Shockwave Ripple) 위상 (0f -> 1f)
    val ripplePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vibrationShockwaveRipple"
    )

    // 마감 10초 전 텐션 심장박동 스케일 및 일시정지 축소 효과
    val dialScale by animateFloatAsState(
        targetValue = when {
            isFinalTenSeconds -> 1.03f
            runState == TimerRunState.PAUSED -> 0.96f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dialScale"
    )

    // 부채꼴 각도 부드러운 스프링/스위핑 보간
    val animatedSweepAngle by animateFloatAsState(
        targetValue = sweepAngleDegrees.coerceIn(0f, 360f),
        animationSpec = if (runState == TimerRunState.RUNNING) {
            tween(durationMillis = 80, easing = LinearEasing)
        } else {
            spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "sectorSweepAngle"
    )

    val isInteractive = runState == TimerRunState.READY || runState == TimerRunState.PAUSED

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(runState, isVibrationRippleActive) {
                    detectTapGestures { offset ->
                        if (isVibrationRippleActive || runState == TimerRunState.COMPLETED_WAITING) {
                            onDismissVibrationAlert()
                        } else if (isInteractive) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val minutes = touchOffsetToMinutes(offset, center)
                            onDialMinutesChanged(minutes)
                        }
                    }
                }
                .pointerInput(isInteractive) {
                    if (!isInteractive) return@pointerInput
                    detectDragGestures { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val minutes = touchOffsetToMinutes(change.position, center)
                        onDialMinutesChanged(minutes)
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = (size.minDimension / 2f) * dialScale
            val dialRadius = maxRadius * 0.82f

            // 1. 배면 다이나믹 오로라 호흡 광원 (Ambient Breathing Glow)
            val glowRadius = dialRadius * (1.22f * breathingScale)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.primaryAccent.copy(alpha = 0.30f),
                        colors.secondaryAccent.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )

            // 2. 타이머 종료 진동 시 동심원 파동 레이어 (Vibration Shockwave Ripple Rings)
            if (isVibrationRippleActive) {
                for (ringIndex in 0 until 3) {
                    val phase = (ripplePhase + ringIndex * 0.33f) % 1f
                    val ringRadius = dialRadius * (0.95f + phase * 0.32f)
                    val ringAlpha = (1f - phase) * 0.65f
                    drawCircle(
                        color = colors.primaryAccent.copy(alpha = ringAlpha),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 4f + (1f - phase) * 6f)
                    )
                }
            }

            // 3. 아날로그 다이얼 외곽 베젤 & 다크 페이스 배경
            drawCircle(
                color = DialFaceBg,
                radius = dialRadius,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = dialRadius,
                center = center,
                style = Stroke(width = 3f)
            )

            // 4. 남은 시간 부채꼴 (60분 고정 눈금 기준 Sector: 12시 방향 -90°에서 시작)
            val sectorRadius = dialRadius * 0.76f
            if (animatedSweepAngle > 0.2f) {
                drawArc(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.secondaryAccent.copy(alpha = 0.95f),
                            colors.primaryAccent.copy(alpha = 0.88f)
                        ),
                        center = center,
                        radius = sectorRadius
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedSweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - sectorRadius, center.y - sectorRadius),
                    size = Size(sectorRadius * 2f, sectorRadius * 2f)
                )

                // 부채꼴 가장자리 선명한 경계선
                val edgeAngleRad = Math.toRadians((-90f + animatedSweepAngle).toDouble())
                val edgeEnd = Offset(
                    x = center.x + (sectorRadius * cos(edgeAngleRad)).toFloat(),
                    y = center.y + (sectorRadius * sin(edgeAngleRad)).toFloat()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = center,
                    end = edgeEnd,
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }

            // 5. 60분 고정 눈금 (Tick Marks: 6° 간격 총 60개, 5분 단위 주 눈금 12개 + 숫자 라벨)
            val labelPaint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                textSize = dialRadius * 0.095f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }

            for (minute in 0 until 60) {
                val angleDeg = minute * 6f
                val angleRad = Math.toRadians((angleDeg - 90f).toDouble())
                val isMajorFiveMin = (minute % 5 == 0)
                val isInsideRemainingSector = angleDeg <= animatedSweepAngle + 0.5f

                val outerTickRadius = dialRadius * 0.95f
                val innerTickRadius = if (isMajorFiveMin) {
                    dialRadius * 0.88f
                } else {
                    dialRadius * 0.915f
                }

                val startOffset = Offset(
                    x = center.x + (innerTickRadius * cos(angleRad)).toFloat(),
                    y = center.y + (innerTickRadius * sin(angleRad)).toFloat()
                )
                val endOffset = Offset(
                    x = center.x + (outerTickRadius * cos(angleRad)).toFloat(),
                    y = center.y + (outerTickRadius * sin(angleRad)).toFloat()
                )

                val tickColor = when {
                    isMajorFiveMin && isInsideRemainingSector -> TextPrimary
                    isMajorFiveMin -> TextSecondary.copy(alpha = 0.65f)
                    isInsideRemainingSector -> TextPrimary.copy(alpha = 0.55f)
                    else -> TextSecondary.copy(alpha = 0.25f)
                }

                drawLine(
                    color = tickColor,
                    start = startOffset,
                    end = endOffset,
                    strokeWidth = if (isMajorFiveMin) 4.5f else 2f,
                    cap = StrokeCap.Round
                )

                // 5분 단위 숫자 라벨 (60, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
                if (isMajorFiveMin) {
                    val labelNumber = if (minute == 0) "60" else minute.toString()
                    val labelRadius = dialRadius * 0.78f
                    val labelX = center.x + (labelRadius * cos(angleRad)).toFloat()
                    val labelY = center.y + (labelRadius * sin(angleRad)).toFloat() -
                        (labelPaint.descent() + labelPaint.ascent()) / 2f

                    labelPaint.color = if (isInsideRemainingSector) {
                        Color.White.toArgb()
                    } else {
                        TextSecondary.copy(alpha = 0.75f).toArgb()
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        labelNumber,
                        labelX,
                        labelY,
                        labelPaint
                    )
                }
            }

            // 6. 부채꼴 끝단 발광 드래그 핸들 노브 (Glowing Knob Handle)
            if (animatedSweepAngle > 0.1f) {
                val knobAngleRad = Math.toRadians((-90f + animatedSweepAngle).toDouble())
                val knobCenter = Offset(
                    x = center.x + (sectorRadius * cos(knobAngleRad)).toFloat(),
                    y = center.y + (sectorRadius * sin(knobAngleRad)).toFloat()
                )
                val knobRadius = if (isInteractive) dialRadius * 0.068f else dialRadius * 0.048f

                // 노브 외곽 발광 후광
                drawCircle(
                    color = colors.primaryAccent.copy(alpha = 0.45f),
                    radius = knobRadius * 1.8f,
                    center = knobCenter
                )
                // 노브 본체
                drawCircle(
                    color = colors.primaryAccent,
                    radius = knobRadius,
                    center = knobCenter
                )
                drawCircle(
                    color = Color.White,
                    radius = knobRadius,
                    center = knobCenter,
                    style = Stroke(width = 3f)
                )
            }

            // 7. 중앙 축 센터 캡 (Center Cap)
            val centerCapRadius = dialRadius * 0.09f
            drawCircle(
                color = Color(0xFF101218),
                radius = centerCapRadius * 1.25f,
                center = center
            )
            drawCircle(
                color = Color(0xFF2B2F3C),
                radius = centerCapRadius,
                center = center
            )
            drawCircle(
                color = colors.primaryAccent.copy(alpha = 0.8f),
                radius = centerCapRadius * 0.38f,
                center = center
            )
        }
    }
}

/**
 * 사용자의 터치 좌표를 12시 방향 기준 시계 방향 각도(0° ~ 360°)로 계산한 뒤 1~60분으로 변환
 */
private fun touchOffsetToMinutes(touch: Offset, center: Offset): Int {
    val dx = touch.x - center.x
    val dy = touch.y - center.y
    // atan2는 3시 방향이 0도이므로 12시 방향(-90도) 기준으로 보정
    val degFromThreeOClock = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    val clockwiseFromTwelve = (degFromThreeOClock + 90f + 360f) % 360f
    return PomodoroUiState.angleToSnappedMinutes(clockwiseFromTwelve, minMinutes = 1, maxMinutes = 60)
}
