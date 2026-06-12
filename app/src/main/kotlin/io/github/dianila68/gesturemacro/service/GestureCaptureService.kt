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
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import io.github.dianila68.gesturemacro.core.actions.AccessibilityExecutor
import io.github.dianila68.gesturemacro.core.actions.ActionDispatcher
import io.github.dianila68.gesturemacro.core.actions.FlashlightExecutor
import io.github.dianila68.gesturemacro.core.actions.IntentExecutor
import io.github.dianila68.gesturemacro.core.actions.MediaControlExecutor
import io.github.dianila68.gesturemacro.core.data.MacroStore
import io.github.dianila68.gesturemacro.core.engine.MacroEngine
import io.github.dianila68.gesturemacro.core.sensors.AndroidSensorStream
import io.github.dianila68.gesturemacro.core.sensors.FlipDetector
import io.github.dianila68.gesturemacro.core.sensors.GestureEvent
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import io.github.dianila68.gesturemacro.core.sensors.SensorType
import io.github.dianila68.gesturemacro.core.sensors.ShakeDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Persistent foreground service hosting the capture pipeline. Holds no detection
 * logic itself (DESIGN.md): it owns the lifecycle of the sensor module and engine.
 */
class GestureCaptureService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var heartbeat: Heartbeat
    private lateinit var dispatcher: ActionDispatcher
    private lateinit var engine: MacroEngine
    private var pipelineJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        heartbeat = Heartbeat(this)
        dispatcher = ActionDispatcher(
            systemToggle = FlashlightExecutor(this),
            mediaControl = MediaControlExecutor(this),
            intent = IntentExecutor(this),
            accessibility = AccessibilityExecutor(),
        )
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        engine = MacroEngine(screenOn = { powerManager.isInteractive })
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
        startPipeline()
        runningState.value = true
        return START_STICKY
    }

    private fun startPipeline() {
        if (pipelineJob != null) return
        val detectors = listOf(
            ShakeDetector(),
            FlipDetector(GesturePattern.FLIP_FACE_DOWN),
            FlipDetector(GesturePattern.FLIP_FACE_UP),
        )
        pipelineJob = scope.launch {
            AndroidSensorStream(this@GestureCaptureService)
                .samples(SensorType.ACCELEROMETER, samplingPeriodUs = SAMPLING_PERIOD_US)
                .catch { e -> Log.w(TAG, "Sensor stream ended: ${e.message}") }
                .collect { sample ->
                    for (detector in detectors) {
                        val event = detector.feed(sample) ?: continue
                        onGesture(event)
                    }
                }
        }
    }

    private fun onGesture(event: GestureEvent) {
        Log.i(TAG, "Gesture detected: ${event.pattern} (confidence ${event.confidence})")
        lastGestureState.value = event
        val fired = engine.match(event, MacroStore.macros.value)
        for (macro in fired) {
            scope.launch {
                val results = dispatcher.run(macro)
                Log.i(TAG, "Macro '${macro.name}' executed: $results")
            }
        }
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
        private const val TAG = "GestureCapture"
        const val ACTION_STOP = "io.github.dianila68.gesturemacro.action.STOP"
        const val CHANNEL_ID = "gesture_engine"
        const val NOTIFICATION_ID = 1001
        const val HEARTBEAT_INTERVAL_MS = 60_000L

        /** 50 Hz: responsive enough for FR-2 latency while staying battery-sane (NFR-1). */
        const val SAMPLING_PERIOD_US = 20_000

        private val runningState = MutableStateFlow(false)
        private val lastGestureState = MutableStateFlow<GestureEvent?>(null)

        /** Observed by the UI; process-local, which is fine: UI and service share the process. */
        val running: StateFlow<Boolean> = runningState
        val lastGesture: StateFlow<GestureEvent?> = lastGestureState

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
