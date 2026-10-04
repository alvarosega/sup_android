package com.alvarosega.trackingventas.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OportunidadesResponse(
    @SerializedName("ruta") val ruta: String,
    @SerializedName("dia") val dia: String,
    @SerializedName("total") val total: Int,
    @SerializedName("oportunidades") val oportunidades: List<OportunidadDto>
)

data class OportunidadDto(
    @SerializedName("client_id") val clientId: Long,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)