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
import com.google.gson.Gson
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

enum class MapMode {
    VISITAS, CENSO, INVENTARIO
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
    private val authRepository: com.alvarosega.trackingventas.data.repository.AuthRepository,
    private val moduleConfigManager: com.alvarosega.trackingventas.data.local.ModuleConfigManager,
    private val sessionPreferences: SessionPreferences
) : ViewModel() {

    val isOperationActive: StateFlow<Boolean> = sessionPreferences.isOperationActive
    val activeRoute: StateFlow<String> = sessionPreferences.activeRoute
    val userName: StateFlow<String> = MutableStateFlow(sessionPreferences.getUserNameDirect())
    val activeDay: StateFlow<String> = sessionPreferences.activeDay
    val workdayLimitTime: StateFlow<String?> = sessionPreferences.workdayLimitTime
    val activeModules: StateFlow<Map<String, Boolean>> = moduleConfigManager.activeModules

    private val _latestLocation = MutableStateFlow<LocationPoint?>(null)
    val latestLocation: StateFlow<LocationPoint?> = _latestLocation.asStateFlow()

    private val _selectedFilter = MutableStateFlow(MapFilterType.TODOS)
    val selectedFilter: StateFlow<MapFilterType> = _selectedFilter.asStateFlow()

    private val _mapMode = MutableStateFlow(MapMode.VISITAS)
    val mapMode: StateFlow<MapMode> = _mapMode.asStateFlow()

    fun setMapMode(mode: MapMode) {
        _mapMode.value = mode
    }

    val allClients: StateFlow<List<PlanRuteoEntity>> = planRuteoDao.getPendingClientsByDay()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredClients: StateFlow<List<PlanRuteoEntity>> = combine(allClients, _selectedFilter) { clients, filter ->
        when (filter) {
            MapFilterType.TODOS -> clients
            MapFilterType.PENDIENTES -> clients.filter { !it.isVisited }
            MapFilterType.VISITADOS -> clients.filter { it.isVisited }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showUnvisited = MutableStateFlow(false)
    val showUnvisited: StateFlow<Boolean> = _showUnvisited.asStateFlow()

    fun toggleShowUnvisited() {
        _showUnvisited.value = !_showUnvisited.value
    }

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

    private val _routeBoundary = MutableStateFlow<com.alvarosega.trackingventas.data.remote.dto.FronteraRutaDto?>(null)
    val routeBoundary: StateFlow<com.alvarosega.trackingventas.data.remote.dto.FronteraRutaDto?> = _routeBoundary.asStateFlow()

    fun fetchFronteraRuta(rutaOpcional: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val targetRoute = rutaOpcional ?: sessionPreferences.activeRoute.value.ifBlank { null }
                val response = visitaApiService.getFronteraRuta(targetRoute)
                Log.d("FRONTERA_DEBUG", "getFronteraRuta HTTP Code: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val rawJson = response.body()!!.string()
                    Log.d("FRONTERA_DEBUG", "Frontera JSON recibido: $rawJson")

                    val gson = Gson()
                    val parsedDto = try {
                        if (rawJson.trim().startsWith("[")) {
                            val listType = object : com.google.gson.reflect.TypeToken<List<com.alvarosega.trackingventas.data.remote.dto.PuntoDto>>() {}.type
                            val points: List<com.alvarosega.trackingventas.data.remote.dto.PuntoDto> = gson.fromJson(rawJson, listType) ?: emptyList()
                            com.alvarosega.trackingventas.data.remote.dto.FronteraRutaDto(puntos = points)
                        } else {
                            gson.fromJson(rawJson, com.alvarosega.trackingventas.data.remote.dto.FronteraRutaDto::class.java)
                        }
                    } catch (e: Exception) {
                        Log.e("FRONTERA_DEBUG", "Error parseando JSON de frontera", e)
                        null
                    }

                    if (parsedDto != null && parsedDto.puntos.isNotEmpty()) {
                        _routeBoundary.value = parsedDto
                        Log.d("FRONTERA_DEBUG", "Frontera procesada exitosamente con ${parsedDto.puntos.size} puntos")
                    } else {
                        Log.w("FRONTERA_DEBUG", "Frontera vacía o sin puntos válidos en el JSON")
                    }
                } else {
                    val err = response.errorBody()?.string()
                    Log.e("FRONTERA_DEBUG", "Error HTTP ${response.code()} consultando frontera de ruta: $err")
                }
            } catch (e: Exception) {
                Log.e("FRONTERA_DEBUG", "Excepcion consultando frontera de ruta", e)
            }
        }
    }

    init {
        refreshAppModules()
        checkWorkdayStatus()
        syncPlanRuteo()
        fetchFronteraRuta()
        forceRefreshGps()
    }

    fun refreshAppModules() {
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.syncAppModules()
        }
    }

    fun isModuleActive(code: String): Boolean {
        return moduleConfigManager.isModuleActive(code)
    }

    fun checkWorkdayStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                refreshAppModules()
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
                    withContext(Dispatchers.Main) {
                        onLocationFound?.invoke(freshLocation.latitude, freshLocation.longitude)
                    }
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
                // 1. Intentar consultar endpoint especializado api/visitas/clientes
                val responseVisitas = try { visitaApiService.getVisitasClientes() } catch (e: Exception) { null }
                val dtos = if (responseVisitas?.isSuccessful == true && !responseVisitas.body().isNullOrEmpty()) {
                    Log.d("SYNC_DEBUG", "Descargado vía api/visitas/clientes: ${responseVisitas.body()!!.size} clientes")
                    responseVisitas.body()!!
                } else {
                    val responsePlan = visitaApiService.getPlanRuteo()
                    Log.d("SYNC_DEBUG", "Descargado vía api/plan-ruteo HTTP: ${responsePlan.code()}")
                    if (responsePlan.isSuccessful && responsePlan.body() != null) {
                        responsePlan.body()!!
                    } else emptyList()
                }

                if (dtos.isNotEmpty()) {
                    Log.d("SYNC_DEBUG", "Clientes totales procesados del servidor: ${dtos.size}")
                    val entities = dtos.map { it.toEntity() }
                    val uniqueEntities = entities.distinctBy { it.clientId }

                    val unsyncedVisitedIds = visitaDao.getUnsyncedVisitedClientIds().toSet()
                    planRuteoDao.refreshPreservingVisited(uniqueEntities, unsyncedVisitedIds)
                    Log.d("SYNC_DEBUG", "Plan ruteo guardado en Room: ${uniqueEntities.size} clientes")
                } else {
                    _statusMessage.value = "Sin ruteo descargado del servidor"
                }
            } catch (e: Exception) {
                Log.e("SYNC_DEBUG", "Excepción durante syncPlanRuteo", e)
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