package com.alvarosega.trackingventas.ui.visita

import android.content.Context
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.alvarosega.trackingventas.data.local.dao.DeviceEventDao
import com.alvarosega.trackingventas.data.local.dao.PlanRuteoDao
import com.alvarosega.trackingventas.data.local.dao.VisitaDao
import com.alvarosega.trackingventas.data.local.entity.DeviceEventEntity
import com.alvarosega.trackingventas.data.local.entity.PlanRuteoEntity
import com.alvarosega.trackingventas.data.local.entity.VisitaEntity
import com.alvarosega.trackingventas.data.local.pref.SessionPreferences
import com.alvarosega.trackingventas.data.location.LocationClient
import com.alvarosega.trackingventas.data.time.ServerTimeManager
import com.alvarosega.trackingventas.util.ExifMetadataHelper
import com.alvarosega.trackingventas.worker.VisitaSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed interface ProximityUiState {
    object Idle : ProximityUiState
    object Loading : ProximityUiState
    object OperationNotActive : ProximityUiState
    data class OutOfRange(val message: String) : ProximityUiState
    data class SingleClientMatch(val client: PlanRuteoEntity, val distance: Float) : ProximityUiState
    data class MultipleClientsMatch(val clientsWithDistance: List<Pair<PlanRuteoEntity, Float>>) : ProximityUiState
}

