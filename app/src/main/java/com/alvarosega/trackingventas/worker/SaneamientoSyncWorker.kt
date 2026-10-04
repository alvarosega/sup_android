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
                val response = if (item.tipoRegistro == "EDICION") {
                    // EDICIÓN: FormUrlEncoded puro
                    apiService.storeSaneamientoForm(
                        tipoRegistro = "EDICION",
                        route = item.route,
                        cliente = item.cliente,
                        tipoNegocio = item.tipoNegocio,
                        zona = item.zona,
                        clientId = item.clienteId ?: 0L,
                        contacto = item.contacto,
                        telefono = item.telefono,
                        celular = item.celular ?: "",
                        direccion = item.direccion,
                        referencia = item.referencia,
                        nombreFactura = item.nombreFactura ?: "",
                        nit = item.nit ?: "0",
                        createdAt = item.createdAt
                    )
                } else {
                    // ALTA: MultipartBody limpio usando MultipartBody.Builder
                    val file = item.localPhotoPath?.let { File(it) }
                    if (file == null || !file.exists()) {
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
                        .addFormDataPart("zona", item.zona)

                    // Solo adjuntar los campos con contenido real (sin nulls ni binarios de texto)
                    item.contacto?.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("contacto", it) }
                    item.telefono?.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("telefono", it) }
                    item.celular?.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("celular", it) }
                    item.direccion?.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("direccion", it) }
                    item.referencia?.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("referencia", it) }
                    item.nombreFactura?.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("nombre_factura", it) }
                    item.nit?.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("nit", it) }
                    item.latitude?.let { builder.addFormDataPart("latitude", it.toString()) }
                    item.longitude?.let { builder.addFormDataPart("longitude", it.toString()) }
                    item.accuracy?.let { builder.addFormDataPart("accuracy", it.toString()) }
                    item.createdAt.let { builder.addFormDataPart("created_at", it) }

                    // Archivo binario de foto real
                    val photoBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    builder.addFormDataPart("photo", file.name, photoBody)

                    apiService.storeSaneamientoMultipart(builder.build())
                }

                if (response.isSuccessful) {
                    saneamientoDao.markAsSynced(item.id)
                    Log.d(TAG, "Saneamiento ID ${item.id} (${item.tipoRegistro}) sincronizado exitosamente (HTTP ${response.code()})")

                    item.localPhotoPath?.let { path ->
                        val file = File(path)
                        if (file.exists()) file.delete()
                    }
                } else {
                    tieneErrores = true
                    Log.e(TAG, "Error del servidor al sincronizar saneamiento ${item.id}: HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                tieneErrores = true
                Log.e(TAG, "Fallo al enviar saneamiento ${item.id}", e)
            }
        }

        if (tieneErrores) Result.retry() else Result.success()
    }
}