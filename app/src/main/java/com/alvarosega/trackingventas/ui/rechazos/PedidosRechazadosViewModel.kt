package com.alvarosega.trackingventas.ui.rechazos

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.alvarosega.trackingventas.data.local.dao.PedidoRechazadoDao
import com.alvarosega.trackingventas.data.local.dao.PlanRuteoDao
import com.alvarosega.trackingventas.data.local.entity.PedidoRechazadoEntity
import com.alvarosega.trackingventas.data.local.entity.PlanRuteoEntity
import com.alvarosega.trackingventas.data.local.pref.SessionPreferences
import com.alvarosega.trackingventas.data.location.LocationClient
import com.alvarosega.trackingventas.data.remote.VisitaApiService
import com.alvarosega.trackingventas.data.remote.dto.MotivoRechazoDto
import com.alvarosega.trackingventas.data.remote.dto.PreventaItemDto
import com.alvarosega.trackingventas.data.remote.dto.PreventaPendienteDto
import com.alvarosega.trackingventas.data.remote.dto.RechazoItemPayloadDto
import com.alvarosega.trackingventas.data.time.ServerTimeManager
import com.alvarosega.trackingventas.worker.RechazoSyncWorker
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class EditableItemRechazo(
    val item: PreventaItemDto,
    var cantidadRechazada: Int
)

