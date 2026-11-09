package com.alvarosega.trackingventas.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alvarosega.trackingventas.data.local.dao.InventarioClienteDao
import com.alvarosega.trackingventas.data.remote.VisitaApiService
import com.alvarosega.trackingventas.data.remote.dto.InventarioItemPayloadDto
import com.alvarosega.trackingventas.data.remote.dto.InventarioRequestDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

                val itemType = object : TypeToken<List<InventarioItemPayloadDto>>() {}.type
                val itemsList: List<InventarioItemPayloadDto> = Gson().fromJson(item.itemsJson, itemType) ?: emptyList()

                if (itemsList.isEmpty()) {
                    Log.e(TAG, "Inventario ID ${item.id} no tiene ítems censados. Se omite.")
                    continue
                }

                val request = InventarioRequestDto(
                    uuid = item.uuid,
                    clienteId = item.clienteId.toLongOrNull() ?: 0L,
                    codigoCliente = clientCode,
                    clienteNombre = clientName,
                    fechaRegistro = item.createdAt,
                    latitude = item.latitude,
                    longitude = item.longitude,
                    accuracy = item.accuracy?.toDouble(),
                    isMockLocation = item.isMockLocation,
                    observaciones = item.comments,
                    items = itemsList
                )

                Log.d(TAG, "Enviando Inventario JSON ID ${item.id} (UUID: ${item.uuid}, Cliente: ${item.clienteId}, Items: ${itemsList.size})")

                val response = apiService.storeInventarioClienteJson(request)

                Log.d(TAG, "storeInventarioClienteJson HTTP Status: ${response.code()}")

                if (response.isSuccessful || response.code() == 201 || response.code() == 200) {
                    val body = response.body()
                    val isSuccess = body?.success == true || body?.idempotent == true
                    if (isSuccess || response.isSuccessful) {
                        inventarioClienteDao.markAsSynced(item.id)
                        Log.d(TAG, "Inventario de PDV local ID ${item.id} (UUID: ${item.uuid}) sincronizado correctamente en Laravel")
                    } else {
                        tieneErrores = true
                        Log.e(TAG, "Respuesta no exitosa de Laravel para inventario ID ${item.id}: ${body?.message}")
                    }
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
