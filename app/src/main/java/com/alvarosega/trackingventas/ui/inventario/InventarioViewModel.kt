package com.alvarosega.trackingventas.ui.inventario

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.alvarosega.trackingventas.data.local.ModuleConfigManager
import com.alvarosega.trackingventas.data.local.dao.InventarioClienteDao
import com.alvarosega.trackingventas.data.local.dao.PlanRuteoDao
import com.alvarosega.trackingventas.data.local.entity.InventarioClienteEntity
import com.alvarosega.trackingventas.data.local.entity.PlanRuteoEntity
import com.alvarosega.trackingventas.data.location.LocationClient
import com.alvarosega.trackingventas.data.remote.VisitaApiService
import com.alvarosega.trackingventas.data.remote.dto.InventarioItemPayloadDto
import com.alvarosega.trackingventas.data.remote.dto.InventarioProductoHistorialDto
import com.alvarosega.trackingventas.data.remote.dto.ProductoInventarioDto
import com.alvarosega.trackingventas.data.time.ServerTimeManager
import com.alvarosega.trackingventas.worker.InventarioSyncWorker
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class InventarioViewModel @Inject constructor(
    private val inventarioClienteDao: InventarioClienteDao,
    private val planRuteoDao: PlanRuteoDao,
    private val apiService: VisitaApiService,
    private val locationClient: LocationClient,
    private val moduleConfigManager: ModuleConfigManager,
    private val serverTimeManager: ServerTimeManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "INVENTARIO_VM"
    }

    private val _selectedClient = MutableStateFlow<PlanRuteoEntity?>(null)
    val selectedClient: StateFlow<PlanRuteoEntity?> = _selectedClient.asStateFlow()

    private val _productosHistorial = MutableStateFlow<List<InventarioProductoHistorialDto>>(emptyList())
    val productosHistorial: StateFlow<List<InventarioProductoHistorialDto>> = _productosHistorial.asStateFlow()

    private val _stockMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val stockMap: StateFlow<Map<String, String>> = _stockMap.asStateFlow()

    private val _comments = MutableStateFlow("")
    val comments: StateFlow<String> = _comments.asStateFlow()

    private val _isLoadingProductos = MutableStateFlow(false)
    val isLoadingProductos: StateFlow<Boolean> = _isLoadingProductos.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun getProdKey(prod: InventarioProductoHistorialDto, idx: Int): String {
        return if (!prod.codigo.isNullOrBlank()) {
            prod.codigo
        } else if (prod.productoId != null && prod.productoId > 0L) {
            prod.productoId.toString()
        } else {
            "PROD_IDX_$idx"
        }
    }

    fun initForClient(clientId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val client = planRuteoDao.getClientById(clientId)
            _selectedClient.value = client
            if (client != null) {
                fetchHistorialProductos(client.clientId)
            } else {
                _statusMessage.value = "Cliente #$clientId no encontrado en la base local"
            }
        }
    }

    fun fetchHistorialProductos(clienteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingProductos.value = true
            try {
                var dias = moduleConfigManager.getIntConfig("inventario_cliente", "dias_historial", 90)
                if (dias < 90) dias = 90
                var response = apiService.getHistorialProductosCliente(clienteId.toString(), dias)
                Log.d(TAG, "getHistorialProductosCliente ($dias días) HTTP Code: ${response.code()}")

                var rawList = if (response.isSuccessful && response.body() != null) {
                    response.body()!!.productos ?: emptyList()
                } else emptyList()

                // Si con 90 días no vino nada, reintentar ampliando a 180 días
                if (rawList.isEmpty()) {
                    val fallbackResp = apiService.getHistorialProductosCliente(clienteId.toString(), 180)
                    if (fallbackResp.isSuccessful && fallbackResp.body() != null) {
                        rawList = fallbackResp.body()!!.productos ?: emptyList()
                    }
                }

                // Fallback adicional: Si tampoco hay historial en ese endpoint, consultar sugeridos del cliente
                if (rawList.isEmpty()) {
                    val sugResp = apiService.getSugeridoCliente(clienteId)
                    if (sugResp.isSuccessful && sugResp.body() != null) {
                        val sugItems = sugResp.body()!!.sugeridos
                        rawList = sugItems.mapIndexed { idx, sug ->
                            ProductoInventarioDto(
                                productoId = sug.codigo.toLongOrNull() ?: (idx + 1L),
                                codigo = sug.codigo,
                                producto = sug.producto,
                                categoria = sug.jerarquia1 ?: "General",
                                jerarquia1 = sug.jerarquia1,
                                totalComprado = if (sug.comproMesActual) sug.cantidadCompradaMesActual else 0
                            )
                        }
                    }
                }

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
                _stockMap.value = emptyMap()
                Log.d(TAG, "Productos de inventario cargados exitosamente para cliente #$clienteId: ${sortedList.size} productos")
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando historial de productos para inventario", e)
                _productosHistorial.value = emptyList()
                _statusMessage.value = "Error de red al consultar productos"
            } finally {
                _isLoadingProductos.value = false
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

    fun setComments(text: String) {
        _comments.value = text
    }

    fun isGpsEnabled(): Boolean = locationClient.isGpsEnabled()

    fun guardarInventario(onSuccess: () -> Unit) {
        val client = _selectedClient.value
        val productos = _productosHistorial.value
        val map = _stockMap.value

        if (client == null) {
            _statusMessage.value = "Cliente no especificado"
            return
        }

        if (productos.isEmpty()) {
            _statusMessage.value = "No hay productos disponibles para este cliente"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val loc = withContext(Dispatchers.IO) { locationClient.getCurrentLocation() }
                val visitedAt = serverTimeManager.getBoliviaTimestamp()
                val uuid = UUID.randomUUID().toString()

                val itemsPayload = productos.mapIndexed { idx, prod ->
                    val key = getProdKey(prod, idx)
                    val prodId = if (prod.productoId != null && prod.productoId > 0L) prod.productoId else (idx + 1L)
                    InventarioItemPayloadDto(
                        productoId = prodId,
                        codigoProducto = prod.codigo ?: "",
                        productoNombre = prod.producto ?: "Producto",
                        categoria = prod.categoria ?: "General",
                        jerarquia1 = prod.jerarquia1,
                        jerarquia2 = prod.jerarquia2,
                        jerarquia3 = prod.jerarquia3,
                        stockFisico = map[key]?.toIntOrNull() ?: 0,
                        cantidadHistorica = prod.totalComprado,
                        ultimaCompraFecha = prod.ultimaCompra?.fecha,
                        ultimaCompraCantidad = prod.ultimaCompra?.cantidad
                    )
                }

                val itemsJson = Gson().toJson(itemsPayload)

                val entity = InventarioClienteEntity(
                    uuid = uuid,
                    clienteId = client.clientId.toString(),
                    clienteNombre = client.clientName,
                    latitude = loc?.latitude,
                    longitude = loc?.longitude,
                    accuracy = loc?.accuracy,
                    isMockLocation = loc?.isFromMockProvider ?: false,
                    comments = if (_comments.value.isBlank()) null else _comments.value.trim(),
                    itemsJson = itemsJson,
                    createdAt = visitedAt,
                    isSynced = false
                )

                withContext(Dispatchers.IO) {
                    val insertedId = inventarioClienteDao.insertInventario(entity)
                    Log.d(TAG, "Inventario de PDV guardado localmente ID: $insertedId, UUID: $uuid")
                    enqueueSyncWorker()
                }

                _statusMessage.value = "Toma de inventario registrada con éxito"
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Error guardando inventario en PDV", e)
                _statusMessage.value = "Error al guardar inventario: ${e.localizedMessage}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun enqueueSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWork = OneTimeWorkRequestBuilder<InventarioSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "InventarioSyncWorker",
            ExistingWorkPolicy.REPLACE,
            syncWork
        )
        Log.d(TAG, "WorkManager: Encolado InventarioSyncWorker")
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
