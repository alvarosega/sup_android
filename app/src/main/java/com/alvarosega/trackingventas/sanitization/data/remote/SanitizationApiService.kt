package com.alvarosega.trackingventas.sanitization.data.remote

import com.alvarosega.trackingventas.sanitization.data.remote.dto.AuditUploadResponse
import com.alvarosega.trackingventas.sanitization.data.remote.dto.ReferenceClientDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface SanitizationApiService {

    @GET("api/sanitization/clients")
    suspend fun getReferenceClients(
        @Query("day") day: String? = null
    ): Response<List<ReferenceClientDto>>

    @Multipart
    @POST("api/sanitization/audit")
    suspend fun uploadAudit(
        @Part("client_id") clientId: RequestBody,
        @Part("audit_status") auditStatus: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("accuracy") accuracy: RequestBody,
        @Part("comments") comments: RequestBody,
        @Part("audited_at") auditedAt: RequestBody,
        @Part photos: List<MultipartBody.Part>
    ): Response<AuditUploadResponse>
}