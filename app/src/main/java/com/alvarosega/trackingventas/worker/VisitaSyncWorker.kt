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
                val photoFiles = visita.getPhotoPathList().map { File(it) }.filter { it.exists() }
                if (photoFiles.isEmpty()) {
                    Log.e(TAG, "Ninguna foto encontrada para visita ID ${visita.id}: ${visita.photoPath}")
                    continue
                }

                Log.d(TAG, "Enviando visita ID ${visita.id}. Total fotos a procesar: ${photoFiles.size}")
                var allPhotosUploadedSuccessfully = true

                for ((index, photoFile) in photoFiles.withIndex()) {
                    val photoRequestBody = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val photoUuid = if (index == 0 && visita.uuid.length == 36) {
                        visita.uuid
                    } else {
                        java.util.UUID.randomUUID().toString()
                    }

                    val builder = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("photo", photoFile.name, photoRequestBody)
                        .addFormDataPart("uuid", photoUuid)
                        .addFormDataPart("status", visita.status)
                        .addFormDataPart("route", visita.route)
                        .addFormDataPart("is_opportunity", if (visita.isOpportunity) "1" else "0")
                        .addFormDataPart("latitude", visita.latitude.toString())
                        .addFormDataPart("longitude", visita.longitude.toString())
                        .addFormDataPart("accuracy", visita.accuracy.toString())
                        .addFormDataPart("is_mock_location", if (visita.isMockLocation) "1" else "0")
                        .addFormDataPart("comments", visita.comments ?: "")
                        .addFormDataPart("visited_at", visita.visitedAt)

                    if (visita.clientId != null) {
                        builder.addFormDataPart("client_id", visita.clientId.toString())
                    }
                    if (!visita.opportunityClientName.isNullOrBlank()) {
                        builder.addFormDataPart("opportunity_client_name", visita.opportunityClientName)
                    }
                    if (visita.distanceToClient != null) {
                        builder.addFormDataPart("distance_to_client", visita.distanceToClient.toString())
                    }

                    val response = apiService.storeVisita(builder.build())

                    Log.d(TAG, "storeVisita foto [$index/${photoFiles.size}] HTTP Status: ${response.code()}")

                    if (response.isSuccessful) {
                        Log.d(TAG, "Foto [$index/${photoFiles.size}] para visita ID ${visita.id} enviada correctamente")
                    } else {
                        allPhotosUploadedSuccessfully = false
                        val errBody = response.errorBody()?.string()
                        Log.e(TAG, "Error HTTP ${response.code()} en foto [$index]: $errBody")
                    }
                }

                if (allPhotosUploadedSuccessfully) {
                    visitaDao.markAsSynced(visita.id)
                    Log.d(TAG, "Visita local ${visita.id} con ${photoFiles.size} foto(s) sincronizada correctamente")
                } else {
                    Log.e(TAG, "Fallo al subir alguna foto de la visita ${visita.id}. Se reintentará cuando el servidor esté disponible.")
                    return Result.retry()
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Fallo general en VisitaSyncWorker", e)
            Result.retry()
        }
    }
}