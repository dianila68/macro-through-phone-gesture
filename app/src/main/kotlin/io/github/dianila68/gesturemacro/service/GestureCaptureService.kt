package io.github.dianila68.gesturemacro.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Persistent foreground service hosting the capture pipeline. Holds no detection
 * logic itself (DESIGN.md): it owns the lifecycle of the sensor module and engine.
 */
class GestureCaptureService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var heartbeat: Heartbeat

    override fun onCreate() {
        super.onCreate()
        heartbeat = Heartbeat(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            heartbeat.recordStop()
            runningState.value = false
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground()
        startHeartbeatLoop()
        runningState.value = true
        return START_STICKY
    }

    override fun onDestroy() {
        runningState.value = false
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
    }

    private fun startHeartbeatLoop() {
        scope.launch {
            while (isActive) {
                heartbeat.beat()
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private fun buildNotification(): android.app.Notification {
        val stopIntent = Intent(this, GestureCaptureService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GestureMacro engine running")
            .setContentText("Listening for hardware gestures")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Stop", stopPending)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gesture engine",
            NotificationManager.IMPORTANCE_LOW,
        )
        channel.description = "Persistent notification required while gesture capture is active"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_STOP = "io.github.dianila68.gesturemacro.action.STOP"
        const val CHANNEL_ID = "gesture_engine"
        const val NOTIFICATION_ID = 1001
        const val HEARTBEAT_INTERVAL_MS = 60_000L

        private val runningState = MutableStateFlow(false)

        /** Observed by the UI; process-local, which is fine: UI and service share the process. */
        val running: StateFlow<Boolean> = runningState

        fun start(context: Context) {
            context.startForegroundService(Intent(context, GestureCaptureService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, GestureCaptureService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
