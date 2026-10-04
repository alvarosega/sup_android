package com.alvarosega.trackingventas.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.alvarosega.trackingventas.MainActivity
import com.alvarosega.trackingventas.data.local.dao.DeviceEventDao
import com.alvarosega.trackingventas.data.local.dao.LocationDao
import com.alvarosega.trackingventas.data.local.entity.DeviceEventEntity
import com.alvarosega.trackingventas.data.local.entity.LocationEntity
import com.alvarosega.trackingventas.data.local.pref.SessionPreferences
import com.alvarosega.trackingventas.data.location.LocationClient
import com.alvarosega.trackingventas.data.motion.MotionHardwareManager
import com.alvarosega.trackingventas.data.remote.AuthApiService
import com.alvarosega.trackingventas.data.time.ServerTimeManager
import com.alvarosega.trackingventas.worker.SyncWorker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : Service() {

    @Inject lateinit var locationClient: LocationClient
    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var deviceEventDao: DeviceEventDao
    @Inject lateinit var serverTimeManager: ServerTimeManager
    @Inject lateinit var sessionPreferences: SessionPreferences
    @Inject lateinit var motionHardwareManager: MotionHardwareManager
    @Inject lateinit var authApiService: AuthApiService

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var telemetryJob: Job? = null
    private var syncJob: Job? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "tracking_channel"
        private const val INTERVAL_MILLIS = 60000L
        private const val SYNC_INTERVAL_MILLIS = 60000L
        private const val TAG = "TrackingServiceDebug"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand recibido con acción: ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
            else -> {
                if (sessionPreferences.isOperationActiveDirect()) {
                    start()
                } else {
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Jornada de Ventas en Curso",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ruta activa"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun start() {
        Log.d(TAG, "Ejecutando start() en TrackingService")
        if (!locationClient.isGpsEnabled()) {
            Log.w(TAG, "GPS no habilitado. Abortando inicio de servicio.")
            stopSelf()
            return
        }

        createNotificationChannel()

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val routeName = sessionPreferences.getActiveRouteDirect().ifBlank { "Asignada" }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jornada en Curso — Ruta $routeName")
            .setContentText("-")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Inicia el muestreo de los sensores inerciales
        motionHardwareManager.startListening()

        serviceScope.launch {
            deviceEventDao.insertEvent(
                DeviceEventEntity(
                    eventType = "SESSION_STARTED",
                    details = "Operación iniciada (Ruta: $routeName)",
                    recordedAt = serverTimeManager.getBoliviaTimestamp()
                )
            )
        }

        telemetryJob?.cancel()
        telemetryJob = serviceScope.launch {
            // Pulso inicial inmediato: confiamos en que Laravel ya aprobó el inicio
            if (sessionPreferences.isOperationActiveDirect()) {
                captureLocationPulse()
            }

            while (isActive) {
                delay(INTERVAL_MILLIS)
                if (!sessionPreferences.isOperationActiveDirect()) {
                    stop()
                    break
                }

                // 1. Verificación contra Laravel (Fuente de la verdad)
                var syncSuccessful = false
                try {
                    val statusRes = authApiService.getWorkdayStatus()
                    if (statusRes.isSuccessful && statusRes.body() != null) {
                        syncSuccessful = true
                        val body = statusRes.body()!!

                        // Actualizar la hora límite que dicta Laravel
                        body.limit_time?.let { sessionPreferences.setWorkdayLimitTime(it) }

                        if (!body.is_active || body.action == "STOP_TRACKING") {
                            Log.w(TAG, "Laravel ordenó detener la jornada (status=${body.status}). Apagando servicio.")
                            sessionPreferences.setOperationState(false)
                            stop()
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Dispositivo offline o sin conexión al backend. Evaluando contingencia local.")
                }

                // 2. Modo Offline / Contingencia: si no hay red, evaluamos contra el límite que Laravel dejó guardado
                if (!syncSuccessful && isPastServerAssignedLimit()) {
                    Log.w(TAG, "Hora límite asignada por Laravel alcanzada sin conexión. Apagando servicio preventivamente.")
                    sessionPreferences.setOperationState(false)
                    stop()
                    break
                }

                captureLocationPulse()
            }
        }

        syncJob?.cancel()
        syncJob = serviceScope.launch {
            while (isActive) {
                delay(SYNC_INTERVAL_MILLIS)
                if (sessionPreferences.isOperationActiveDirect()) {
                    triggerSync()
                }
            }
        }
    }

    /**
     * Evalúa si ya pasó la hora límite enviada por Laravel (formato HH:mm:ss o HH:mm).
     * Solo opera en contingencia offline.
     */
    private fun isPastServerAssignedLimit(): Boolean {
        val limitStr = sessionPreferences.getWorkdayLimitTimeDirect() ?: return false
        return try {
            val parts = limitStr.split(":")
            val limitHour = parts[0].toInt()
            val limitMinute = parts[1].toInt()
            val limitTotalMinutes = limitHour * 60 + limitMinute

            val cal = Calendar.getInstance(TimeZone.getTimeZone("America/La_Paz"))
            val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

            currentMinutes >= limitTotalMinutes
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando hora límite de Laravel: $limitStr", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun captureLocationPulse() {
        try {
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    serviceScope.launch {
                        persistLocation(location)
                    }
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { fallbackLoc: Location? ->
                        if (fallbackLoc != null) {
                            serviceScope.launch {
                                persistLocation(fallbackLoc)
                            }
                        } else {
                            Log.w(TAG, "Imposible adquirir ubicación: ambas lecturas fueron null")
                        }
                    }
                }
            }.addOnFailureListener { ex ->
                Log.e(TAG, "Error en getCurrentLocation, intentando fallback", ex)
                fusedLocationClient.lastLocation.addOnSuccessListener { fallbackLoc: Location? ->
                    if (fallbackLoc != null) {
                        serviceScope.launch {
                            persistLocation(fallbackLoc)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en captureLocationPulse", e)
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun persistLocation(location: Location) {
        val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            location.isFromMockProvider
        }

        val motionSnapshot = motionHardwareManager.consumeMotionSnapshot()

        val recordedAt = serverTimeManager.getBoliviaTimestamp()
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val insertedId = locationDao.insertLocation(
            LocationEntity(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                speed = location.speed,
                batteryLevel = batteryLevel,
                isMock = isMock,
                isMoving = motionSnapshot.isMoving,
                stepCount = motionSnapshot.stepCount,
                motionVariance = motionSnapshot.motionVariance,
                recordedAt = recordedAt,
                isSynced = false
            )
        )

        Log.d(
            TAG,
            "Punto guardado -> ID: $insertedId | Moviéndose: ${motionSnapshot.isMoving} (Pasos: ${motionSnapshot.stepCount}, Varianza: ${"%.2f".format(motionSnapshot.motionVariance)})"
        )

        if (isMock) {
            deviceEventDao.insertEvent(
                DeviceEventEntity(
                    eventType = "MOCK_LOCATION_DETECTED",
                    details = "Lat ${location.latitude}, Lon ${location.longitude}",
                    recordedAt = recordedAt
                )
            )
        }
    }

    private fun triggerSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWork = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "TrackingPeriodicSync",
            ExistingWorkPolicy.KEEP,
            syncWork
        )
    }

    private fun stop() {
        motionHardwareManager.stopListening()
        telemetryJob?.cancel()
        syncJob?.cancel()
        serviceScope.launch {
            deviceEventDao.insertEvent(
                DeviceEventEntity(
                    eventType = "SESSION_ENDED",
                    details = "Operación finalizada",
                    recordedAt = serverTimeManager.getBoliviaTimestamp()
                )
            )
            triggerSync()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        motionHardwareManager.stopListening()
        serviceScope.cancel()
    }
}