@HiltViewModel
class VisitaViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionPreferences: SessionPreferences,
    private val locationClient: LocationClient,
    private val planRuteoDao: PlanRuteoDao,
    private val visitaDao: VisitaDao,
    private val deviceEventDao: DeviceEventDao,
    private val serverTimeManager: ServerTimeManager
) : ViewModel() {

    val isOperationActive: StateFlow<Boolean> = sessionPreferences.isOperationActive
    val activeRoute: StateFlow<String> = sessionPreferences.activeRoute

    private val _proximityState = MutableStateFlow<ProximityUiState>(ProximityUiState.Idle)
    val proximityState: StateFlow<ProximityUiState> = _proximityState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    var activePhotoFile: File? = null
    var capturedLocation: Location? = null

    companion object {
        private const val MAX_PROXIMITY_RADIUS_METERS = 150.0f // Actualizado de 20m a 35m
        private const val TAG = "VISITA_DEBUG"
    }

    fun startVisitaForClient(client: PlanRuteoEntity, distanceMeters: Float, lat: Double, lon: Double, accuracy: Float) {
        val loc = Location("FusedLocation").apply {
            latitude = lat
            longitude = lon
            this.accuracy = accuracy
            time = serverTimeManager.getSecureCurrentTimeMillis()
        }
        capturedLocation = loc
        _proximityState.value = ProximityUiState.SingleClientMatch(client, distanceMeters)
    }

    fun startVisitaOpportunity(lat: Double, lon: Double, accuracy: Float) {
        val loc = Location("FusedLocation").apply {
            latitude = lat
            longitude = lon
            this.accuracy = accuracy
            time = serverTimeManager.getSecureCurrentTimeMillis()
        }
        capturedLocation = loc
        _proximityState.value = ProximityUiState.OutOfRange("Registro de nuevo punto comercial (Cliente Oportunidad).")
    }

    fun evaluateProximity(currentDay: String) {
        if (!sessionPreferences.isOperationActive.value) {
            _proximityState.value = ProximityUiState.OperationNotActive
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _proximityState.value = ProximityUiState.Loading
            val currentLocation = locationClient.getCurrentLocation()

            if (currentLocation == null) {
                _proximityState.value = ProximityUiState.OutOfRange("Imposible obtener coordenadas GPS válidas.")
                return@launch
            }

            if (currentLocation.isFromMockProvider) {
                deviceEventDao.insertEvent(
                    DeviceEventEntity(
                        eventType = "MOCK_LOCATION_VISITA_BLOCKED",
                        details = "Intento de visita con ubicación simulada",
                        recordedAt = serverTimeManager.getBoliviaTimestamp()
                    )
                )
                _proximityState.value = ProximityUiState.OutOfRange("Ubicación simulada detectada. Acción bloqueada.")
                return@launch
            }

            capturedLocation = currentLocation
            val pendingClients = planRuteoDao.getPendingClientsByDay().first()

            val clientsInRange = pendingClients.mapNotNull { client ->
                val distanceResults = FloatArray(1)
                Location.distanceBetween(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    client.latitude,
                    client.longitude,
                    distanceResults
                )
                val distance = distanceResults[0]
                if (distance <= MAX_PROXIMITY_RADIUS_METERS) Pair(client, distance) else null
            }.sortedBy { it.second }

            _proximityState.value = when {
                clientsInRange.isEmpty() -> ProximityUiState.OutOfRange("Fuera del rango de clientes. Registro como Cliente Oportunidad.")
                clientsInRange.size == 1 -> ProximityUiState.SingleClientMatch(clientsInRange.first().first, clientsInRange.first().second)
                else -> ProximityUiState.MultipleClientsMatch(clientsInRange)
            }
        }
    }

    fun generatePrivatePhotoUri(): Uri {
        val dir = File(context.filesDir, "visitas_evidence").apply { mkdirs() }
        val secureNow = serverTimeManager.getSecureCurrentTimeMillis()
        val file = File(dir, "VISITA_${secureNow}.jpg")
        activePhotoFile = file
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun processCapturedPhoto(sellerCode: String, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = activePhotoFile
            val loc = capturedLocation
            if (file != null && loc != null && file.exists()) {
                val serverTime = serverTimeManager.getSecureCurrentTimeMillis()
                loc.time = serverTime
                ExifMetadataHelper.compressAndInjectMetadata(
                    photoFile = file,
                    location = loc,
                    sellerCode = sellerCode,
                    serverTimeMillis = serverTime
                )
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun saveVisita(
        selectedClient: PlanRuteoEntity?,
        isOpportunity: Boolean,
        opportunityClientName: String?,
        status: String?,
        comments: String?,
        onSuccess: () -> Unit
    ) {
        Log.d(TAG, "saveVisita invocado. OpActiva: ${sessionPreferences.isOperationActive.value}, Status: $status, isOpp: $isOpportunity")

        if (!sessionPreferences.isOperationActive.value) {
            _errorMessage.value = "Operación no iniciada. Debe iniciar operación primero."
            return
        }

        if (status.isNullOrBlank()) {
            _errorMessage.value = "Debe seleccionar un estado de atención manualmente."
            return
        }

        val file = activePhotoFile
        val loc = capturedLocation

        if (file == null || !file.exists()) {
            _errorMessage.value = "La fotografía es obligatoria."
            return
        }

        if (loc == null) {
            _errorMessage.value = "Lectura de GPS no disponible."
            return
        }

        val currentRoute = sessionPreferences.activeRoute.value

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val visitedAt = serverTimeManager.getBoliviaTimestamp()

                    val visita = VisitaEntity(
                        clientId = if (isOpportunity) null else selectedClient?.clientId,
                        route = currentRoute.ifBlank { "SIN_RUTA" },
                        status = status,
                        isOpportunity = isOpportunity,
                        opportunityClientName = if (isOpportunity) opportunityClientName else null,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracy = loc.accuracy,
                        photoPath = file.absolutePath,
                        comments = comments,
                        visitedAt = visitedAt,
                        isSynced = false
                    )

                    val insertedId = visitaDao.insertVisita(visita)
                    Log.d(TAG, "Visita guardada en Room local con ID: $insertedId con timestamp ServerTimeManager: $visitedAt")

                    Log.d(TAG, "Guardando visita -> isOpportunity: $isOpportunity, selectedClient: ${selectedClient?.clientId}")
                    if (!isOpportunity && selectedClient != null) {
                        planRuteoDao.markClientVisited(selectedClient.clientId)
                        Log.d(TAG, "Cliente #${selectedClient.clientId} marcado como isVisited = 1")
                    } else {
                        Log.w(TAG, "No se marcó como visitado en el plan de ruteo local.")
                    }

                    enqueueSyncWorker()
                }

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error crítico al registrar visita", e)
                _errorMessage.value = "Error al guardar: ${e.localizedMessage ?: "Excepción desconocida"}"
            }
        }
    }

    private fun enqueueSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWork = OneTimeWorkRequestBuilder<VisitaSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "VisitaSyncWorker",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncWork
        )
        Log.d(TAG, "WorkManager: Encolado VisitaSyncWorker")
    }

    fun clearError() {
        _errorMessage.value = null
    }
}