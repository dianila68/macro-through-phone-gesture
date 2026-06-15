package io.github.dianila68.gesturemacro.core.actions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.dianila68.gesturemacro.core.serialization.LocationAlertAction
import io.github.dianila68.gesturemacro.core.serialization.MacroAction
import kotlinx.coroutines.delay

class LocationAlertExecutor(private val context: Context) : ActionExecutor {

    companion object {
        const val CHANNEL_ID = "location_alert"
        private const val NOTIFICATION_ID = 2001
    }

    override suspend fun execute(action: MacroAction): ExecResult {
        val alertAction = action as? LocationAlertAction
            ?: return ExecResult.Failure("LocationAlertExecutor received unexpected action type: ${action::class.simpleName}")

        // Check location permission
        val hasFineLoc = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLoc = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFineLoc && !hasCoarseLoc) {
            return ExecResult.Failure(
                "Location permission not granted (ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION required)",
                fatal = false,
            )
        }

        // Check SMS permission
        val hasSms = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasSms) {
            return ExecResult.Failure(
                "SEND_SMS permission not granted",
                fatal = false,
            )
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(notificationManager)

        // Show countdown notification
        if (alertAction.countdownSec > 0) {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Sending location alert")
                .setContentText(
                    "Sending location to ${alertAction.contactName} in ${alertAction.countdownSec}s — clear to cancel"
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setAutoCancel(false)
                .build()
            notificationManager.notify(NOTIFICATION_ID, notification)

            delay(alertAction.countdownSec * 1000L)
        }

        // Get location
        val location = getBestLastKnownLocation(hasFineLoc)

        // Build SMS body
        val smsBody = if (location != null) {
            val lat = location.latitude
            val lon = location.longitude
            "I need help. My location: https://maps.google.com/?q=$lat,$lon\n${alertAction.message}".trimEnd()
        } else {
            "I need help. Could not get location.\n${alertAction.message}".trimEnd()
        }

        // Send SMS
        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(alertAction.contactPhone, null, smsBody, null, null)
            notificationManager.cancel(NOTIFICATION_ID)
            ExecResult.Success
        } catch (e: Exception) {
            notificationManager.cancel(NOTIFICATION_ID)
            ExecResult.Failure("Failed to send SMS: ${e.message}")
        }
    }

    private fun getBestLastKnownLocation(hasFine: Boolean): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            val providers = buildList {
                if (hasFine) add(LocationManager.GPS_PROVIDER)
                add(LocationManager.NETWORK_PROVIDER)
            }
            providers
                .mapNotNull { provider ->
                    runCatching {
                        @Suppress("MissingPermission")
                        locationManager.getLastKnownLocation(provider)
                    }.getOrNull()
                }
                .maxByOrNull { it.accuracy }  // lower accuracy value = more precise; pick most recent as fallback
                ?.also { } // return it
                ?: run {
                    // If accuracy-based pick fails (e.g., all same accuracy), pick most recent
                    providers.mapNotNull { provider ->
                        runCatching {
                            @Suppress("MissingPermission")
                            locationManager.getLastKnownLocation(provider)
                        }.getOrNull()
                    }.maxByOrNull { it.time }
                }
        } catch (e: Exception) {
            null
        }
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location alert",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Countdown notification before sending your location via SMS"
            }
            manager.createNotificationChannel(channel)
        }
    }
}
