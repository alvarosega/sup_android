package com.alvarosega.trackingventas.sanitization.ui

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvarosega.trackingventas.data.location.LocationClient
import com.alvarosega.trackingventas.sanitization.data.local.dao.ClientAuditDao
import com.alvarosega.trackingventas.sanitization.data.local.dao.ReferenceClientDao
import com.alvarosega.trackingventas.sanitization.data.local.entity.ClientAuditEntity
import com.alvarosega.trackingventas.sanitization.data.local.entity.ReferenceClientEntity
import com.alvarosega.trackingventas.sanitization.data.remote.SanitizationApiService
import com.alvarosega.trackingventas.sanitization.util.ImageCompressor
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SanitizationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: SanitizationApiService,
    private val clientDao: ReferenceClientDao,
    private val auditDao: ClientAuditDao,
    private val locationClient: LocationClient
) : ViewModel() {

    private val _clients = MutableStateFlow<List<ReferenceClientEntity>>(emptyList())
    val clients: StateFlow<List<ReferenceClientEntity>> = _clients.asStateFlow()

    private val _selectedClient = MutableStateFlow<ReferenceClientEntity?>(null)
    val selectedClient: StateFlow<ReferenceClientEntity?> = _selectedClient.asStateFlow()

    private val _capturedPhotoUris = MutableStateFlow<List<Uri>>(emptyList())
    val capturedPhotoUris: StateFlow<List<Uri>> = _capturedPhotoUris.asStateFlow()

    private val _savedPhotoFiles = mutableListOf<File>()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    var pendingPhotoFile: File? = null

    init {
        loadLocalClients()
    }

    private fun loadLocalClients() {
        viewModelScope.launch(Dispatchers.IO) {
            clientDao.getClientsByDay("LUNES").collect { list ->
                _clients.value = list
            }
        }
    }

    fun syncDownMasterClients() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getReferenceClients()
                if (response.isSuccessful && response.body() != null) {
                    val entities = response.body()!!.map {
                        ReferenceClientEntity(
                            clientId = it.clientId,
                            route = it.route,
                            day = it.day,
                            address = it.address,
                            referenceLatitude = it.latitude,
                            referenceLongitude = it.longitude,
                            isAudited = false
                        )
                    }
                    clientDao.insertClients(entities)
                    _statusMessage.value = "Clientes sincronizados: ${entities.size}"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error al descargar clientes: ${e.localizedMessage}"
            }
        }
    }

    fun selectClient(client: ReferenceClientEntity) {
        _selectedClient.value = client
        _capturedPhotoUris.value = emptyList()
        _savedPhotoFiles.clear()
    }

    fun clearSelection() {
        _selectedClient.value = null
        _capturedPhotoUris.value = emptyList()
        _savedPhotoFiles.clear()
    }

    fun createTempPictureUri(): Uri {
        val storageDir = File(context.filesDir, "audit_photos").apply { mkdirs() }
        val file = File.createTempFile("TEMP_${System.currentTimeMillis()}_", ".jpg", storageDir)
        pendingPhotoFile = file
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun onPhotoTakenSuccess() {
        val file = pendingPhotoFile ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val compressed = ImageCompressor.compressImageFile(context, file)
            _savedPhotoFiles.add(compressed)
            val uri = Uri.fromFile(compressed)
            _capturedPhotoUris.value = _capturedPhotoUris.value + uri
        }
    }

    fun saveAudit(statusDecision: String, comments: String, onFinished: () -> Unit) {
        val client = _selectedClient.value ?: return

        if (_savedPhotoFiles.isEmpty() && statusDecision == "VALIDATED") {
            _statusMessage.value = "Se requiere al menos 1 fotografía"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val location = locationClient.getCurrentLocation()
            if (location == null) {
                _statusMessage.value = "Activa el GPS para capturar la ubicación"
                return@launch
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val photoPaths = _savedPhotoFiles.map { it.absolutePath }
            val photoPathsJson = Gson().toJson(photoPaths)

            val auditEntity = ClientAuditEntity(
                clientId = client.clientId,
                auditStatus = statusDecision,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                comments = comments,
                photoPathsJson = photoPathsJson,
                auditedAt = timestamp,
                isSynced = false
            )

            auditDao.insertAudit(auditEntity)
            clientDao.updateAuditedStatus(client.clientId, true)

            _statusMessage.value = "Auditoría guardada en local"
            clearSelection()
            onFinished()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}