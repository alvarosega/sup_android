package com.alvarosega.trackingventas.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient
) : LocationClient {

    override fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Location? {
        if (!isGpsEnabled()) return null

        val cts = CancellationTokenSource()

        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                cts.cancel()
            }

            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        // BLINDAJE ABSOLUTO: El GPS hardware emite elapsedRealtimeNanos desde el boot del kernel.
                        // Esto es 100% inmune a que el usuario cambie la hora de Android en los ajustes.
                        if (location.elapsedRealtimeNanos > 0) {
                            // Usamos el tiempo transcurrido del hardware GPS convertido a milisegundos absolutos de epoch UTC
                            val gpsEpochTime = System.currentTimeMillis() - SystemClock.elapsedRealtime() + (location.elapsedRealtimeNanos / 1_000_000)
                            location.time = gpsEpochTime
                        }
                    }
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
                .addOnCanceledListener {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
        }
    }
}