package com.alvarosega.trackingventas.data.remote.dto

import com.alvarosega.trackingventas.data.local.entity.PlanRuteoEntity
import com.google.gson.Gson
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

    // Columnas adicionales para saneamiento y calidad
    @SerializedName("is_visited") val isVisited: Boolean? = null,
    @SerializedName("visitado") val visitado: Boolean? = null,
    @SerializedName("tipo_negocio") val tipoNegocio: String?,
    @SerializedName("zona") val zona: String?,
    @SerializedName("contacto") val contacto: String?,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("celular") val celular: String?,
    @SerializedName("referencia") val referencia: String?,
    @SerializedName("nombre_factura") val nombreFactura: String?,
    @SerializedName("nit") val nit: String?,
    @SerializedName("tiene_edicion_pendiente") val tieneEdicionPendiente: Boolean? = null,

    // Nuevas alertas de saneamiento enviadas desde Laravel
    @SerializedName("requiere_saneamiento") val requiereSaneamiento: Boolean? = false,
    @SerializedName("saneamiento_obligatorio") val saneamientoObligatorio: Boolean? = false,
    @SerializedName("saneamiento_alertas") val saneamientoAlertas: List<String>? = emptyList(),
    @SerializedName("saneamiento_campos_incompletos") val saneamientoCamposIncompletos: List<String>? = emptyList(),
    @SerializedName("calidad_score") val calidadScore: Int? = null
)

fun PlanRuteoDto.toEntity(): PlanRuteoEntity {
    val serverIsVisited = this.isVisited == true || this.visitado == true || this.status.equals("VISITADO", ignoreCase = true)
    val gson = Gson()
    val alertasJson = if (!this.saneamientoAlertas.isNullOrEmpty()) gson.toJson(this.saneamientoAlertas) else null
    val camposJson = if (!this.saneamientoCamposIncompletos.isNullOrEmpty()) gson.toJson(this.saneamientoCamposIncompletos) else null

    return PlanRuteoEntity(
        clientId = this.clientId,
        clientName = this.clientName ?: "Sin Nombre Comercial",
        route = this.route ?: "SIN_RUTA",
        day = this.day ?: "",
        address = this.address,
        latitude = this.latitude ?: 0.0,
        longitude = this.longitude ?: 0.0,
        status = this.status ?: "Activo",
        isVisited = serverIsVisited,
        tipoNegocio = this.tipoNegocio,
        zona = this.zona,
        contacto = this.contacto,
        telefono = this.telefono,
        celular = this.celular,
        referencia = this.referencia,
        nombreFactura = this.nombreFactura,
        nit = this.nit,
        hasPendingReview = this.tieneEdicionPendiente ?: false,
        requiereSaneamiento = (this.requiereSaneamiento == true || this.saneamientoObligatorio == true),
        saneamientoObligatorio = this.saneamientoObligatorio ?: false,
        saneamientoAlertasJson = alertasJson,
        saneamientoCamposJson = camposJson,
        calidadScore = this.calidadScore
    )
}
