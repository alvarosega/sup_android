package com.alvarosega.trackingventas.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HistorialProductosResponseDto(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("cliente_id") val clienteId: Any? = null,
    @SerializedName(value = "cliente", alternate = ["cliente_nombre"]) val clienteNombre: String? = null,
    @SerializedName("codigo_cliente") val codigoCliente: String? = null,
    @SerializedName("ruta") val ruta: String? = null,
    @SerializedName("dias_historial") val diasHistorial: Int? = 30,
    @SerializedName("total_productos") val totalProductos: Int? = 0,
    @SerializedName("filtros") val filtros: FiltrosInventarioDto? = null,
    @SerializedName(value = "productos", alternate = ["items", "data"]) val productos: List<ProductoInventarioDto>? = emptyList()
)

data class FiltrosInventarioDto(
    @SerializedName("jerarquia1") val jerarquia1: List<String>? = emptyList(),
    @SerializedName("jerarquia2") val jerarquia2: List<String>? = emptyList(),
    @SerializedName("jerarquia3") val jerarquia3: List<String>? = emptyList(),
    @SerializedName("categorias") val categorias: List<String>? = emptyList()
)

data class ProductoInventarioDto(
    @SerializedName(value = "producto_id", alternate = ["id"]) val productoId: Long? = 0L,
    @SerializedName(value = "codigo", alternate = ["codigo_producto"]) val codigo: String? = "",
    @SerializedName(value = "producto", alternate = ["producto_nombre"]) val producto: String? = "Producto",
    @SerializedName("categoria") val categoria: String? = "General",
    @SerializedName("jerarquia1") val jerarquia1: String? = null,
    @SerializedName("jerarquia2") val jerarquia2: String? = null,
    @SerializedName("jerarquia3") val jerarquia3: String? = null,
    @SerializedName("frecuencia_compra") val frecuenciaCompra: Int? = 0,
    @SerializedName("monto_historico") val montoHistorico: Double? = 0.0,
    @SerializedName("total_comprado") val totalComprado: Int? = 0,
    @SerializedName("compras_desde_ultimo_stock") val comprasDesdeUltimoStock: Int? = 0,
    @SerializedName("stock_teorico_disponible") val stockTeoricoDisponible: Int? = null,
    @SerializedName("ultima_compra") val ultimaCompra: CompraItemDto? = null,
    @SerializedName("ultimo_stock") val ultimoStock: StockItemDto? = null,
    @SerializedName(value = "historial_compras", alternate = ["historial"]) val historialCompras: List<CompraItemDto>? = emptyList(),
    @SerializedName("historico_stocks") val historicoStocks: List<StockItemDto>? = emptyList()
)

val ProductoInventarioDto.historial: List<CompraItemDto>
    get() = historialCompras ?: emptyList()

typealias InventarioProductoHistorialDto = ProductoInventarioDto
typealias UltimaCompraDto = CompraItemDto
typealias HistorialItemDto = CompraItemDto

data class StockItemDto(
    @SerializedName("fecha") val fecha: String,
    @SerializedName("stock_fisico") val stockFisico: Int
)

data class CompraItemDto(
    @SerializedName("fecha") val fecha: String?,
    @SerializedName("cantidad") val cantidad: Int?
)

data class InventarioRequestDto(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("cliente_id") val clienteId: Long,
    @SerializedName("codigo_cliente") val codigoCliente: String? = null,
    @SerializedName("cliente_nombre") val clienteNombre: String? = null,
    @SerializedName("fecha_registro") val fechaRegistro: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("accuracy") val accuracy: Double? = null,
    @SerializedName("is_mock_location") val isMockLocation: Boolean = false,
    @SerializedName("observaciones") val observaciones: String? = null,
    @SerializedName("items") val items: List<InventarioItemPayloadDto>
)

data class InventarioItemPayloadDto(
    @SerializedName(value = "producto_id", alternate = ["id"]) val productoId: Long?,
    @SerializedName(value = "codigo_producto", alternate = ["codigo"]) val codigoProducto: String,
    @SerializedName(value = "producto_nombre", alternate = ["producto"]) val productoNombre: String,
    @SerializedName("categoria") val categoria: String? = null,
    @SerializedName("jerarquia1") val jerarquia1: String? = null,
    @SerializedName("jerarquia2") val jerarquia2: String? = null,
    @SerializedName("jerarquia3") val jerarquia3: String? = null,
    @SerializedName(value = "stock_fisico", alternate = ["stockFisico"]) val stockFisico: Int,
    @SerializedName("cantidad_historica") val cantidadHistorica: Int? = null,
    @SerializedName("ultima_compra_fecha") val ultimaCompraFecha: String? = null,
    @SerializedName("ultima_compra_cantidad") val ultimaCompraCantidad: Int? = null
)

data class InventarioResponseDto(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("idempotent") val idempotent: Boolean? = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: Any? = null
)