@HiltViewModel
class PedidosRechazadosViewModel @Inject constructor(
    private val pedidoRechazadoDao: PedidoRechazadoDao,
    private val planRuteoDao: PlanRuteoDao,
    private val apiService: VisitaApiService,
    private val locationClient: LocationClient,
    private val sessionPreferences: SessionPreferences,
    private val serverTimeManager: ServerTimeManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "RECHAZO_VM"
    }

    private val _motivosList = MutableStateFlow<List<MotivoRechazoDto>>(emptyList())
    val motivosList: StateFlow<List<MotivoRechazoDto>> = _motivosList.asStateFlow()

    private val _pendingRechazosCount = MutableStateFlow(0)
    val pendingRechazosCount: StateFlow<Int> = _pendingRechazosCount.asStateFlow()

    val localRechazos: StateFlow<List<PedidoRechazadoEntity>> =
        pedidoRechazadoDao.getAllLocalRechazos().toStateFlow(emptyList())

    private val _clientsList = MutableStateFlow<List<PlanRuteoEntity>>(emptyList())
    val clientsList: StateFlow<List<PlanRuteoEntity>> = _clientsList.asStateFlow()

    private val _selectedClient = MutableStateFlow<PlanRuteoEntity?>(null)
    val selectedClient: StateFlow<PlanRuteoEntity?> = _selectedClient.asStateFlow()

    private val _preventasPendientes = MutableStateFlow<List<PreventaPendienteDto>>(emptyList())
    val preventasPendientes: StateFlow<List<PreventaPendienteDto>> = _preventasPendientes.asStateFlow()

    private val _selectedPreventa = MutableStateFlow<PreventaPendienteDto?>(null)
    val selectedPreventa: StateFlow<PreventaPendienteDto?> = _selectedPreventa.asStateFlow()

    private val _tipoRechazo = MutableStateFlow("TOTAL") // "TOTAL" o "PARCIAL"
    val tipoRechazo: StateFlow<String> = _tipoRechazo.asStateFlow()

    private val _selectedMotivo = MutableStateFlow<MotivoRechazoDto?>(null)
    val selectedMotivo: StateFlow<MotivoRechazoDto?> = _selectedMotivo.asStateFlow()

    private val _comentarios = MutableStateFlow("")
    val comentarios: StateFlow<String> = _comentarios.asStateFlow()

    private val _itemsState = MutableStateFlow<List<EditableItemRechazo>>(emptyList())
    val itemsState: StateFlow<List<EditableItemRechazo>> = _itemsState.asStateFlow()

    private val _capturedPhotoFile = MutableStateFlow<File?>(null)
    val capturedPhotoFile: StateFlow<File?> = _capturedPhotoFile.asStateFlow()

    private val _isLoadingPreventas = MutableStateFlow(false)
    val isLoadingPreventas: StateFlow<Boolean> = _isLoadingPreventas.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        loadMotivos()
        loadLocalClients()
        fetchPreventasPendientes()
    }

    fun loadMotivos() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getMotivosRechazo()
                if (response.isSuccessful && response.body()?.success == true) {
                    val list = response.body()!!.motivos
                    _motivosList.value = list
                    if (list.isNotEmpty()) {
                        _selectedMotivo.value = list.first()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando catálogo de motivos", e)
            }
        }
    }

    private fun loadLocalClients() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val clients = planRuteoDao.getPendingClientsByDay().firstOrNull() ?: emptyList()
                _clientsList.value = clients
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando clientes locales", e)
            }
        }
    }

    fun selectClient(client: PlanRuteoEntity) {
        _selectedClient.value = client
        _selectedPreventa.value = null
        _itemsState.value = emptyList()
        fetchPreventasPendientes(client.clientId.toString())
    }

    fun selectClientById(clienteIdStr: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val client = planRuteoDao.getClientById(clienteIdStr.toLongOrNull() ?: -1L)
            _selectedClient.value = client
            _selectedPreventa.value = null
            _itemsState.value = emptyList()
            fetchPreventasPendientes(clienteIdStr)
        }
    }

    fun fetchPreventasPendientes(clienteId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingPreventas.value = true
            try {
                val response = apiService.getPreventasPendientes(clienteId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val list = response.body()!!.data
                    _preventasPendientes.value = list
                    _pendingRechazosCount.value = response.body()!!.total
                    if (list.isNotEmpty()) {
                        selectPreventa(list.first())
                    }
                } else {
                    _preventasPendientes.value = emptyList()
                    _pendingRechazosCount.value = 0
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error consultando preventas pendientes", e)
                _preventasPendientes.value = emptyList()
            } finally {
                _isLoadingPreventas.value = false
            }
        }
    }

    fun selectPreventa(preventa: PreventaPendienteDto) {
        _selectedPreventa.value = preventa
        val tipo = preventa.tipoSugerido ?: if ((preventa.montoTotalFacturado ?: 0.0) == 0.0) "TOTAL" else "PARCIAL"
        _tipoRechazo.value = tipo
        if (tipo == "PARCIAL") {
            _motivosList.value.firstOrNull { it.codigo == "RECHAZO_PARCIAL" }?.let {
                _selectedMotivo.value = it
            }
        }
        updateItemsState(preventa, tipo)
    }

    private fun updateItemsState(preventa: PreventaPendienteDto, tipo: String) {
        val editables = preventa.items.map { item ->
            val cantRechazada = if (tipo == "TOTAL") {
                item.cantidadPreventa
            } else {
                val sug = item.cantidadRechazada ?: (item.cantidadPreventa - (item.cantidadFacturada ?: 0))
                sug.coerceIn(0, item.cantidadPreventa)
            }
            EditableItemRechazo(item = item, cantidadRechazada = cantRechazada)
        }
        _itemsState.value = editables
    }

    fun updateItemCantidadRechazada(index: Int, nuevaCantidad: Int) {
        val list = _itemsState.value.toMutableList()
        if (index in list.indices) {
            val current = list[index]
            val clamped = nuevaCantidad.coerceIn(0, current.item.cantidadPreventa)
            list[index] = current.copy(cantidadRechazada = clamped)
            _itemsState.value = list
        }
    }

    fun setSelectedMotivo(motivo: MotivoRechazoDto) {
        _selectedMotivo.value = motivo
    }

    fun setComentarios(text: String) {
        _comentarios.value = text
    }

    fun createPrivatePhotoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val photoFile = File(context.filesDir, "RECHAZO_${timeStamp}.jpg")
        _capturedPhotoFile.value = photoFile
        return photoFile
    }

    fun removeCapturedPhoto() {
        _capturedPhotoFile.value?.let { if (it.exists()) it.delete() }
        _capturedPhotoFile.value = null
    }

    fun isGpsEnabled(): Boolean = locationClient.isGpsEnabled()

    fun guardarRechazo(onSuccess: () -> Unit) {
        val preventa = _selectedPreventa.value
        val items = _itemsState.value
        val motivoObj = _selectedMotivo.value

        if (preventa == null) {
            _statusMessage.value = "Debe seleccionar una preventa rechazada de la lista"
            return
        }

        if (items.isEmpty()) {
            _statusMessage.value = "No hay productos en la preventa elegida"
            return
        }

        if (_tipoRechazo.value == "PARCIAL" && items.all { it.cantidadRechazada == 0 }) {
            _statusMessage.value = "Para rechazo parcial, al menos un producto debe tener cantidad mayor a 0"
            return
        }

        val motivoCodigo = motivoObj?.codigo ?: "OTRO"

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val loc = withContext(Dispatchers.IO) { locationClient.getCurrentLocation() }
                val visitedAt = serverTimeManager.getBoliviaTimestamp()
                val uuid = UUID.randomUUID().toString()

                val itemsPayload = items.filter { it.cantidadRechazada > 0 }.map { editable ->
                    RechazoItemPayloadDto(
                        preventaItemId = editable.item.preventaItemId,
                        productoId = editable.item.productoId,
                        codigoProducto = editable.item.codigoProducto,
                        productoNombre = editable.item.productoNombre,
                        categoria = editable.item.categoria,
                        cantidadPreventa = editable.item.cantidadPreventa,
                        cantidadRechazada = editable.cantidadRechazada,
                        precioUnitario = editable.item.precioUnitario,
                        motivoEspecifico = motivoCodigo
                    )
                }

                val itemsJson = Gson().toJson(itemsPayload)

                val entity = PedidoRechazadoEntity(
                    uuid = uuid,
                    clienteId = preventa.clienteId,
                    clienteNombre = preventa.clienteNombre ?: "Cliente #${preventa.clienteId}",
                    nroPreventa = preventa.nroPreventa,
                    fechaPreventa = preventa.fechaPreventa,
                    fechaRechazo = visitedAt,
                    motivo = motivoCodigo,
                    tipoRechazo = _tipoRechazo.value,
                    comentarios = _comentarios.value.ifBlank { null },
                    latitude = loc?.latitude,
                    longitude = loc?.longitude,
                    accuracy = loc?.accuracy,
                    isMockLocation = loc?.isFromMockProvider ?: false,
                    localPhotoPath = _capturedPhotoFile.value?.absolutePath,
                    itemsJson = itemsJson,
                    isSynced = false
                )

                withContext(Dispatchers.IO) {
                    val insertedId = pedidoRechazadoDao.insertPedidoRechazado(entity)
                    Log.d(TAG, "Rechazo guardado localmente con ID: $insertedId, UUID: $uuid")
                    enqueueSyncWorker()
                }

                _statusMessage.value = "Justificación de rechazo registrada correctamente"
                
                // Actualizar la lista local removiendo la preventa justificada
                val updatedList = _preventasPendientes.value.filter { it.nroPreventa != preventa.nroPreventa }
                _preventasPendientes.value = updatedList
                _pendingRechazosCount.value = updatedList.size

                resetForm()
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Error guardando justificación de rechazo", e)
                _statusMessage.value = "Error al guardar rechazo: ${e.localizedMessage}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun enqueueSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWork = OneTimeWorkRequestBuilder<RechazoSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "RechazoSyncWorker",
            ExistingWorkPolicy.REPLACE,
            syncWork
        )
        Log.d(TAG, "WorkManager: Encolado RechazoSyncWorker")
    }

    fun resetForm() {
        _selectedClient.value = null
        _selectedPreventa.value = null
        _itemsState.value = emptyList()
        _tipoRechazo.value = "TOTAL"
        _selectedMotivo.value = _motivosList.value.firstOrNull()
        _comentarios.value = ""
        _capturedPhotoFile.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.toStateFlow(initialValue: T): StateFlow<T> {
        val flow = MutableStateFlow(initialValue)
        viewModelScope.launch {
            collect { flow.value = it }
        }
        return flow.asStateFlow()
    }
}
