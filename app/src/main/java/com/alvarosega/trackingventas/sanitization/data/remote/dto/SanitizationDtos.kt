package com.alvarosega.trackingventas.sanitization.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ReferenceClientDto(
    @SerializedName("id_cliente") val clientId: Long,
    @SerializedName("ruta") val route: String,
    @SerializedName("dia") val day: String,
    @SerializedName("direccion") val address: String,
    @SerializedName("latitud") val latitude: Double?,
    @SerializedName("longitud") val longitude: Double?
)

data class AuditUploadResponse(
    @SerializedName("message") val message: String,
    @SerializedName("audit_id") val auditId: Long?
)