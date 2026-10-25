package com.alvarosega.trackingventas.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alvarosega.trackingventas.data.local.dao.InventarioClienteDao
import com.alvarosega.trackingventas.data.remote.VisitaApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody

@HiltWorker
class InventarioSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val inventarioClienteDao: InventarioClienteDao,
    private val apiService: VisitaApiService
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "INVENTARIO_SYNC"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pendientes = inventarioClienteDao.getPendingSync()
        if (pendientes.isEmpty()) {
            Log.d(TAG, "No hay inventarios de PDV pendientes de sincronizar")
            return@withContext Result.success()
        }

        var tieneErrores = false

        for (item in pendientes) {
            try {
                val clientCode = item.codigoCliente ?: item.clienteId
                val clientName = item.clienteNombre ?: ""

                val builder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("uuid", item.uuid)
                    .addFormDataPart("cliente_id", item.clienteId)
                    .addFormDataPart("codigo_cliente", clientCode)
                    .addFormDataPart("cliente_nombre", clientName)
                    .addFormDataPart("cliente", clientName)

                // Deserializar JSON e insertar cada elemento como un arreglo multipart nativo para PHP/Laravel (items[0][...])
                try {
                    val itemType = object : com.google.gson.reflect.TypeToken<List<com.alvarosega.trackingventas.data.remote.dto.InventarioItemPayloadDto>>() {}.type
                    val itemsList: List<com.alvarosega.trackingventas.data.remote.dto.InventarioItemPayloadDto> = com.google.gson.Gson().fromJson(item.itemsJson, itemType) ?: emptyList()

                    itemsList.forEachIndexed { idx, payload ->
                        builder.addFormDataPart("items[$idx][producto_id]", payload.productoId.toString())
                        builder.addFormDataPart("items[$idx][codigo_producto]", payload.codigoProducto)
                        builder.addFormDataPart("items[$idx][producto_nombre]", payload.productoNombre)
                        builder.addFormDataPart("items[$idx][categoria]", payload.categoria ?: "General")
                        builder.addFormDataPart("items[$idx][stock_fisico]", payload.stockFisico.toString())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deserializando itemsJson para inventario ID ${item.id}", e)
                    builder.addFormDataPart("items", item.itemsJson)
                }

                item.comments?.takeIf { it.isNotBlank() }?.let { obs ->
                    builder.addFormDataPart("comentarios", obs)
                    builder.addFormDataPart("comments", obs)
                    builder.addFormDataPart("observaciones", obs)
                }
                item.latitude?.let { builder.addFormDataPart("latitude", String.format(java.util.Locale.US, "%.8f", it)) }
                item.longitude?.let { builder.addFormDataPart("longitude", String.format(java.util.Locale.US, "%.8f", it)) }
                item.accuracy?.let { builder.addFormDataPart("accuracy", String.format(java.util.Locale.US, "%.2f", it)) }
                builder.addFormDataPart("is_mock_location", if (item.isMockLocation) "1" else "0")

                val response = apiService.storeInventarioClienteMultipart(builder.build())

                if (response.isSuccessful) {
                    inventarioClienteDao.markAsSynced(item.id)
                    Log.d(TAG, "Inventario de PDV local ID ${item.id} (UUID: ${item.uuid}) sincronizado con éxito")
                } else {
                    tieneErrores = true
                    val errBody = response.errorBody()?.string()
                    Log.e(TAG, "Error HTTP ${response.code()} sincronizando inventario ID ${item.id}: $errBody")
                }
            } catch (e: Exception) {
                tieneErrores = true
                Log.e(TAG, "Excepción al sincronizar inventario de PDV ID ${item.id}", e)
            }
        }

        if (tieneErrores) Result.retry() else Result.success()
    }
}
