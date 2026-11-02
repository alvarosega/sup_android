package com.alvarosega.trackingventas.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HistorialProductosResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("cliente_id") val clienteId: Any?,
    @SerializedName(value = "cliente", alternate = ["cliente_nombre"]) val clienteNombre: String?,
    @SerializedName("ruta") val ruta: String? = null,
    @SerializedName("dias_historial") val diasHistorial: Int? = 30,
    @SerializedName("total_productos") val totalProductos: Int? = 0,
    @SerializedName("productos") val productos: List<InventarioProductoHistorialDto>? = emptyList()
)

data class InventarioProductoHistorialDto(
    @SerializedName(value = "producto_id", alternate = ["id"]) val productoId: Long? = 0L,
    @SerializedName(value = "codigo", alternate = ["codigo_producto"]) val codigo: String? = "",
    @SerializedName(value = "producto", alternate = ["producto_nombre"]) val producto: String? = "Producto",
    @SerializedName("categoria") val categoria: String? = "General",
    @SerializedName("frecuencia_compra") val frecuenciaCompra: Int? = 0,
    @SerializedName("monto_historico") val montoHistorico: Double? = 0.0,
    @SerializedName("total_comprado") val totalComprado: Int? = 0,
    @SerializedName("ultima_compra") val ultimaCompra: UltimaCompraDto? = null,
    @SerializedName("historial") val historial: List<HistorialItemDto>? = emptyList()
)

data class UltimaCompraDto(
    @SerializedName("fecha") val fecha: String?,
    @SerializedName("cantidad") val cantidad: Int?
)

data class HistorialItemDto(
    @SerializedName("fecha") val fecha: String?,
    @SerializedName("cantidad") val cantidad: Int?
)

data class InventarioItemPayloadDto(
    @SerializedName("producto_id") val productoId: Long?,
    @SerializedName("codigo_producto") val codigoProducto: String,
    @SerializedName("producto_nombre") val productoNombre: String,
    @SerializedName("categoria") val categoria: String?,
    @SerializedName("stock_fisico") val stockFisico: Int
)

data class InventarioResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("id") val id: Long?,
    @SerializedName("uuid") val uuid: String?
)
