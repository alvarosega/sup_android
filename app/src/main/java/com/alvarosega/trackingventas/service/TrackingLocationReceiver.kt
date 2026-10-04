package com.alvarosega.trackingventas.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import com.alvarosega.trackingventas.data.local.dao.DeviceEventDao
import com.alvarosega.trackingventas.data.local.dao.LocationDao
import com.alvarosega.trackingventas.data.local.entity.DeviceEventEntity
import com.alvarosega.trackingventas.data.local.entity.LocationEntity
import com.alvarosega.trackingventas.data.local.pref.SessionPreferences
import com.alvarosega.trackingventas.data.time.ServerTimeManager
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TrackingLocationReceiver : BroadcastReceiver() {

    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var deviceEventDao: DeviceEventDao
    @Inject lateinit var serverTimeManager: ServerTimeManager
    @Inject lateinit var sessionPreferences: SessionPreferences

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        // Lectura atómica directa desde disco para el proceso :telemetry
        if (!sessionPreferences.isOperationActiveDirect()) {
            return
        }

        if (LocationResult.hasResult(intent)) {
            val locationResult = LocationResult.extractResult(intent) ?: return
            val location = locationResult.lastLocation ?: return

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val isMock = location.isFromMockProvider
                    val recordedAt = serverTimeManager.getBoliviaTimestamp()
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

                    locationDao.insertLocation(
                        LocationEntity(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            speed = location.speed,
                            batteryLevel = batteryLevel,
                            isMock = isMock,
                            recordedAt = recordedAt,
                            isSynced = false
                        )
                    )

                    if (isMock) {
                        deviceEventDao.insertEvent(
                            DeviceEventEntity(
                                eventType = "MOCK_LOCATION_DETECTED",
                                details = "Coordenada inyectada: Lat ${location.latitude}, Lon ${location.longitude}",
                                recordedAt = recordedAt
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e("TrackingReceiver", "Error insertando coordenada en Room", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}