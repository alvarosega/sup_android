package com.alvarosega.trackingventas.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alvarosega.trackingventas.data.local.dao.SaneamientoDao
import com.alvarosega.trackingventas.data.remote.VisitaApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@HiltWorker
class SaneamientoSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val saneamientoDao: SaneamientoDao,
    private val apiService: VisitaApiService
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SANEAMIENTO_SYNC"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pendientes = saneamientoDao.getPendingSync()
        if (pendientes.isEmpty()) {
            Log.d(TAG, "No hay registros pendientes de saneamiento")
            return@withContext Result.success()
        }

        var tieneErrores = false

        for (item in pendientes) {
            try {
                val file = item.localPhotoPath?.let { File(it) }

                // ALTA exige foto de fachada obligatoria
                if (item.tipoRegistro == "ALTA" && (file == null || !file.exists())) {
                    Log.e(TAG, "Alta sin archivo de foto local válido para saneamiento ID: ${item.id}")
                    tieneErrores = true
                    continue
                }

                val builder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("tipo_registro", item.tipoRegistro)
                    .addFormDataPart("route", item.route)
                    .addFormDataPart("cliente", item.cliente)
                    .addFormDataPart("tipo_negocio", item.tipoNegocio)
                    .addFormDataPart("tipo", item.tipoNegocio)
                    .addFormDataPart("zona", item.zona)
                    .addFormDataPart("zona_comercial", item.zona)
                    .addFormDataPart("nombre_zona", item.zona)

                item.clienteId?.let {
                    builder.addFormDataPart("cliente_id", it.toString())
                    builder.addFormDataPart("codigo_cliente", it.toString())
                }

                // Campos opcionales limpios
                item.contacto?.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("contacto", it) }
                item.telefono?.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("telefono", it) }
                item.celular?.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("celular", it) }
                item.direccion?.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("direccion", it) }
                item.referencia?.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("referencia", it) }
                item.nombreFactura?.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("nombre_factura", it) }
                item.nit?.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("nit", it) }
                item.latitude?.let { builder.addFormDataPart("latitude", String.format(java.util.Locale.US, "%.8f", it)) }
                item.longitude?.let { builder.addFormDataPart("longitude", String.format(java.util.Locale.US, "%.8f", it)) }
                item.accuracy?.let { builder.addFormDataPart("accuracy", String.format(java.util.Locale.US, "%.2f", it)) }
                item.createdAt.let { builder.addFormDataPart("created_at", it) }

                // Archivo binario de foto opcional o de alta
                if (file != null && file.exists()) {
                    val photoBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    builder.addFormDataPart("photo", file.name, photoBody)
                }

                val response = apiService.storeSaneamientoMultipart(builder.build())

                if (response.isSuccessful) {
                    saneamientoDao.markAsSynced(item.id)
                    Log.d(TAG, "Saneamiento ID ${item.id} (${item.tipoRegistro}) sincronizado exitosamente (HTTP ${response.code()})")

                    item.localPhotoPath?.let { path ->
                        val localFile = File(path)
                        if (localFile.exists()) localFile.delete()
                    }
                } else {
                    tieneErrores = true
                    val errBody = response.errorBody()?.string()
                    Log.e(TAG, "Error del servidor al sincronizar saneamiento ID ${item.id} (${item.tipoRegistro}): HTTP ${response.code()} -> $errBody")
                }
            } catch (e: Exception) {
                tieneErrores = true
                Log.e(TAG, "Excepción al sincronizar saneamiento ID ${item.id} (${item.tipoRegistro})", e)
            }
        }

        if (tieneErrores) Result.retry() else Result.success()
    }
}