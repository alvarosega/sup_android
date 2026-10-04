package com.alvarosega.trackingventas.sanitization.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alvarosega.trackingventas.sanitization.data.local.dao.ClientAuditDao
import com.alvarosega.trackingventas.sanitization.data.remote.SanitizationApiService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class SanitizationSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SanitizationSyncWorkerEntryPoint {
        fun clientAuditDao(): ClientAuditDao
        fun sanitizationApiService(): SanitizationApiService
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                SanitizationSyncWorkerEntryPoint::class.java
            )
            val clientAuditDao = entryPoint.clientAuditDao()
            val apiService = entryPoint.sanitizationApiService()

            val pendingAudits = clientAuditDao.getUnsyncedAudits(limit = 10)
            if (pendingAudits.isEmpty()) {
                return Result.success()
            }

            val syncedIds = mutableListOf<Long>()

            for (audit in pendingAudits) {
                val clientIdPart = audit.clientId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val statusPart = audit.auditStatus.toRequestBody("text/plain".toMediaTypeOrNull())
                val latPart = audit.latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val lonPart = audit.longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val accPart = audit.accuracy.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val commentsPart = audit.comments.toRequestBody("text/plain".toMediaTypeOrNull())
                val auditedAtPart = audit.auditedAt.toRequestBody("text/plain".toMediaTypeOrNull())

                val listType = object : TypeToken<List<String>>() {}.type
                val photoPaths: List<String> = try {
                    Gson().fromJson(audit.photoPathsJson, listType) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }

                val photoParts = mutableListOf<MultipartBody.Part>()
                for (path in photoPaths) {
                    val file = File(path)
                    if (file.exists()) {
                        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        val part = MultipartBody.Part.createFormData("photos[]", file.name, requestFile)
                        photoParts.add(part)
                    }
                }

                val response = apiService.uploadAudit(
                    clientId = clientIdPart,
                    auditStatus = statusPart,
                    latitude = latPart,
                    longitude = lonPart,
                    accuracy = accPart,
                    comments = commentsPart,
                    auditedAt = auditedAtPart,
                    photos = photoParts
                )

                if (response.isSuccessful) {
                    syncedIds.add(audit.id)
                }
            }

            if (syncedIds.isNotEmpty()) {
                clientAuditDao.markAsSynced(syncedIds)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}