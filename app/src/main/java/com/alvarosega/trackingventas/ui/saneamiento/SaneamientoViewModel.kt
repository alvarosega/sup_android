package com.alvarosega.trackingventas.ui.saneamiento

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
import com.alvarosega.trackingventas.data.local.dao.SaneamientoDao
import com.alvarosega.trackingventas.data.local.entity.DeviceEventEntity
import com.alvarosega.trackingventas.data.local.entity.SaneamientoBaseEntity
import com.alvarosega.trackingventas.data.local.pref.SessionPreferences
import com.alvarosega.trackingventas.data.location.LocationClient
import com.alvarosega.trackingventas.data.time.ServerTimeManager
import com.alvarosega.trackingventas.util.CatalogoTiposNegocio
import com.alvarosega.trackingventas.util.ExifMetadataHelper
import com.alvarosega.trackingventas.worker.SaneamientoSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class SaneamientoFormState(
    val clienteId: Long? = null,
    val esEdicion: Boolean = false,
    val route: String = "",
    val cliente: String = "",
    val tipoNegocio: String = "",
    val zona: String = "",
    val contacto: String = "",
    val telefono: String = "",
    val celular: String = "",
    val direccion: String = "",
    val referencia: String = "",
    val nombreFactura: String = "",
    val nit: String = "",
    val photoCaptured: Boolean = false,
    val isSaving: Boolean = false,
    val gpsReady: Boolean = false
)

