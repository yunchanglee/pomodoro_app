package com.example.pomodoro

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.pomodoro.model.TimerRunState
import com.example.pomodoro.ui.PomodoroScreen
import com.example.pomodoro.ui.theme.PomodoroTheme
import com.example.pomodoro.viewmodel.PomodoroViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PomodoroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PomodoroTheme {
                val uiState by viewModel.uiState.collectAsState()

                // 타이머가 실행 중이거나 종료 진동 알림 중일 때는 화면 꺼짐 방지 (Keep Screen On)
                LaunchedEffect(uiState.runState) {
                    if (uiState.runState == TimerRunState.RUNNING ||
                        uiState.runState == TimerRunState.COMPLETED_WAITING
                    ) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                PomodoroScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}
