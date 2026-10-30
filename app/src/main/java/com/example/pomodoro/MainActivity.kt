package com.example.pomodoro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.example.pomodoro.model.TimerRunState
import com.example.pomodoro.service.PomodoroTimerService
import com.example.pomodoro.ui.PomodoroScreen
import com.example.pomodoro.ui.theme.PomodoroTheme
import com.example.pomodoro.viewmodel.PomodoroViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PomodoroViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PomodoroTimerService.ensureNotificationChannel(this)
        requestNotificationPermissionIfNeeded()

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

    override fun onStart() {
        super.onStart()
        viewModel.onAppBackgroundStateChanged(inBackground = false)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            viewModel.onAppBackgroundStateChanged(inBackground = true)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
