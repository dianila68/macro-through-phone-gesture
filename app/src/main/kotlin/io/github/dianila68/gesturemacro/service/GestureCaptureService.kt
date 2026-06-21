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
import io.github.dianila68.gesturemacro.android.actions.AccessibilityExecutor
import io.github.dianila68.gesturemacro.android.actions.AccessibilityServiceGate
import io.github.dianila68.gesturemacro.android.actions.FlashlightExecutor
import io.github.dianila68.gesturemacro.android.actions.IntentExecutor
import io.github.dianila68.gesturemacro.android.actions.LocationAlertExecutor
import io.github.dianila68.gesturemacro.android.actions.MediaControlExecutor
import io.github.dianila68.gesturemacro.android.actions.SoundExecutor
import io.github.dianila68.gesturemacro.core.actions.ActionDispatcher
import io.github.dianila68.gesturemacro.core.engine.BuiltinExecutorRegistry
import io.github.dianila68.gesturemacro.android.data.MacroStore
import io.github.dianila68.gesturemacro.core.engine.EngineMetrics
import io.github.dianila68.gesturemacro.core.engine.EngineMetricsCollector
import io.github.dianila68.gesturemacro.core.engine.MacroEngine
import io.github.dianila68.gesturemacro.core.sensors.AndroidSensorStream
import io.github.dianila68.gesturemacro.core.sensors.GestureDetector
import io.github.dianila68.gesturemacro.core.sensors.GestureEvent
import io.github.dianila68.gesturemacro.core.sensors.ProximityWaveDetector
import io.github.dianila68.gesturemacro.core.sensors.SensorSample
import io.github.dianila68.gesturemacro.core.sensors.SensorType
import io.github.dianila68.gesturemacro.core.serialization.PatternKind
import io.github.dianila68.gesturemacro.core.triggers.TriggerLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Persistent foreground service hosting the capture pipeline. Holds no detection
 * logic itself (DESIGN.md): it owns the lifecycle of the sensor module and engine.
 */
