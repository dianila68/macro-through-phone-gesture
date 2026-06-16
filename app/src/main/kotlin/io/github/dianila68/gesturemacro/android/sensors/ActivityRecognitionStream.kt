package io.github.dianila68.gesturemacro.android.sensors

import android.content.Context
import io.github.dianila68.gesturemacro.core.sensors.GestureEvent
import io.github.dianila68.gesturemacro.core.sensors.GesturePattern
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * ticket-048: Wraps Google Play Services ActivityRecognitionClient.
 * Emits GestureEvents for activity transitions.
 *
 * Requires ACTIVITY_RECOGNITION permission (API 29+) and Google Play Services.
 * Full implementation registers ActivityTransitionRequest via PendingIntent +
 * BroadcastReceiver — this skeleton provides the mapping logic only.
 */
class ActivityRecognitionStream(private val context: Context) {

    fun activityEvents(): Flow<GestureEvent> = callbackFlow {
        // Full impl: register ActivityTransitionRequest + BroadcastReceiver here
        awaitClose { }
    }

    companion object {
        fun detectedActivityToPattern(type: Int): GesturePattern? = when (type) {
            0 -> GesturePattern.ACTIVITY_IN_VEHICLE    // DetectedActivity.IN_VEHICLE
            1 -> GesturePattern.ACTIVITY_ON_BICYCLE    // DetectedActivity.ON_BICYCLE
            3 -> GesturePattern.ACTIVITY_STILL         // DetectedActivity.STILL
            7 -> GesturePattern.ACTIVITY_WALKING       // DetectedActivity.WALKING
            8 -> GesturePattern.ACTIVITY_RUNNING       // DetectedActivity.RUNNING
            else -> null
        }
    }
}
