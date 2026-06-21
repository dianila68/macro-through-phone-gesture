package io.github.dianila68.gesturemacro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dianila68.gesturemacro.core.engine.EngineMetrics
import io.github.dianila68.gesturemacro.service.GestureCaptureService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardState(
    val gestureCount: Long,
    val dispatchCount: Long,
    val hitRate: String,
    val missCount: Long,
    val failureCount: Long,
    val latencyP50: String,
    val latencyP95: String,
    val isEngineRunning: Boolean,
) {
    companion object {
        val EMPTY = DashboardState(
            gestureCount = 0L,
            dispatchCount = 0L,
            hitRate = "—",
            missCount = 0L,
            failureCount = 0L,
            latencyP50 = "—",
            latencyP95 = "—",
            isEngineRunning = false,
        )
    }
}

/**
 * ticket-056: ViewModel for the macro analytics dashboard screen.
 * Combines EngineMetrics (latency / miss / failure counters) with the service
 * running state to produce a single DashboardState ready for display.
 */
class AnalyticsDashboardViewModel : ViewModel() {

    val dashboardState: StateFlow<DashboardState> = combine(
        GestureCaptureService.metrics,
        GestureCaptureService.running,
    ) { metrics, running -> metrics.toDashboardState(running) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardState.EMPTY,
        )

    companion object {
        private fun EngineMetrics.toDashboardState(running: Boolean): DashboardState {
            val hitRateStr = if (gestureCount == 0L) "—"
            else "%.1f%%".format(dispatchCount * 100.0 / gestureCount)
            return DashboardState(
                gestureCount = gestureCount,
                dispatchCount = dispatchCount,
                hitRate = hitRateStr,
                missCount = missedGestureCount,
                failureCount = executorFailureCount,
                latencyP50 = if (latencyP50Ms == 0L) "—" else "${latencyP50Ms} ms",
                latencyP95 = if (latencyP95Ms == 0L) "—" else "${latencyP95Ms} ms",
                isEngineRunning = running,
            )
        }
    }
}