class GestureCaptureService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var heartbeat: Heartbeat
    private lateinit var wakeLock: WakeLockGuard
    private lateinit var dispatcher: ActionDispatcher
    private lateinit var engine: MacroEngine
    private var pipelineJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        heartbeat = Heartbeat(this)
        wakeLock = WakeLockGuard(this, WAKELOCK_TAG)
        val registry = BuiltinExecutorRegistry(
            flashlight = FlashlightExecutor(this),
            mediaControl = MediaControlExecutor(this),
            intent = IntentExecutor(this),
            accessibility = AccessibilityExecutor(AccessibilityServiceGate { MacroAccessibilityService.instance.value }),
            sound = SoundExecutor(this),
            locationAlert = LocationAlertExecutor(this),
        )
        dispatcher = ActionDispatcher(registry)
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        engine = MacroEngine(screenOn = { powerManager.isInteractive })
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                heartbeat.recordStop()
                runningState.value = false
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_ARM -> {
                setArmed(this, true)
                refreshNotification()
                return START_NOT_STICKY
            }
            ACTION_DISARM -> {
                setArmed(this, false)
                refreshNotification()
                return START_NOT_STICKY
            }
        }
        startInForeground()
        startHeartbeatLoop()
        startPipeline()
        runningState.value = true
        return START_STICKY
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startPipeline() {
        if (pipelineJob != null) return
        val stream = AndroidSensorStream(this@GestureCaptureService)
        pipelineJob = scope.launch {
            MacroStore.macros
                .map { macros -> macros.filter { it.enabled }.map { it.trigger.pattern }.toSet() }
                .distinctUntilChanged()
                .debounce(MACRO_CHANGE_DEBOUNCE_MS)
                .flatMapLatest { patterns -> detectorStream(stream, patterns) }
                .catch { e -> Log.w(TAG, "Sensor stream ended: ${e.message}") }
                .collect { (detectors, sample) ->
                    for (detector in detectors) {
                        val event = detector.feed(sample) ?: continue
                        onGesture(event)
                    }
                }
        }
    }

    private fun detectorStream(
        stream: AndroidSensorStream,
        patterns: Set<PatternKind>,
    ): Flow<Pair<List<GestureDetector>, SensorSample>> {
        val proximityMaxRange = stream.sensorMaxRange(SensorType.PROXIMITY)
        val detectors = patterns.mapNotNull { pattern ->
            val spec = TriggerLibrary.forPattern(pattern) ?: return@mapNotNull null
            if (pattern == PatternKind.PROXIMITY_WAVE && proximityMaxRange != null) {
                ProximityWaveDetector(maximumRange = proximityMaxRange)
            } else {
                spec.buildDetector()
            }
        }
        if (detectors.isEmpty()) return emptyFlow()
        val streams = detectors.map { it.sensor }.distinct().map { sensorType ->
            stream.samples(sensorType, samplingPeriodUs = SAMPLING_PERIOD_US)
        }
        return merge(*streams.toTypedArray()).map { sample -> detectors to sample }
    }

    private fun onGesture(event: GestureEvent) {
        if (!isArmed(this)) return  // service alive but user has disarmed gesture processing
        Log.i(TAG, "Gesture detected: ${event.pattern} (confidence ${event.confidence})")
        lastGestureState.value = event
        metricsCollector.recordGesture()
        val receivedAt = System.currentTimeMillis()
        val fired = engine.match(event, MacroStore.macros.value)
        if (fired.isEmpty()) {
            metricsCollector.recordMissed()
            return
        }
        wakeLock.openWindow(GESTURE_WINDOW_TIMEOUT_MS)
        for (macro in fired) {
            scope.launch {
                val results = dispatcher.run(macro)
                metricsCollector.recordDispatch(receivedAt, System.currentTimeMillis())
                MacroStore.recordExecution(macro, results)
                Log.i(TAG, "Macro '${macro.name}' executed: $results")
            }
        }
        scope.launch {
            delay(GESTURE_WINDOW_TIMEOUT_MS)
            wakeLock.closeWindow()
        }
    }

    override fun onDestroy() {
        runningState.value = false
        wakeLock.closeWindow()
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
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(isArmed(this)), type)
    }

    private fun refreshNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(isArmed(this)))
    }

    private fun startHeartbeatLoop() {
        scope.launch {
            while (isActive) {
                heartbeat.beat()
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private fun buildNotification(armed: Boolean): android.app.Notification {
        val stopIntent = Intent(this, GestureCaptureService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val toggleAction = if (armed) ACTION_DISARM else ACTION_ARM
        val toggleLabel  = if (armed) "Disarm" else "Arm"
        val toggleIntent = Intent(this, GestureCaptureService::class.java).setAction(toggleAction)
        val togglePending = PendingIntent.getService(
            this, REQUEST_TOGGLE_NOTIFICATION,
            toggleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val statusText = if (armed) "Listening for hardware gestures"
                         else "Engine paused — tap Arm to resume"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GestureMacro engine running")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, toggleLabel, togglePending)
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
        private const val WAKELOCK_TAG = "$TAG:gestureWindow"
        private const val GESTURE_WINDOW_TIMEOUT_MS = 5_000L
        private const val PREFS_NAME = "macro_widget_prefs"
        private const val KEY_ARMED  = "armed"
        private const val REQUEST_TOGGLE_NOTIFICATION = 101
        const val ACTION_STOP   = "io.github.dianila68.gesturemacro.action.STOP"
        const val ACTION_ARM    = "io.github.dianila68.gesturemacro.action.ARM"
        const val ACTION_DISARM = "io.github.dianila68.gesturemacro.action.DISARM"
        const val CHANNEL_ID = "gesture_engine"
        const val NOTIFICATION_ID = 1001
        const val HEARTBEAT_INTERVAL_MS = 60_000L
        const val MACRO_CHANGE_DEBOUNCE_MS = 300L
        const val SAMPLING_PERIOD_US = 20_000

        private val runningState = MutableStateFlow(false)
        private val lastGestureState = MutableStateFlow<GestureEvent?>(null)
        private val metricsCollector = EngineMetricsCollector()

        val running: StateFlow<Boolean> = runningState
        val lastGesture: StateFlow<GestureEvent?> = lastGestureState
        val metrics: StateFlow<EngineMetrics> = metricsCollector.metrics

        fun isArmed(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ARMED, true)  // default: armed on first launch

        fun setArmed(context: Context, armed: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ARMED, armed).apply()
        }

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
