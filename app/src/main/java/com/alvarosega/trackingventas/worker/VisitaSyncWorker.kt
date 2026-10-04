package com.alvarosega.trackingventas.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alvarosega.trackingventas.data.local.dao.VisitaDao
import com.alvarosega.trackingventas.data.remote.VisitaApiService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class VisitaSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface VisitaSyncWorkerEntryPoint {
        fun visitaDao(): VisitaDao
        fun visitaApiService(): VisitaApiService
    }

    companion object {
        private const val TAG = "VISITA_DEBUG"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "VisitaSyncWorker: Ejecutando tarea doWork()")
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                VisitaSyncWorkerEntryPoint::class.java
            )
            val visitaDao = entryPoint.visitaDao()
            val apiService = entryPoint.visitaApiService()

            val pendingVisitas = visitaDao.getPendingSyncVisitas()
            Log.d(TAG, "VisitaSyncWorker: Visitas pendientes locales = ${pendingVisitas.size}")

            if (pendingVisitas.isEmpty()) {
                return Result.success()
            }

            for (visita in pendingVisitas) {
                val photoFile = File(visita.photoPath)
                if (!photoFile.exists()) {
                    Log.e(TAG, "Foto no encontrada en ruta: ${visita.photoPath}")
                    continue
                }

                Log.d(TAG, "Enviando visita ID ${visita.id}. Tamaño foto: ${photoFile.length()} bytes")

                val requestFile = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val photoBody = MultipartBody.Part.createFormData("photo", photoFile.name, requestFile)

                val statusBody = visita.status.toRequestBody("text/plain".toMediaTypeOrNull())
                val routeBody = visita.route.toRequestBody("text/plain".toMediaTypeOrNull())
                val isOpportunityBody = (if (visita.isOpportunity) "1" else "0").toRequestBody("text/plain".toMediaTypeOrNull())
                val clientIdBody = visita.clientId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                val opportunityNameBody = visita.opportunityClientName?.toRequestBody("text/plain".toMediaTypeOrNull())
                val latBody = visita.latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val lonBody = visita.longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val accBody = visita.accuracy.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val commentsBody = (visita.comments ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
                val visitedAtBody = visita.visitedAt.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = apiService.storeVisita(
                    photo = photoBody,
                    status = statusBody,
                    route = routeBody,
                    isOpportunity = isOpportunityBody,
                    clientId = clientIdBody,
                    opportunityClientName = opportunityNameBody,
                    latitude = latBody,
                    longitude = lonBody,
                    accuracy = accBody,
                    comments = commentsBody,
                    visitedAt = visitedAtBody
                )

                Log.d(TAG, "storeVisita HTTP Status: ${response.code()}")

                if (response.isSuccessful) {
                    visitaDao.markAsSynced(visita.id)
                    Log.d(TAG, "Visita local ${visita.id} sincronizada correctamente")
                } else {
                    val errBody = response.errorBody()?.string()
                    Log.e(TAG, "Error HTTP ${response.code()} en backend: $errBody")
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Fallo general en VisitaSyncWorker", e)
            Result.retry()
        }
    }
}