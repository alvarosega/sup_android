package com.alvarosega.trackingventas.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alvarosega.trackingventas.data.local.dao.PedidoRechazadoDao
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
class RechazoSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pedidoRechazadoDao: PedidoRechazadoDao,
    private val apiService: VisitaApiService
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "RECHAZO_SYNC"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pendientes = pedidoRechazadoDao.getPendingSync()
        if (pendientes.isEmpty()) {
            Log.d(TAG, "No hay pedidos rechazados pendientes de sincronizar")
            return@withContext Result.success()
        }

        var tieneErrores = false

        for (item in pendientes) {
            try {
                val builder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("uuid", item.uuid)
                    .addFormDataPart("cliente_id", item.clienteId)
                    .addFormDataPart("motivo", item.motivo)
                    .addFormDataPart("tipo_rechazo", item.tipoRechazo)
                    .addFormDataPart("items", item.itemsJson)

                item.nroPreventa?.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("nro_preventa", it) }
                item.fechaPreventa?.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("fecha_preventa", it) }
                item.fechaRechazo.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("fecha_rechazo", it) }
                item.comentarios?.takeIf { it.isNotBlank() }?.let { builder.addFormDataPart("comentarios", it) }
                item.latitude?.let { builder.addFormDataPart("latitude", it.toString()) }
                item.longitude?.let { builder.addFormDataPart("longitude", it.toString()) }
                item.accuracy?.let { builder.addFormDataPart("accuracy", it.toString()) }
                builder.addFormDataPart("is_mock_location", if (item.isMockLocation) "1" else "0")

                item.localPhotoPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        val photoBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        builder.addFormDataPart("photo", file.name, photoBody)
                    }
                }

                val response = apiService.storePedidoRechazadoMultipart(builder.build())

                if (response.isSuccessful) {
                    pedidoRechazadoDao.markAsSynced(item.id)
                    Log.d(TAG, "Pedido Rechazado local ID ${item.id} (UUID: ${item.uuid}) sincronizado con éxito")

                    item.localPhotoPath?.let { path ->
                        val file = File(path)
                        if (file.exists()) file.delete()
                    }
                } else {
                    tieneErrores = true
                    val errBody = response.errorBody()?.string()
                    Log.e(TAG, "Error HTTP ${response.code()} sincronizando rechazo ID ${item.id}: $errBody")
                }
            } catch (e: Exception) {
                tieneErrores = true
                Log.e(TAG, "Excepción al sincronizar pedido rechazado ID ${item.id}", e)
            }
        }

        if (tieneErrores) Result.retry() else Result.success()
    }
}
