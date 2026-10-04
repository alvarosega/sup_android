package com.alvarosega.trackingventas.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alvarosega.trackingventas.data.local.dao.DeviceEventDao
import com.alvarosega.trackingventas.data.local.dao.LocationDao
import com.alvarosega.trackingventas.data.local.pref.SessionPreferences
import com.alvarosega.trackingventas.data.remote.AuthApiService
import com.alvarosega.trackingventas.data.remote.dto.DeviceEventPayload
import com.alvarosega.trackingventas.data.remote.dto.LocationPayload
import com.alvarosega.trackingventas.data.remote.dto.SyncLocationsRequest
import com.alvarosega.trackingventas.service.TrackingService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val tag = "SyncWorkerDebug"

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun locationDao(): LocationDao
        fun deviceEventDao(): DeviceEventDao
        fun authApiService(): AuthApiService
        fun sessionPreferences(): SessionPreferences
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                SyncWorkerEntryPoint::class.java
            )
            val locationDao = entryPoint.locationDao()
            val deviceEventDao = entryPoint.deviceEventDao()
            val apiService = entryPoint.authApiService()
            val sessionPreferences = entryPoint.sessionPreferences()

            val unsyncedLocations = locationDao.getUnsyncedLocations(limit = 100)
            val unsyncedEvents = deviceEventDao.getUnsyncedEvents(limit = 50)

            Log.d(tag, "Puntos locales pendientes: ${unsyncedLocations.size} ubicaciones, ${unsyncedEvents.size} eventos")

            if (unsyncedLocations.isEmpty() && unsyncedEvents.isEmpty()) {
                return Result.success()
            }

            val request = SyncLocationsRequest(
                locations = unsyncedLocations.map {
                    LocationPayload(
                        localId = it.id,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        accuracy = it.accuracy,
                        speed = it.speed,
                        batteryLevel = it.batteryLevel,
                        isMock = it.isMock,
                        isMoving = it.isMoving,
                        stepCount = it.stepCount,
                        motionVariance = it.motionVariance,
                        recordedAt = it.recordedAt
                    )
                },
                events = unsyncedEvents.map {
                    DeviceEventPayload(
                        localId = it.id,
                        eventType = it.eventType,
                        details = it.details,
                        recordedAt = it.recordedAt
                    )
                }
            )

            val response = apiService.syncTrackingData(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d(tag, "Sincronización exitosa: ${body.syncedLocations.size} ubicaciones marcadas en servidor")

                if (body.syncedLocations.isNotEmpty()) {
                    locationDao.markAsSynced(body.syncedLocations)
                }
                if (body.syncedEvents.isNotEmpty()) {
                    deviceEventDao.markAsSynced(body.syncedEvents)
                }

                val cutoff = getSevenDaysAgoCutoff()
                locationDao.purgeOldSyncedLocations(cutoff)
                deviceEventDao.purgeOldSyncedEvents(cutoff)

                Result.success()
            } else {
                val errorCode = response.code()
                val errorBody = response.errorBody()?.string() ?: ""
                Log.e(tag, "Fallo HTTP $errorCode: $errorBody")

                // Si el servidor rechaza por jornada cerrada o vencida
                if (errorCode == 403 && (errorBody.contains("STOP_TRACKING") || errorBody.contains("WORKDAY_CLOSED"))) {
                    Log.w(tag, "Servidor ordenó STOP_TRACKING. Deteniendo servicio y sesión local.")
                    sessionPreferences.setOperationState(false)
                    val stopIntent = Intent(applicationContext, TrackingService::class.java).apply {
                        action = TrackingService.ACTION_STOP
                    }
                    applicationContext.startService(stopIntent)
                    return Result.failure()
                }

                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(tag, "Excepción durante la sincronización", e)
            Result.retry()
        }
    }

    private fun getSevenDaysAgoCutoff(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(calendar.time)
    }
}