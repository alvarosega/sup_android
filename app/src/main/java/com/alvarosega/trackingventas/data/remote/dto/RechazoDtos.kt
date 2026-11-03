package com.alvarosega.trackingventas.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MotivosResponseDto(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("motivos") val motivos: List<MotivoRechazoDto> = emptyList()
)

data class MotivoRechazoDto(
    @SerializedName("id") val id: Long,
    @SerializedName("codigo") val codigo: String,
    @SerializedName("nombre") val nombre: String
)

data class DistribuidoresResponseDto(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("distribuidores") val distribuidores: List<DistribuidorDto> = emptyList()
)

data class DistribuidorDto(
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String
)

data class PreventasPendientesResponseDto(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("total") val total: Int = 0,
    @SerializedName("filtros") val filtros: Map<String, Any>? = null,
    @SerializedName(value = "data", alternate = ["preventas"]) val data: List<PreventaPendienteDto> = emptyList()
)

data class PreventaPendienteDto(
    @SerializedName(value = "nro_preventa", alternate = ["pre_venta_id", "id"]) val nroPreventa: Long = 0L,
    @SerializedName(value = "fecha_preventa", alternate = ["fecha", "created_at"]) val fechaPreventa: String? = "",
    @SerializedName("dias_transcurridos") val diasTranscurridos: Int? = 0,
    @SerializedName(value = "cliente_id", alternate = ["id_cliente"]) val clienteId: String = "",
    @SerializedName("codigo_cliente") val codigoCliente: String? = "",
    @SerializedName(value = "cliente_nombre", alternate = ["razon_social"]) val clienteNombre: String? = "",
    @SerializedName("cliente") val cliente: String? = null,
    @SerializedName("ruta") val ruta: String? = "",
    @SerializedName("vendedor") val vendedor: String? = "",
    @SerializedName("tipo_sugerido") val tipoSugerido: String? = "TOTAL", // "TOTAL" o "PARCIAL"
    @SerializedName("monto_total_preventa") val montoTotalPreventa: Double? = 0.0,
    @SerializedName("monto_total_facturado") val montoTotalFacturado: Double? = 0.0,
    @SerializedName("monto_total_rechazado") val montoTotalRechazado: Double? = 0.0,
    @SerializedName("total_items_preventa") val totalItemsPreventa: Int? = 0,
    @SerializedName("total_items_facturados") val totalItemsFacturados: Int? = 0,
    @SerializedName("total_items_rechazados") val totalItemsRechazados: Int? = 0,
    @SerializedName("fecha_facturada") val fechaFacturada: String? = null,
    @SerializedName("nro_factura") val nroFactura: String? = null,
    @SerializedName("items_rechazados") val itemsRechazados: List<PreventaItemDto>? = emptyList(),
    @SerializedName("items") val items: List<PreventaItemDto>? = emptyList()
)

data class PreventaItemDto(
    @SerializedName(value = "preventa_item_id", alternate = ["id", "item_id"]) val preventaItemId: Long = 0L,
    @SerializedName(value = "producto_id", alternate = ["prod_id"]) val productoId: Long = 0L,
    @SerializedName(value = "codigo_producto", alternate = ["codigo"]) val codigoProducto: String? = "",
    @SerializedName(value = "producto_nombre", alternate = ["producto"]) val productoNombre: String? = "",
    @SerializedName("categoria") val categoria: String? = "General",
    @SerializedName(value = "cantidad_preventa", alternate = ["cantidad"]) val cantidadPreventa: Int = 0,
    @SerializedName("cantidad_facturada") val cantidadFacturada: Int? = 0,
    @SerializedName("cantidad_rechazada") val cantidadRechazada: Int? = 0,
    @SerializedName(value = "precio_unitario", alternate = ["precio"]) val precioUnitario: Double = 0.0,
    @SerializedName("monto_preventa") val montoPreventa: Double? = 0.0,
    @SerializedName(value = "monto_facturado", alternate = ["monto_final"]) val montoFacturado: Double? = 0.0,
    @SerializedName("monto_rechazado") val montoRechazado: Double? = 0.0,
    @SerializedName("es_parcial") val esParcial: Boolean? = false,
    @SerializedName("es_totalmente_rechazado") val esTotalmenteRechazado: Boolean? = false,
    @SerializedName("fecha_facturada") val fechaFacturada: String? = null,
    @SerializedName("nro_factura") val nroFactura: String? = null
)

data class RechazoItemPayloadDto(
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
    @SerializedName("nro_preventa") val nroPreventa: Long?,
    @SerializedName("cliente_id") val clienteId: String,
    @SerializedName(value = "cliente_nombre", alternate = ["cliente"]) val clienteNombre: String?,
    @SerializedName("vendedor") val vendedor: String?,
    @SerializedName("ruta") val ruta: String?,
    @SerializedName("motivo") val motivo: String,
    @SerializedName("tipo_rechazo") val tipoRechazo: String?,
    @SerializedName("total_items_rechazados") val totalItemsRechazados: Int?,
    @SerializedName("monto_total_rechazado") val montoTotalRechazado: Double?,
    @SerializedName("photo_url") val photoUrl: String?,
    @SerializedName(value = "fecha_rechazo", alternate = ["created_at"]) val fechaRechazo: String?,
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
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("fecha") val fecha: String? = null,
    @SerializedName("total") val total: Int = 0,
    @SerializedName(value = "data", alternate = ["rechazos"]) val data: List<PedidoRechazadoResponseDto> = emptyList()
)
