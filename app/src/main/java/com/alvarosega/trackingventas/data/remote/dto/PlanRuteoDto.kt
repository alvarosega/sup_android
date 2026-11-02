package com.alvarosega.trackingventas.data.remote.dto

import com.alvarosega.trackingventas.data.local.entity.PlanRuteoEntity
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class PlanRuteoDto(
    @SerializedName(value = "cliente_id", alternate = ["id_cliente", "id"]) val clientId: Long,
    @SerializedName(value = "cliente", alternate = ["nombre", "nombre_cliente", "razon_social"]) val clientName: String?,
    @SerializedName("ruta") val route: String?,
    @SerializedName("dia") val day: String?,
    @SerializedName("direccion") val address: String?,
    @SerializedName("latitud") val latitude: Double?,
    @SerializedName("longitud") val longitude: Double?,
    @SerializedName(value = "status", alternate = ["estado", "status_visita"]) val status: String?,

    // Columnas adicionales para saneamiento y calidad
    @SerializedName("is_visited") val isVisited: Boolean? = null,
    @SerializedName("visitado") val visitado: Boolean? = null,

    @SerializedName("tipo_negocio") val tipoNegocioRaw: String? = null,
    @SerializedName("tipo") val tipo: String? = null,
    @SerializedName("rubro") val rubro: String? = null,

    @SerializedName("zona") val zonaRaw: String? = null,
    @SerializedName("zona_comercial") val zonaComercial: String? = null,
    @SerializedName("nombre_zona") val nombreZona: String? = null,
    @SerializedName("zone") val zone: String? = null,

    @SerializedName("contacto") val contacto: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("celular") val celular: String? = null,
    @SerializedName("referencia") val referencia: String? = null,
    @SerializedName("nombre_factura") val nombreFactura: String? = null,
    @SerializedName("nit") val nit: String? = null,
    @SerializedName("tiene_edicion_pendiente") val tieneEdicionPendiente: Boolean? = null,
    @SerializedName("has_pending_review") val hasPendingReview: Boolean? = null,
    @SerializedName("en_revision") val enRevision: Boolean? = null,
    @SerializedName("pendiente_revision") val pendienteRevision: Boolean? = null,

    // Nuevas alertas de saneamiento enviadas desde Laravel
    @SerializedName("requiere_saneamiento") val requiereSaneamiento: Boolean? = false,
    @SerializedName("saneamiento_obligatorio") val saneamientoObligatorio: Boolean? = false,
    @SerializedName("saneamiento_alertas") val saneamientoAlertas: List<String>? = emptyList(),
    @SerializedName("saneamiento_campos_incompletos") val saneamientoCamposIncompletos: List<String>? = emptyList(),
    @SerializedName("calidad_score") val calidadScore: Int? = null
)

fun PlanRuteoDto.toEntity(): PlanRuteoEntity {
    val serverStatus = this.status?.trim()?.lowercase() ?: "unvisited"
    val serverIsVisited = this.isVisited == true ||
            this.visitado == true ||
            this.status.equals("VISITADO", ignoreCase = true) ||
            (this.status.equals("visited", ignoreCase = true) && this.isVisited == true)

    val isPendingReview = this.tieneEdicionPendiente == true || this.hasPendingReview == true || this.enRevision == true || this.pendienteRevision == true
    val gson = Gson()
    val alertasJson = if (!this.saneamientoAlertas.isNullOrEmpty()) gson.toJson(this.saneamientoAlertas) else null
    val camposJson = if (!this.saneamientoCamposIncompletos.isNullOrEmpty()) gson.toJson(this.saneamientoCamposIncompletos) else null

    val finalZona = this.zonaRaw?.ifBlank { null }
        ?: this.zonaComercial?.ifBlank { null }
        ?: this.nombreZona?.ifBlank { null }
        ?: this.zone?.ifBlank { null }

    val finalTipoNegocio = this.tipoNegocioRaw?.ifBlank { null }
        ?: this.tipo?.ifBlank { null }
        ?: this.rubro?.ifBlank { null }

    return PlanRuteoEntity(
        clientId = this.clientId,
        clientName = this.clientName ?: "Sin Nombre Comercial",
        route = this.route ?: "SIN_RUTA",
        day = this.day ?: "",
        address = this.address,
        latitude = this.latitude ?: 0.0,
        longitude = this.longitude ?: 0.0,
        status = serverStatus,
        isVisited = serverIsVisited,
        tipoNegocio = finalTipoNegocio,
        zona = finalZona,
        contacto = this.contacto,
        telefono = this.telefono,
        celular = this.celular,
        referencia = this.referencia,
        nombreFactura = this.nombreFactura,
        nit = this.nit,
        hasPendingReview = isPendingReview,
        requiereSaneamiento = (this.requiereSaneamiento == true || this.saneamientoObligatorio == true),
        saneamientoObligatorio = this.saneamientoObligatorio ?: false,
        saneamientoAlertasJson = alertasJson,
        saneamientoCamposJson = camposJson,
        calidadScore = this.calidadScore
    )
}
