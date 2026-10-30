package com.example.pomodoro.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.pomodoro.MainActivity
import com.example.pomodoro.R
import com.example.pomodoro.model.PomodoroUiState
import com.example.pomodoro.model.SessionType
import com.example.pomodoro.model.TimerRunState
import java.util.Locale

/**
 * 앱이 백그라운드로 전환되었을 때 스마트폰 상단 상태 표시줄(Status Bar)에
 * 포모도로 타이머 아이콘과 실시간 남은 시간을 표시하는 포그라운드 서비스 및 알림 매니저.
 */
class PomodoroTimerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_OR_UPDATE -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Dynamic Pomodoro"
                val content = intent.getStringExtra(EXTRA_CONTENT) ?: "포모도로 타이머가 동작 중입니다."
                val ongoing = intent.getBooleanExtra(EXTRA_ONGOING, true)

                ensureNotificationChannel(this)
                val notification = buildStatusBarNotification(
                    context = this,
                    title = title,
                    content = content,
                    ongoing = ongoing
                )

                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                }.onFailure {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_NOT_STICKY
    }

    companion object {
        const val CHANNEL_ID = "pomodoro_background_status_channel"
        const val NOTIFICATION_ID = 1001

        private const val ACTION_START_OR_UPDATE = "com.example.pomodoro.action.START_OR_UPDATE"
        private const val ACTION_STOP = "com.example.pomodoro.action.STOP"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_CONTENT = "extra_content"
        private const val EXTRA_ONGOING = "extra_ongoing"

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val existing = manager.getNotificationChannel(CHANNEL_ID)
                if (existing == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "포모도로 백그라운드 상태 표시",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "앱이 백그라운드로 전환되었을 때 상단 상태 표시줄 아이콘과 남은 시간을 표시합니다."
                        setShowBadge(false)
                    }
                    manager.createNotificationChannel(channel)
                }
            }
        }

        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        /**
         * 백그라운드 상태에서 상단 상태 표시줄 아이콘 및 알림을 표시/업데이트합니다.
         */
        fun showOrUpdateBackgroundStatus(context: Context, uiState: PomodoroUiState) {
            if (!hasNotificationPermission(context)) return

            ensureNotificationChannel(context)

            val formattedTime = String.format(
                Locale.US,
                "%02d:%02d",
                uiState.displayMinutes,
                uiState.displaySeconds
            )
            val sessionBadge = "${uiState.currentSession.titleKo} (#${uiState.currentCycleIndex}/${uiState.settings.sessionsBeforeLongBreak})"

            val (title, content, isOngoing) = when (uiState.runState) {
                TimerRunState.RUNNING -> Triple(
                    "$sessionBadge • $formattedTime",
                    "타이머가 실행 중입니다. 탭하여 앱으로 돌아가기",
                    true
                )
                TimerRunState.PAUSED -> Triple(
                    "$sessionBadge • $formattedTime (일시정지)",
                    "타이머가 일시정지되었습니다. 탭하여 재개하기",
                    true
                )
                TimerRunState.COMPLETED_WAITING -> {
                    val nextLabel = if (uiState.currentSession == SessionType.FOCUS) "휴식" else "집중"
                    Triple(
                        "$sessionBadge 완료! (00:00)",
                        "탭하여 다음 $nextLabel 세션을 시작하세요.",
                        true
                    )
                }
                TimerRunState.READY -> Triple(
                    "$sessionBadge • $formattedTime (대기 중)",
                    "탭하여 포모도로 타이머로 돌아가기",
                    false
                )
            }

            val serviceIntent = Intent(context, PomodoroTimerService::class.java).apply {
                action = ACTION_START_OR_UPDATE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_CONTENT, content)
                putExtra(EXTRA_ONGOING, isOngoing)
            }

            runCatching {
                ContextCompat.startForegroundService(context, serviceIntent)
            }.onFailure {
                // 백그라운드 서비스 시작 제한 시 NotificationManager로 직접 상단 아이콘 표시
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(
                    NOTIFICATION_ID,
                    buildStatusBarNotification(context, title, content, isOngoing)
                )
            }
        }

        /**
         * 앱이 다시 포그라운드로 돌아오거나 종료될 때 상단 상태 표시줄 아이콘/알림을 해제합니다.
         */
        fun hideBackgroundStatus(context: Context) {
            runCatching {
                val stopIntent = Intent(context, PomodoroTimerService::class.java).apply {
                    action = ACTION_STOP
                }
                context.stopService(stopIntent)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(NOTIFICATION_ID)
        }

        internal fun buildStatusBarNotification(
            context: Context,
            title: String,
            content: String,
            ongoing: Boolean
        ): Notification {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                pendingIntentFlags
            )

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_pomodoro)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(contentPendingIntent)
                .setOngoing(ongoing)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }
    }
}