@HiltViewModel
class SaneamientoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionPreferences: SessionPreferences,
    private val locationClient: LocationClient,
    private val planRuteoDao: PlanRuteoDao,
    private val saneamientoDao: SaneamientoDao,
    private val deviceEventDao: DeviceEventDao,
    private val serverTimeManager: ServerTimeManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SaneamientoFormState())
    val uiState: StateFlow<SaneamientoFormState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    var activePhotoFile: File? = null
    var capturedLocation: Location? = null

    companion object {
        private const val TAG = "SANEAMIENTO_VM"
    }

    fun initForm(clientId: Long?) {
        val currentRoute = sessionPreferences.activeRoute.value.ifBlank { "SIN_RUTA" }

        if (clientId != null && clientId > 0L) {
            viewModelScope.launch(Dispatchers.IO) {
                val clienteOriginal = planRuteoDao.getClientById(clientId)
                val saneamientoPrevio = saneamientoDao.getLatestByClient(clientId)
                withContext(Dispatchers.Main) {
                    if (clienteOriginal != null) {
                        _uiState.value = SaneamientoFormState(
                            clienteId = clienteOriginal.clientId,
                            esEdicion = true,
                            route = clienteOriginal.route.ifBlank { currentRoute },
                            cliente = saneamientoPrevio?.cliente ?: clienteOriginal.clientName,
                            tipoNegocio = saneamientoPrevio?.tipoNegocio ?: (clienteOriginal.tipoNegocio ?: ""),
                            zona = saneamientoPrevio?.zona ?: (clienteOriginal.zona ?: ""),
                            contacto = saneamientoPrevio?.contacto ?: (clienteOriginal.contacto ?: ""),
                            telefono = saneamientoPrevio?.telefono ?: (clienteOriginal.telefono ?: ""),
                            celular = saneamientoPrevio?.celular ?: (clienteOriginal.celular ?: ""),
                            direccion = saneamientoPrevio?.direccion ?: (clienteOriginal.address ?: ""),
                            referencia = saneamientoPrevio?.referencia ?: (clienteOriginal.referencia ?: ""),
                            nombreFactura = saneamientoPrevio?.nombreFactura ?: (clienteOriginal.nombreFactura ?: ""),
                            nit = saneamientoPrevio?.nit ?: (clienteOriginal.nit ?: ""),
                            gpsReady = true
                        )
                    } else {
                        _errorMessage.value = "Cliente #$clientId no encontrado en el plan local."
                    }
                }
            }
        } else {
            _uiState.value = SaneamientoFormState(
                clienteId = null,
                esEdicion = false,
                route = currentRoute,
                gpsReady = false
            )
            capturarGpsParaAlta()
        }
    }

    fun capturarGpsParaAlta() {
        viewModelScope.launch(Dispatchers.IO) {
            val loc = locationClient.getCurrentLocation()
            if (loc == null) {
                _errorMessage.value = "No se pudo obtener la señal GPS para el alta."
                return@launch
            }
            if (loc.isFromMockProvider) {
                deviceEventDao.insertEvent(
                    DeviceEventEntity(
                        eventType = "MOCK_LOCATION_SANEAMIENTO_BLOCKED",
                        details = "Intento de alta comercial con GPS simulado",
                        recordedAt = serverTimeManager.getBoliviaTimestamp()
                    )
                )
                _errorMessage.value = "Ubicación simulada detectada. Acción bloqueada."
                return@launch
            }

            capturedLocation = loc
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(gpsReady = true)
            }
        }
    }

    fun updateField(fieldUpdater: SaneamientoFormState.() -> SaneamientoFormState) {
        _uiState.value = _uiState.value.fieldUpdater()
    }

    fun generatePrivatePhotoUri(): Uri {
        val dir = File(context.filesDir, "saneamiento_evidence").apply { mkdirs() }
        val secureNow = serverTimeManager.getSecureCurrentTimeMillis()
        val file = File(dir, "ALTA_${secureNow}.jpg")
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
                _uiState.value = _uiState.value.copy(photoCaptured = true)
                onComplete()
            }
        }
    }

    fun guardarRegistro(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.isSaving) return

        // 1. Validaciones básicas obligatorias (Alta y Edición)
        if (state.cliente.isBlank()) {
            _errorMessage.value = "El nombre o razón social es obligatorio."
            return
        }

        if (Regex("[#$+%_=<>{}\\[\\]\\^]").containsMatchIn(state.cliente)) {
            _errorMessage.value = "El nombre contiene caracteres especiales inválidos."
            return
        }

        if (state.tipoNegocio.isBlank() || !CatalogoTiposNegocio.TIPOS.contains(state.tipoNegocio)) {
            _errorMessage.value = "Debe seleccionar un Tipo de Negocio válido del catálogo."
            return
        }

        if (state.zona.isBlank() || !CatalogoTiposNegocio.ZONAS.contains(state.zona)) {
            _errorMessage.value = "Debe seleccionar una Zona Comercial válida del catálogo."
            return
        }

        // 2. Celular obligatorio (Alta y Edición)
        val digitsCelular = state.celular.filter { it.isDigit() }
        if (digitsCelular.length !in 7..8) {
            _errorMessage.value = "El celular es obligatorio y debe tener entre 7 y 8 dígitos numéricos."
            return
        }

        // 3. Teléfono opcional pero limpio
        val digitsTelefono = state.telefono.filter { it.isDigit() }
        if (state.telefono.isNotBlank() && digitsTelefono.length < 7) {
            _errorMessage.value = "El teléfono fijo debe ser numérico y tener al menos 7 dígitos."
            return
        }

        // 4. Facturación obligatoria tanto en Alta como en Edición
        if (state.nombreFactura.isBlank()) {
            _errorMessage.value = "La Razón Social / Nombre Factura es obligatoria."
            return
        }
        val digitsNit = state.nit.filter { it.isDigit() }
        if (state.nit.isBlank() || digitsNit.isEmpty() || !state.nit.all { it.isDigit() }) {
            _errorMessage.value = "El NIT es obligatorio y debe ser numérico (ingrese 0 si no cuenta con NIT)."
            return
        }

        // 5. Exclusivo de Alta: Foto y GPS
        if (!state.esEdicion) {
            if (capturedLocation == null) {
                _errorMessage.value = "El GPS es obligatorio para un Alta."
                return
            }
            if (activePhotoFile == null || !activePhotoFile!!.exists() || !state.photoCaptured) {
                _errorMessage.value = "La fotografía de fachada es obligatoria para un Alta."
                return
            }
        }

        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val timestamp = serverTimeManager.getBoliviaTimestamp()
                    val loc = capturedLocation

                    val entity = SaneamientoBaseEntity(
                        clienteId = if (state.esEdicion) state.clienteId else null,
                        tipoRegistro = if (state.esEdicion) "EDICION" else "ALTA",
                        route = state.route.ifBlank { "SIN_RUTA" },
                        cliente = state.cliente.trim(),
                        tipoNegocio = state.tipoNegocio.trim(),
                        zona = state.zona.trim(),
                        contacto = state.contacto.trim().ifEmpty { null },
                        telefono = state.telefono.trim().ifEmpty { null },
                        celular = state.celular.trim().ifEmpty { null },
                        direccion = state.direccion.trim().ifEmpty { null },
                        referencia = state.referencia.trim().ifEmpty { null },
                        nombreFactura = state.nombreFactura.trim().ifEmpty { null },
                        nit = state.nit.trim().ifEmpty { null },
                        latitude = if (state.esEdicion) null else loc?.latitude,
                        longitude = if (state.esEdicion) null else loc?.longitude,
                        accuracy = if (state.esEdicion) null else loc?.accuracy,
                        localPhotoPath = if (state.esEdicion) null else activePhotoFile?.absolutePath,
                        createdAt = timestamp,
                        isSynced = false
                    )

                    val insertedId = saneamientoDao.insertSaneamiento(entity)
                    Log.d(TAG, "Saneamiento local persistido. ID: $insertedId, Tipo: ${entity.tipoRegistro}")

                    if (state.esEdicion && state.clienteId != null) {
                        val original = planRuteoDao.getClientById(state.clienteId)
                        if (original != null) {
                            val updated = original.copy(
                                clientName = state.cliente.trim(),
                                tipoNegocio = state.tipoNegocio.trim(),
                                zona = state.zona.trim(),
                                contacto = state.contacto.trim().ifEmpty { null },
                                telefono = state.telefono.trim().ifEmpty { null },
                                celular = state.celular.trim().ifEmpty { null },
                                address = state.direccion.trim().ifEmpty { null },
                                referencia = state.referencia.trim().ifEmpty { null },
                                nombreFactura = state.nombreFactura.trim().ifEmpty { null },
                                nit = state.nit.trim().ifEmpty { null },
                                hasPendingReview = true
                            )
                            planRuteoDao.insertIfNotExists(listOf(updated))
                        }
                    }

                    enqueueSyncWorker()
                }

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    onSuccess()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error al persistir saneamiento local", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    _errorMessage.value = "Error al guardar: ${e.localizedMessage ?: "Error desconocido"}"
                }
            }
        }
    }

    private fun enqueueSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWork = OneTimeWorkRequestBuilder<SaneamientoSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "SaneamientoSyncWorker",
            ExistingWorkPolicy.REPLACE,
            syncWork
        )
        Log.d(TAG, "WorkManager: Encolado forzado de SaneamientoSyncWorker con REPLACE")
    }

    fun clearError() {
        _errorMessage.value = null
    }
}