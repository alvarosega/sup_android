package com.alvarosega.trackingventas.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PuntoDto(
    @SerializedName(value = "latitud", alternate = ["latitude", "lat"])
    val latitud: Double,
    @SerializedName(value = "longitud", alternate = ["longitude", "lng", "lon"])
    val longitud: Double
)

data class FronteraRutaDto(
    @SerializedName("ruta")
    val ruta: String? = null,
    @SerializedName("canal")
    val canal: String? = null,
    @SerializedName("version")
    val version: Int? = 1,
    @SerializedName("origen")
    val origen: String? = null,
    @SerializedName("total_puntos")
    val totalPuntos: Int? = 0,
    @SerializedName("puntos")
    val puntos: List<PuntoDto> = emptyList()
)
