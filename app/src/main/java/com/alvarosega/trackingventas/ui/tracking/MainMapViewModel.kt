package com.alvarosega.trackingventas.ui.tracking

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvarosega.trackingventas.data.local.dao.LocationDao
import com.alvarosega.trackingventas.data.local.dao.PlanRuteoDao
import com.alvarosega.trackingventas.data.local.entity.LocationEntity
import com.alvarosega.trackingventas.data.local.entity.PlanRuteoEntity
import com.alvarosega.trackingventas.data.local.pref.SessionPreferences
import com.alvarosega.trackingventas.data.location.LocationClient
import com.alvarosega.trackingventas.data.remote.AuthApiService
import com.alvarosega.trackingventas.data.remote.VisitaApiService
import com.alvarosega.trackingventas.data.remote.dto.OportunidadDto
import com.alvarosega.trackingventas.data.remote.dto.PlanRuteoDto
import com.alvarosega.trackingventas.service.TrackingService
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

@HiltViewModel
class MainMapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationClient: LocationClient,
    locationDao: LocationDao,
    private val planRuteoDao: PlanRuteoDao,
    private val visitaApiService: VisitaApiService,
    private val authApiService: AuthApiService,
    private val sessionPreferences: SessionPreferences
) : ViewModel() {

    val isOperationActive: StateFlow<Boolean> = sessionPreferences.isOperationActive
    val activeRoute: StateFlow<String> = sessionPreferences.activeRoute

    private val _manualLocation = MutableStateFlow<LocationEntity?>(null)

    val latestLocation: StateFlow<LocationEntity?> = combine(
        locationDao.getLatestLocation(),
        _manualLocation
    ) { dbLoc, manualLoc ->
        manualLoc ?: dbLoc
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
        syncPlanRuteo()
        forceRefreshGps()
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
                    val updatedEntity = LocationEntity(
                        latitude = freshLocation.latitude,
                        longitude = freshLocation.longitude,
                        accuracy = freshLocation.accuracy,
                        speed = freshLocation.speed,
                        batteryLevel = 0,
                        isMock = freshLocation.isFromMockProvider,
                        recordedAt = "",
                        isSynced = true
                    )
                    _manualLocation.value = updatedEntity
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

                    val entities = dtos.map { dto: PlanRuteoDto ->
                        PlanRuteoEntity(
                            clientId = dto.clientId,
                            clientName = dto.clientName ?: "Cliente Sin Nombre",
                            route = dto.route ?: "Sin Ruta",
                            day = dto.day ?: "Sin Asignar",
                            address = dto.address ?: "Sin dirección",
                            latitude = dto.latitude ?: 0.0,
                            longitude = dto.longitude ?: 0.0,
                            status = dto.status ?: "Activo",
                            isVisited = false,
                            tipoNegocio = dto.tipoNegocio,
                            zona = dto.zona,
                            contacto = dto.contacto,
                            telefono = dto.telefono,
                            celular = dto.celular,
                            referencia = dto.referencia,
                            nombreFactura = dto.nombreFactura,
                            nit = dto.nit
                        )
                    }
                    planRuteoDao.refreshPreservingVisited(entities)
                    Log.d("SYNC_DEBUG", "Entidades insertadas en Room: ${entities.size}")
                    _statusMessage.value = "Ruteo sincronizado (${entities.size} puntos)"
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

                    if (response.isSuccessful) {
                        sessionPreferences.setOperationState(true)
                        startTrackingService()
                        _statusMessage.value = "Jornada iniciada correctamente"
                    } else {
                        val errorBody = response.errorBody()?.string() ?: ""
                        Log.e("WORKDAY_DEBUG", "Fallo HTTP ${response.code()} en startWorkday: $errorBody")
                        _statusMessage.value = "No autorizado: $errorBody"
                    }
                } catch (e: Exception) {
                    Log.e("WORKDAY_DEBUG", "Excepción al iniciar jornada en backend", e)
                    _statusMessage.value = "Error de red al iniciar jornada"
                }
            }
        } else {
            sessionPreferences.setOperationState(false)
            stopTrackingService()
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

    private fun startTrackingService() {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopTrackingService() {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP
        }
        context.startService(intent)
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