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
import io.github.dianila68.gesturemacro.android.data.RecordedGestureStore
import io.github.dianila68.gesturemacro.core.actions.ActionDispatcher
import io.github.dianila68.gesturemacro.core.engine.BuiltinExecutorRegistry
import io.github.dianila68.gesturemacro.android.data.MacroStore
import io.github.dianila68.gesturemacro.core.engine.MacroEngine
import io.github.dianila68.gesturemacro.core.sensors.AndroidSensorStream
import io.github.dianila68.gesturemacro.core.sensors.GestureDetector
import io.github.dianila68.gesturemacro.core.sensors.GestureEvent
import io.github.dianila68.gesturemacro.core.sensors.ProximityWaveDetector
import io.github.dianila68.gesturemacro.core.sensors.SensorSample
import io.github.dianila68.gesturemacro.core.sensors.SensorType
import io.github.dianila68.gesturemacro.core.serialization.PatternKind
import io.github.dianila68.gesturemacro.core.triggers.RecordedGestureDetector
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
    private lateinit var recordedGestureStore: RecordedGestureStore
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
        recordedGestureStore = RecordedGestureStore(this)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startPipeline() {
        if (pipelineJob != null) return
        val stream = AndroidSensorStream(this@GestureCaptureService)
        // The active detector/sensor set tracks the *enabled* macros: when no enabled
        // macro uses a pattern, its detector never runs and its sensor is never
        // registered (NFR-1 — the gyroscope stays off unless a twist macro is on).
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

    /**
     * Detectors for the currently enabled trigger [patterns], merged over only the
     * sensors they need. Patterns without an available detector contribute nothing,
     * so an empty set registers no sensors at all. Each sample is paired with the
     * detector list that must see it (every detector is fed to keep its state in
     * step; detectors ignore samples from other sensors).
     *
     * RECORDED_GESTURE detectors are built per-macro (each macro has its own envelope),
     * loaded from [RecordedGestureStore] here.
     */
    private suspend fun detectorStream(
        stream: AndroidSensorStream,
        patterns: Set<PatternKind>,
    ): Flow<Pair<List<GestureDetector>, SensorSample>> {
        // Proximity wave needs the sensor's maximumRange for relative near/far classification.
        val proximityMaxRange = stream.sensorMaxRange(SensorType.PROXIMITY)
        val standardDetectors = patterns.mapNotNull { pattern ->
            if (pattern == PatternKind.RECORDED_GESTURE) return@mapNotNull null  // handled below
            val spec = TriggerLibrary.forPattern(pattern) ?: return@mapNotNull null
            if (pattern == PatternKind.PROXIMITY_WAVE && proximityMaxRange != null) {
                ProximityWaveDetector(maximumRange = proximityMaxRange)
            } else {
                spec.buildDetector()
            }
        }

        // Per-macro recorded gesture detectors: one RecordedGestureDetector per enabled macro
        // that references a stored envelope via trigger.recordedGestureId.
        val recordedDetectors: List<GestureDetector> = if (PatternKind.RECORDED_GESTURE in patterns) {
            MacroStore.macros.value
                .filter { it.enabled && it.trigger.pattern == PatternKind.RECORDED_GESTURE }
                .mapNotNull { macro ->
                    val envelopeId = macro.trigger.recordedGestureId ?: return@mapNotNull null
                    val envelope = recordedGestureStore.getEnvelope(envelopeId) ?: return@mapNotNull null
                    RecordedGestureDetector(envelope, envelopeId, macro.trigger.sensitivity)
                }
        } else emptyList()

        val detectors = standardDetectors + recordedDetectors
        if (detectors.isEmpty()) return emptyFlow()
        val streams = detectors.map { it.sensor }.distinct().map { sensorType ->
            stream.samples(sensorType, samplingPeriodUs = SAMPLING_PERIOD_US)
        }
        return merge(*streams.toTypedArray()).map { sample -> detectors to sample }
    }

    private fun onGesture(event: GestureEvent) {
        Log.i(TAG, "Gesture detected: ${event.pattern} (confidence ${event.confidence})")
        lastGestureState.value = event
        val fired = engine.match(event, MacroStore.macros.value)
        if (fired.isEmpty()) return
        wakeLock.openWindow(GESTURE_WINDOW_TIMEOUT_MS)
        for (macro in fired) {
            scope.launch {
                val results = dispatcher.run(macro)
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
        private const val WAKELOCK_TAG = "$TAG:gestureWindow"
        private const val GESTURE_WINDOW_TIMEOUT_MS = 5_000L
        const val ACTION_STOP = "io.github.dianila68.gesturemacro.action.STOP"
        const val CHANNEL_ID = "gesture_engine"
        const val NOTIFICATION_ID = 1001
        const val HEARTBEAT_INTERVAL_MS = 60_000L

        /** Settle window so rapid macro toggles re-subscribe sensors once, not per keystroke. */
        const val MACRO_CHANGE_DEBOUNCE_MS = 300L

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
