package com.alvarosega.trackingventas.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MotivosResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("motivos") val motivos: List<MotivoRechazoDto> = emptyList()
)

data class MotivoRechazoDto(
    @SerializedName("id") val id: Long,
    @SerializedName("codigo") val codigo: String,
    @SerializedName("nombre") val nombre: String
)

data class PreventasPendientesResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("total") val total: Int = 0,
    @SerializedName("data") val data: List<PreventaPendienteDto> = emptyList()
)

data class PreventaPendienteDto(
    @SerializedName("nro_preventa") val nroPreventa: String,
    @SerializedName("fecha_preventa") val fechaPreventa: String,
    @SerializedName("cliente_id") val clienteId: String,
    @SerializedName("codigo_cliente") val codigoCliente: String?,
    @SerializedName("cliente_nombre") val clienteNombre: String?,
    @SerializedName("ruta") val ruta: String?,
    @SerializedName("vendedor") val vendedor: String?,
    @SerializedName("tipo_sugerido") val tipoSugerido: String?, // "TOTAL" o "PARCIAL"
    @SerializedName("monto_total_preventa") val montoTotalPreventa: Double?,
    @SerializedName("monto_total_facturado") val montoTotalFacturado: Double?,
    @SerializedName("monto_total_rechazado") val montoTotalRechazado: Double?,
    @SerializedName("total_items_preventa") val totalItemsPreventa: Int?,
    @SerializedName("total_items_facturados") val totalItemsFacturados: Int?,
    @SerializedName("total_items_rechazados") val totalItemsRechazados: Int?,
    @SerializedName("items_rechazados") val itemsRechazados: List<PreventaItemDto> = emptyList(),
    @SerializedName("items") val items: List<PreventaItemDto> = emptyList()
)

data class PreventaItemDto(
    @SerializedName("preventa_item_id") val preventaItemId: Long,
    @SerializedName("producto_id") val productoId: Long,
    @SerializedName("codigo_producto") val codigoProducto: String,
    @SerializedName("producto_nombre") val productoNombre: String,
    @SerializedName("categoria") val categoria: String?,
    @SerializedName("cantidad_preventa") val cantidadPreventa: Int,
    @SerializedName("cantidad_facturada") val cantidadFacturada: Int? = 0,
    @SerializedName("cantidad_rechazada") val cantidadRechazada: Int? = 0,
    @SerializedName("precio_unitario") val precioUnitario: Double,
    @SerializedName("monto_preventa") val montoPreventa: Double?,
    @SerializedName("monto_facturado") val montoFacturado: Double?,
    @SerializedName("monto_rechazado") val montoRechazado: Double?,
    @SerializedName("es_parcial") val esParcial: Boolean? = false,
    @SerializedName("es_totalmente_rechazado") val esTotalmenteRechazado: Boolean? = false
)

data class RechazoItemPayloadDto(
    @SerializedName("preventa_item_id") val preventaItemId: Long,
    @SerializedName("producto_id") val productoId: Long,
    @SerializedName("codigo_producto") val codigoProducto: String,
    @SerializedName("producto_nombre") val productoNombre: String,
    @SerializedName("categoria") val categoria: String?,
    @SerializedName("cantidad_preventa") val cantidadPreventa: Int,
    @SerializedName("cantidad_rechazada") val cantidadRechazada: Int,
    @SerializedName("precio_unitario") val precioUnitario: Double,
    @SerializedName("motivo_especifico") val motivoEspecifico: String?
)

data class PedidoRechazadoResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("uuid") val uuid: String,
    @SerializedName("cliente_id") val clienteId: String,
    @SerializedName("cliente_nombre") val clienteNombre: String?,
    @SerializedName("vendedor") val vendedor: String?,
    @SerializedName("ruta") val ruta: String?,
    @SerializedName("motivo") val motivo: String,
    @SerializedName("tipo_rechazo") val tipoRechazo: String?,
    @SerializedName("total_items_rechazados") val totalItemsRechazados: Int?,
    @SerializedName("monto_total_rechazado") val montoTotalRechazado: Double?,
    @SerializedName("photo_url") val photoUrl: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("items") val items: List<PedidoRechazadoItemResponseDto> = emptyList()
)

data class PedidoRechazadoItemResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("producto_id") val productoId: Long,
    @SerializedName("producto_nombre") val productoNombre: String,
    @SerializedName("cantidad_rechazada") val cantidadRechazada: Int,
    @SerializedName("monto_rechazado") val montoRechazado: Double?
)

data class HistorialResponseDto(
    @SerializedName("fecha") val fecha: String?,
    @SerializedName("total") val total: Int = 0,
    @SerializedName("rechazos") val rechazos: List<PedidoRechazadoResponseDto> = emptyList()
)
