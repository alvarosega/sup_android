package com.alvarosega.trackingventas.data.remote.dto

import com.alvarosega.trackingventas.data.local.entity.PlanRuteoEntity
import com.google.gson.annotations.SerializedName

data class PlanRuteoDto(
    @SerializedName("cliente_id") val clientId: Long,
    @SerializedName("cliente") val clientName: String?,
    @SerializedName("ruta") val route: String?,
    @SerializedName("dia") val day: String?,
    @SerializedName("direccion") val address: String?,
    @SerializedName("latitud") val latitude: Double?,
    @SerializedName("longitud") val longitude: Double?,
    @SerializedName("estado") val status: String?,

    // Columnas adicionales para saneamiento
    @SerializedName("tipo_negocio") val tipoNegocio: String?,
    @SerializedName("zona") val zona: String?,
    @SerializedName("contacto") val contacto: String?,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("celular") val celular: String?,
    @SerializedName("referencia") val referencia: String?,
    @SerializedName("nombre_factura") val nombreFactura: String?,
    @SerializedName("nit") val nit: String?,
    @SerializedName("tiene_edicion_pendiente") val tieneEdicionPendiente: Boolean? = null
)

fun PlanRuteoDto.toEntity(): PlanRuteoEntity {
    return PlanRuteoEntity(
        clientId = this.clientId,
        clientName = this.clientName ?: "Sin Nombre Comercial",
        route = this.route ?: "SIN_RUTA",
        day = this.day ?: "",
        address = this.address,
        latitude = this.latitude ?: 0.0,
        longitude = this.longitude ?: 0.0,
        status = this.status ?: "Activo",
        isVisited = false,
        tipoNegocio = this.tipoNegocio,
        zona = this.zona,
        contacto = this.contacto,
        telefono = this.telefono,
        celular = this.celular,
        referencia = this.referencia,
        nombreFactura = this.nombreFactura,
        nit = this.nit,
        hasPendingReview = this.tieneEdicionPendiente ?: false
    )
}