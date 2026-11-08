package com.alvarosega.trackingventas.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient
) : LocationClient {

    companion object {
        private const val TAG = "LOCATION_DEBUG"
        private const val MAX_LOCATION_AGE_MS = 30_000L // 30 segundos de antigüedad máxima
    }

    override fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.IO) {
        if (!isGpsEnabled()) return@withContext null

        // 1. Obtener la última ubicación de FusedLocation como base inmediata (0 ms de respuesta)
        val lastLoc = getLastKnownLocation()

        // Si la última ubicación es fresca (< 15s) y precisa (< 35m), retornarla directamente para AHORRAR BATERÍA
        if (lastLoc != null && isLocationFreshAndAccurate(lastLoc, maxAgeMs = 15_000L, maxAccuracyMeters = 35.0f)) {
            Log.d(TAG, "Ahorro de Batería: Usando ubicación en caché fresca y precisa (±${lastLoc.accuracy}m)")
            return@withContext calibrateGpsTime(lastLoc)
        }

        // 2. Intentar obtener una fijación activa rápida
        val freshLoc = getActiveGpsLocation()
        val bestLoc = freshLoc ?: lastLoc

        if (bestLoc != null) {
            Log.d(TAG, "Ubicación obtenida exitosamente: Lat ${bestLoc.latitude}, Lon ${bestLoc.longitude}, ±${bestLoc.accuracy}m")
            return@withContext calibrateGpsTime(bestLoc)
        }

        null
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(): Location? = suspendCancellableCoroutine { continuation ->
        client.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null && !com.alvarosega.trackingventas.util.LocationAntiSpoofingHelper.isMockLocation(loc, context)) {
                    continuation.resume(loc)
                } else {
                    continuation.resume(null)
                }
            }
            .addOnFailureListener { continuation.resume(null) }
            .addOnCanceledListener { continuation.resume(null) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getActiveGpsLocation(): Location? = withTimeoutOrNull(3_500L) {
        val cts = CancellationTokenSource()
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { cts.cancel() }

            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null && isLocationFreshAndAccurate(loc, maxAgeMs = MAX_LOCATION_AGE_MS, maxAccuracyMeters = 50.0f)) {
                        if (continuation.isActive) continuation.resume(loc)
                    } else {
                        // Solicitar actualizaciones activas si getCurrentLocation devolvió una de baja precisión
                        requestSingleUpdate(continuation)
                    }
                }
                .addOnFailureListener {
                    requestSingleUpdate(continuation)
                }
                .addOnCanceledListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleUpdate(continuation: kotlinx.coroutines.CancellableContinuation<Location?>) {
        if (!continuation.isActive) return

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMaxUpdates(1)
            .setDurationMillis(3000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                client.removeLocationUpdates(this)
                val loc = result.lastLocation
                if (continuation.isActive) {
                    continuation.resume(loc)
                }
            }
        }

        try {
            client.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
            continuation.invokeOnCancellation {
                client.removeLocationUpdates(callback)
            }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resume(null)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFallbackLastLocation(): Location? = suspendCancellableCoroutine { continuation ->
        client.lastLocation
            .addOnSuccessListener { loc -> continuation.resume(loc) }
            .addOnFailureListener { continuation.resume(null) }
    }

    private fun isLocationFreshAndAccurate(location: Location, maxAgeMs: Long, maxAccuracyMeters: Float): Boolean {
        if (com.alvarosega.trackingventas.util.LocationAntiSpoofingHelper.isMockLocation(location, context)) return false
        if (location.accuracy > maxAccuracyMeters) return false
        val ageMs = if (location.elapsedRealtimeNanos > 0) {
            (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
        } else {
            System.currentTimeMillis() - location.time
        }
        return ageMs in 0..maxAgeMs
    }

    private fun calibrateGpsTime(location: Location): Location {
        if (location.elapsedRealtimeNanos > 0) {
            val gpsEpochTime = System.currentTimeMillis() - SystemClock.elapsedRealtime() + (location.elapsedRealtimeNanos / 1_000_000)
            location.time = gpsEpochTime
        }
        return location
    }
}
