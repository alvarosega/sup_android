package com.alvarosega.trackingventas.ui.tracking

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvarosega.trackingventas.data.local.dao.PlanRuteoDao
import com.alvarosega.trackingventas.data.local.dao.VisitaDao
import com.alvarosega.trackingventas.data.local.entity.PlanRuteoEntity
import com.alvarosega.trackingventas.data.local.pref.SessionPreferences
import com.alvarosega.trackingventas.data.location.LocationClient
import com.alvarosega.trackingventas.data.remote.AuthApiService
import com.alvarosega.trackingventas.data.remote.VisitaApiService
import com.alvarosega.trackingventas.data.remote.dto.OportunidadDto
import com.alvarosega.trackingventas.data.remote.dto.PlanRuteoDto
import com.alvarosega.trackingventas.data.remote.dto.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class MapFilterType {
    TODOS, PENDIENTES, VISITADOS
}

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f
)

@HiltViewModel
class MainMapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationClient: LocationClient,
    private val planRuteoDao: PlanRuteoDao,
    private val visitaDao: VisitaDao,
    private val visitaApiService: VisitaApiService,
    private val authApiService: AuthApiService,
    private val sessionPreferences: SessionPreferences
) : ViewModel() {

    val isOperationActive: StateFlow<Boolean> = sessionPreferences.isOperationActive
    val activeRoute: StateFlow<String> = sessionPreferences.activeRoute
    val workdayLimitTime: StateFlow<String?> = sessionPreferences.workdayLimitTime

    private val _latestLocation = MutableStateFlow<LocationPoint?>(null)
    val latestLocation: StateFlow<LocationPoint?> = _latestLocation.asStateFlow()

    private val _selectedFilter = MutableStateFlow(MapFilterType.TODOS)
    val selectedFilter: StateFlow<MapFilterType> = _selectedFilter.asStateFlow()

    val allClients: StateFlow<List<PlanRuteoEntity>> = planRuteoDao.getPendingClientsByDay()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredClients: StateFlow<List<PlanRuteoEntity>> = combine(allClients, _selectedFilter) { clients, filter ->
        when (filter) {
            MapFilterType.TODOS -> clients
            MapFilterType.PENDIENTES -> clients.filter { !it.isVisited }
            MapFilterType.VISITADOS -> clients.filter { it.isVisited }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- ESTADO EN MEMORIA PARA OPORTUNIDADES (SOLO LECTURA) ---
    private val _showOpportunities = MutableStateFlow(false)
    val showOpportunities: StateFlow<Boolean> = _showOpportunities.asStateFlow()

    private val _opportunities = MutableStateFlow<List<OportunidadDto>>(emptyList())
    val opportunities: StateFlow<List<OportunidadDto>> = _opportunities.asStateFlow()

    private val _isLoadingOpportunities = MutableStateFlow(false)
    val isLoadingOpportunities: StateFlow<Boolean> = _isLoadingOpportunities.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isRefreshingGps = MutableStateFlow(false)
    val isRefreshingGps: StateFlow<Boolean> = _isRefreshingGps.asStateFlow()

    init {
        checkWorkdayStatus()
        syncPlanRuteo()
        forceRefreshGps()
    }

    fun checkWorkdayStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = authApiService.getWorkdayStatus()
                Log.d("WORKDAY_DEBUG", "getWorkdayStatus HTTP Code: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val status = response.body()!!
                    Log.d("WORKDAY_DEBUG", "getWorkdayStatus result: isActive=${status.is_active}, status=${status.status}, limit=${status.limit_time}")
                    status.limit_time?.let { sessionPreferences.setWorkdayLimitTime(it) }
                    if (!status.is_active || status.action == "STOP_TRACKING" || status.within_schedule == false) {
                        sessionPreferences.setOperationState(false)
                    } else {
                        sessionPreferences.setOperationState(true)
                    }
                }
            } catch (e: Exception) {
                Log.e("WORKDAY_DEBUG", "Excepción verificando estado de jornada en backend", e)
            }
        }
    }

    fun toggleOpportunities() {
        val nextState = !_showOpportunities.value
        _showOpportunities.value = nextState
        Log.d("OPORTUNIDADES_DEBUG", "Toggle oportunidades: $nextState")

        if (nextState) {
            fetchOpportunities()
        }
    }

    private fun fetchOpportunities() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingOpportunities.value = true
            Log.d("OPORTUNIDADES_DEBUG", "Invocando api/oportunidades...")
            try {
                val response = visitaApiService.getOportunidades()
                Log.d("OPORTUNIDADES_DEBUG", "getOportunidades HTTP Code: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!.oportunidades
                    _opportunities.value = list
                    Log.d("OPORTUNIDADES_DEBUG", "Oportunidades cargadas en memoria: ${list.size}")
                    _statusMessage.value = "Oportunidades cargadas: ${list.size} puntos"
                } else {
                    val err = response.errorBody()?.string()
                    Log.e("OPORTUNIDADES_DEBUG", "Fallo HTTP ${response.code()}: $err")
                    _statusMessage.value = "Sin oportunidades (${response.code()})"
                }
            } catch (e: Exception) {
                Log.e("OPORTUNIDADES_DEBUG", "Excepcion al consultar oportunidades", e)
                _statusMessage.value = "Error oportunidades: ${e.localizedMessage ?: "Fallo de red"}"
            } finally {
                _isLoadingOpportunities.value = false
            }
        }
    }

    fun forceRefreshGps(onLocationFound: ((Double, Double) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingGps.value = true
            try {
                val freshLocation = locationClient.getCurrentLocation()
                if (freshLocation != null) {
                    _latestLocation.value = LocationPoint(
                        latitude = freshLocation.latitude,
                        longitude = freshLocation.longitude,
                        accuracy = freshLocation.accuracy
                    )
                    _statusMessage.value = "GPS actualizado (±${freshLocation.accuracy.toInt()}m)"

                    withContext(Dispatchers.Main) {
                        onLocationFound?.invoke(freshLocation.latitude, freshLocation.longitude)
                    }
                } else {
                    _statusMessage.value = "Esperando señal satelital GPS..."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error GPS: ${e.localizedMessage ?: "No disponible"}"
            } finally {
                _isRefreshingGps.value = false
            }
        }
    }

    fun setFilter(filter: MapFilterType) {
        _selectedFilter.value = filter
    }

    fun syncPlanRuteo() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("SYNC_DEBUG", "Iniciando syncPlanRuteo()...")
            try {
                val response = visitaApiService.getPlanRuteo()
                Log.d("SYNC_DEBUG", "getPlanRuteo HTTP: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val dtos = response.body()!!
                    Log.d("SYNC_DEBUG", "Clientes recibidos del servidor: ${dtos.size}")
                    dtos.forEachIndexed { index, dto ->
                        Log.d("SYNC_DEBUG", "DTO[$index]: clientId=${dto.clientId}, name=${dto.clientName}, lat=${dto.latitude}, lon=${dto.longitude}")
                    }

                    val entities = dtos.map { it.toEntity() }
                    val uniqueEntities = entities.distinctBy { it.clientId }
                    Log.d("SYNC_DEBUG", "Entidades únicas por clientId: ${uniqueEntities.size} de ${entities.size}")

                    val unsyncedVisitedIds = visitaDao.getUnsyncedVisitedClientIds().toSet()
                    planRuteoDao.refreshPreservingVisited(uniqueEntities, unsyncedVisitedIds)
                    _statusMessage.value = "Ruteo sincronizado (${uniqueEntities.size} puntos)"
                } else {
                    val err = response.errorBody()?.string()
                    Log.e("SYNC_DEBUG", "Error respuesta plan-ruteo ${response.code()}: $err")
                    _statusMessage.value = "Error al descargar ruteo: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("SYNC_DEBUG", "Excepcion durante syncPlanRuteo", e)
                _statusMessage.value = "Fallo de red al sincronizar ruteo"
            }
        }
    }
    fun isGpsEnabled(): Boolean = locationClient.isGpsEnabled()

    fun setOperationState(active: Boolean) {
        if (active) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val response = authApiService.startWorkday()
                    Log.d("WORKDAY_DEBUG", "startWorkday HTTP Code: ${response.code()}")

                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        body.limit_time?.let { sessionPreferences.setWorkdayLimitTime(it) }
                        if (body.is_active == false) {
                            sessionPreferences.setOperationState(false)
                            _statusMessage.value = body.message ?: "No autorizado para iniciar jornada"
                        } else {
                            sessionPreferences.setOperationState(true)
                            androidx.work.WorkManager.getInstance(context).cancelUniqueWork("TrackingPeriodicSync")
                            androidx.work.WorkManager.getInstance(context).cancelUniqueWork("TrackingForegroundSync")
                            _statusMessage.value = body.message ?: "Jornada iniciada correctamente"
                        }
                    } else {
                        sessionPreferences.setOperationState(false)
                        val errorBody = response.errorBody()?.string() ?: ""
                        Log.e("WORKDAY_DEBUG", "Fallo HTTP ${response.code()} en startWorkday: $errorBody")
                        val cleanMsg = try {
                            val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                            json.get("message")?.asString ?: "Fuera de horario laboral"
                        } catch (e: Exception) {
                            "Fuera de horario laboral configurado"
                        }
                        _statusMessage.value = cleanMsg
                    }
                } catch (e: Exception) {
                    sessionPreferences.setOperationState(false)
                    Log.e("WORKDAY_DEBUG", "Excepción al iniciar jornada en backend", e)
                    _statusMessage.value = "Error de red al iniciar jornada"
                }
            }
        } else {
            sessionPreferences.setOperationState(false)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val response = authApiService.stopWorkday()
                    Log.d("WORKDAY_DEBUG", "stopWorkday HTTP Code: ${response.code()}")
                    _statusMessage.value = "Jornada finalizada"
                } catch (e: Exception) {
                    Log.e("WORKDAY_DEBUG", "Excepción al finalizar jornada en backend", e)
                }
            }
        }
    }

    fun logout() {
        setOperationState(false)
        viewModelScope.launch(Dispatchers.IO) {
            planRuteoDao.clearPlanRuteo()
            sessionPreferences.clearSession()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}