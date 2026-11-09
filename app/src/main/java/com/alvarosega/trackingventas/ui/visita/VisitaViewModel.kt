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
import com.alvarosega.trackingventas.data.local.ModuleConfigManager
import com.alvarosega.trackingventas.data.local.dao.DeviceEventDao
import com.alvarosega.trackingventas.data.local.dao.InventarioClienteDao
import com.alvarosega.trackingventas.data.local.dao.PlanRuteoDao
import com.alvarosega.trackingventas.data.local.dao.VisitaDao
import com.alvarosega.trackingventas.data.local.entity.DeviceEventEntity
import com.alvarosega.trackingventas.data.local.entity.InventarioClienteEntity
import com.alvarosega.trackingventas.data.local.entity.PlanRuteoEntity
import com.alvarosega.trackingventas.data.local.entity.VisitaEntity
import com.alvarosega.trackingventas.data.local.pref.SessionPreferences
import com.alvarosega.trackingventas.data.location.LocationClient
import com.alvarosega.trackingventas.data.remote.VisitaApiService
import com.alvarosega.trackingventas.data.remote.dto.InventarioItemPayloadDto
import com.alvarosega.trackingventas.data.remote.dto.InventarioProductoHistorialDto
import com.alvarosega.trackingventas.data.remote.dto.ProductoInventarioDto
import com.alvarosega.trackingventas.data.time.ServerTimeManager
import com.alvarosega.trackingventas.util.ExifMetadataHelper
import com.alvarosega.trackingventas.worker.InventarioSyncWorker
import com.alvarosega.trackingventas.worker.VisitaSyncWorker
import com.google.gson.Gson
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
import java.util.UUID
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
    private val serverTimeManager: ServerTimeManager,
    private val apiService: VisitaApiService,
    private val inventarioClienteDao: InventarioClienteDao,
    private val moduleConfigManager: ModuleConfigManager
) : ViewModel() {

    val isOperationActive: StateFlow<Boolean> = sessionPreferences.isOperationActive
    val activeRoute: StateFlow<String> = sessionPreferences.activeRoute
    val activeModules: StateFlow<Map<String, Boolean>> = moduleConfigManager.activeModules

    private val _proximityState = MutableStateFlow<ProximityUiState>(ProximityUiState.Idle)
    val proximityState: StateFlow<ProximityUiState> = _proximityState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _capturedPhotos = MutableStateFlow<List<File>>(emptyList())
    val capturedPhotos: StateFlow<List<File>> = _capturedPhotos.asStateFlow()

    // ESTADO PARA INVENTARIO EN PDV INTEGRADO EN VISITA
    private val _productosHistorial = MutableStateFlow<List<InventarioProductoHistorialDto>>(emptyList())
    val productosHistorial: StateFlow<List<InventarioProductoHistorialDto>> = _productosHistorial.asStateFlow()

    // El mapa almacena la cadena digitada para cada producto por su clave única (Key -> String)
    private val _stockMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val stockMap: StateFlow<Map<String, String>> = _stockMap.asStateFlow()

    private val _isLoadingProductos = MutableStateFlow(false)
    val isLoadingProductos: StateFlow<Boolean> = _isLoadingProductos.asStateFlow()

    private val _sugeridoState = MutableStateFlow<com.alvarosega.trackingventas.data.remote.dto.SugeridoResponseDto?>(null)
    val sugeridoState: StateFlow<com.alvarosega.trackingventas.data.remote.dto.SugeridoResponseDto?> = _sugeridoState.asStateFlow()

    private val _isLoadingSugerido = MutableStateFlow(false)
    val isLoadingSugerido: StateFlow<Boolean> = _isLoadingSugerido.asStateFlow()

    var activePhotoFile: File? = null
    var capturedLocation: Location? = null

    companion object {
        private const val MAX_PROXIMITY_RADIUS_METERS = 150.0f
        private const val TAG = "VISITA_DEBUG"
    }

    fun isModuleActive(code: String): Boolean {
        return moduleConfigManager.isModuleActive(code)
    }

    fun getProdKey(prod: InventarioProductoHistorialDto, idx: Int): String {
        return if (!prod.codigo.isNullOrBlank()) {
            prod.codigo!!
        } else if (prod.productoId != null && prod.productoId!! > 0L) {
            prod.productoId!!.toString()
        } else {
            "PROD_IDX_$idx"
        }
    }

    fun startVisitaForClient(client: PlanRuteoEntity, distanceMeters: Float, lat: Double, lon: Double, accuracy: Float) {
        _capturedPhotos.value = emptyList()
        _stockMap.value = emptyMap()
        activePhotoFile = null
        val loc = Location("FusedLocation").apply {
            latitude = lat
            longitude = lon
            this.accuracy = accuracy
            time = serverTimeManager.getSecureCurrentTimeMillis()
        }
        capturedLocation = loc
        _proximityState.value = ProximityUiState.SingleClientMatch(client, distanceMeters)

        if (isModuleActive("inventario_cliente")) {
            fetchHistorialProductos(client.clientId)
        }
        fetchSugeridoCliente(client.clientId)
    }

    fun fetchHistorialProductos(clienteId: Long) {
        fetchSugeridoCliente(clienteId)
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingProductos.value = true
            try {
                val dias = moduleConfigManager.getIntConfig("inventario_cliente", "dias_historial", 30)
                val response = apiService.getHistorialProductosCliente(clienteId.toString(), dias)
                Log.d(TAG, "getHistorialProductosCliente HTTP Code: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val rawList = response.body()!!.productos ?: emptyList()
                    val sortedList = rawList.sortedWith(
                        compareByDescending<ProductoInventarioDto> {
                            (it.historialCompras?.size ?: 0).coerceAtLeast(it.frecuenciaCompra ?: 0)
                        }
                        .thenByDescending {
                            it.montoHistorico ?: (it.totalComprado ?: 0).toDouble()
                        }
                        .thenBy { it.producto ?: "" }
                    )
                    _productosHistorial.value = sortedList
                    _stockMap.value = emptyMap() // Por defecto es vacío/sin censar
                    Log.d(TAG, "Productos de inventario cargados exitosamente para cliente #$clienteId: ${sortedList.size} productos")
                } else {
                    val err = response.errorBody()?.string()
                    Log.e(TAG, "Error HTTP ${response.code()} cargando historial de productos para cliente #$clienteId: $err")
                    _productosHistorial.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando productos de inventario en visita", e)
                _productosHistorial.value = emptyList()
            } finally {
                _isLoadingProductos.value = false
            }
        }
    }

    fun fetchSugeridoCliente(clienteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingSugerido.value = true
            try {
                val response = apiService.getSugeridoCliente(clienteId)
                Log.d(TAG, "getSugeridoCliente HTTP Code: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _sugeridoState.value = body
                    Log.d(TAG, "Sugerido cargado para cliente #$clienteId: Total Sugerido Bs. ${body.totalSugerido}")
                } else {
                    val err = response.errorBody()?.string()
                    Log.e(TAG, "Error HTTP ${response.code()} cargando sugerido para cliente #$clienteId: $err")
                    _sugeridoState.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción cargando sugerido de cliente #$clienteId", e)
                _sugeridoState.value = null
            } finally {
                _isLoadingSugerido.value = false
            }
        }
    }

    fun updateStockInput(key: String, inputVal: String) {
        val filtered = inputVal.filter { it.isDigit() }
        val currentMap = _stockMap.value.toMutableMap()
        currentMap[key] = filtered
        _stockMap.value = currentMap
    }

    fun adjustStockFisico(key: String, delta: Int) {
        val currentMap = _stockMap.value.toMutableMap()
        val currentVal = currentMap[key]?.toIntOrNull() ?: 0
        val newVal = (currentVal + delta).coerceAtLeast(0)
        currentMap[key] = "$newVal"
        _stockMap.value = currentMap
    }

    fun resetForm() {
        _capturedPhotos.value = emptyList()
        _productosHistorial.value = emptyList()
        _stockMap.value = emptyMap()
        activePhotoFile = null
        capturedLocation = null
        _proximityState.value = ProximityUiState.Idle
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

            if (com.alvarosega.trackingventas.util.LocationAntiSpoofingHelper.isMockLocation(currentLocation, context)) {
                deviceEventDao.insertEvent(
                    DeviceEventEntity(
                        eventType = "MOCK_LOCATION_VISITA_BLOCKED",
                        details = "Intento de visita con ubicación simulada o aplicación Fake GPS",
                        recordedAt = serverTimeManager.getBoliviaTimestamp()
                    )
                )
                _proximityState.value = ProximityUiState.OutOfRange("Ubicación simulada detectada. Acción bloqueada por seguridad.")
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

    fun createPrivatePhotoFile(): File {
        val dir = File(context.filesDir, "visitas_evidence").apply { mkdirs() }
        val secureNow = serverTimeManager.getSecureCurrentTimeMillis()
        val count = _capturedPhotos.value.size + 1
        val file = File(dir, "VISITA_${secureNow}_$count.jpg")
        activePhotoFile = file
        return file
    }

    fun onPhotoTakenSuccessfully() {
        val file = activePhotoFile
        if (file != null && file.exists() && file.length() > 0) {
            if (!_capturedPhotos.value.contains(file)) {
                _capturedPhotos.value = _capturedPhotos.value + file
            }
        }
    }

    fun getOrCreatePrivatePhotoFile(): File {
        return activePhotoFile ?: createPrivatePhotoFile()
    }

    fun removeCapturedPhoto(file: File) {
        _capturedPhotos.value = _capturedPhotos.value.filter { it != file }
        if (file.exists()) {
            file.delete()
        }
        if (activePhotoFile == file) {
            activePhotoFile = _capturedPhotos.value.lastOrNull()
        }
    }

    fun generatePrivatePhotoUri(): Uri {
        val file = getOrCreatePrivatePhotoFile()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun processCapturedPhoto(sellerCode: String, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = activePhotoFile
            val loc = capturedLocation
            if (file != null && file.exists() && file.length() > 0) {
                if (loc != null) {
                    val serverTime = serverTimeManager.getSecureCurrentTimeMillis()
                    loc.time = serverTime
                    ExifMetadataHelper.compressAndInjectMetadata(
                        photoFile = file,
                        location = loc,
                        sellerCode = sellerCode,
                        serverTimeMillis = serverTime
                    )
                }
            }
            withContext(Dispatchers.Main) {
                val currentFile = activePhotoFile
                if (currentFile != null && currentFile.exists() && currentFile.length() > 0) {
                    if (!_capturedPhotos.value.contains(currentFile)) {
                        _capturedPhotos.value = _capturedPhotos.value + currentFile
                    } else {
                        _capturedPhotos.value = _capturedPhotos.value.toList()
                    }
                }
                onComplete()
            }
        }
    }

    fun isGpsEnabled(): Boolean = locationClient.isGpsEnabled()

    fun saveVisita(
        selectedClient: PlanRuteoEntity?,
        isOpportunity: Boolean,
        opportunityClientName: String?,
        status: String?,
        comments: String?,
        distanceToClientMeters: Double? = null,
        onSuccess: () -> Unit
    ) {
        Log.d(TAG, "saveVisita invocado. Status: $status, isOpp: $isOpportunity")

        if (!locationClient.isGpsEnabled()) {
            _errorMessage.value = "El GPS debe estar encendido para guardar la visita."
            return
        }

        if (!sessionPreferences.isOperationActive.value) {
            _errorMessage.value = "Operación no iniciada. Debe iniciar operación primero."
            return
        }

        if (status.isNullOrBlank()) {
            _errorMessage.value = "Debe seleccionar un estado de atención manualmente."
            return
        }

        val photos = _capturedPhotos.value
        val loc = capturedLocation

        if (photos.isEmpty() || !photos.any { it.exists() }) {
            _errorMessage.value = "Debe tomar al menos una fotografía de evidencia."
            return
        }

        if (loc == null) {
            _errorMessage.value = "Lectura de GPS no disponible."
            return
        }

        val currentRoute = sessionPreferences.activeRoute.value
        val photoPathsString = photos.filter { it.exists() }.joinToString("|") { it.absolutePath }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val visitedAt = serverTimeManager.getBoliviaTimestamp()

                    // 1. Guardar la Visita
                    val visita = VisitaEntity(
                        uuid = UUID.randomUUID().toString(),
                        clientId = if (isOpportunity) null else selectedClient?.clientId,
                        route = currentRoute.ifBlank { "SIN_RUTA" },
                        status = status,
                        isOpportunity = isOpportunity,
                        opportunityClientName = if (isOpportunity) opportunityClientName else null,
                        distanceToClient = distanceToClientMeters,
                        isMockLocation = loc.isFromMockProvider,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracy = loc.accuracy,
                        photoPath = photoPathsString,
                        comments = comments,
                        visitedAt = visitedAt,
                        isSynced = false
                    )

                    val insertedId = visitaDao.insertVisita(visita)
                    Log.d(TAG, "Visita guardada en Room local ID: $insertedId")

                    // 2. Guardar Inventario en PDV si hay productos cargados
                    val prods = _productosHistorial.value
                    if (prods.isNotEmpty() && selectedClient != null) {
                        val itemsPayload = prods.mapIndexed { idx, prod ->
                            val key = getProdKey(prod, idx)
                            val valorStock = _stockMap.value[key]?.toIntOrNull() ?: 0
                            val prodId = if (prod.productoId != null && prod.productoId > 0L) prod.productoId else (idx + 1L)
                            InventarioItemPayloadDto(
                                productoId = prodId,
                                codigoProducto = prod.codigo ?: "",
                                productoNombre = prod.producto ?: "Producto",
                                categoria = prod.categoria ?: "General",
                                stockFisico = valorStock
                            )
                        }
                        val itemsJson = Gson().toJson(itemsPayload)

                        val invEntity = InventarioClienteEntity(
                            uuid = UUID.randomUUID().toString(),
                            clienteId = selectedClient.clientId.toString(),
                            clienteNombre = selectedClient.clientName,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracy = loc.accuracy,
                            isMockLocation = loc.isFromMockProvider,
                            comments = comments,
                            itemsJson = itemsJson,
                            createdAt = visitedAt,
                            isSynced = false
                        )

                        val invInsertedId = inventarioClienteDao.insertInventario(invEntity)
                        Log.d(TAG, "INVENTARIO_INSERT: Registro guardado en inventario_cliente_local con ID: $invInsertedId, UUID: ${invEntity.uuid}, Items: ${prods.size}")
                        enqueueInventarioSyncWorker()
                    } else {
                        Log.w(TAG, "INVENTARIO_SKIP: No se guardó inventario porque prods.isEmpty() es ${prods.isEmpty()} o selectedClient es null")
                    }

                    if (!isOpportunity && selectedClient != null) {
                        planRuteoDao.markClientVisited(selectedClient.clientId)
                    }

                    enqueueVisitaSyncWorker()
                }

                withContext(Dispatchers.Main) {
                    resetForm()
                    onSuccess()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error crítico al registrar visita", e)
                _errorMessage.value = "Error al guardar: ${e.localizedMessage ?: "Excepción desconocida"}"
            }
        }
    }

    private fun enqueueVisitaSyncWorker() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val syncWork = OneTimeWorkRequestBuilder<VisitaSyncWorker>().setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniqueWork("VisitaSyncWorker", ExistingWorkPolicy.KEEP, syncWork)
    }

    private fun enqueueInventarioSyncWorker() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val syncWork = OneTimeWorkRequestBuilder<InventarioSyncWorker>().setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniqueWork("InventarioSyncWorker", ExistingWorkPolicy.KEEP, syncWork)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
