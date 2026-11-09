package com.alvarosega.trackingventas.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HistorialProductosResponseDto(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("cliente_id") val clienteId: Any? = null,
    @SerializedName(value = "cliente", alternate = ["cliente_nombre"]) val clienteNombre: String? = null,
    @SerializedName("codigo_cliente") val codigoCliente: String? = null,
    @SerializedName("ruta") val ruta: String? = null,
    @SerializedName("dias_historial") val diasHistorial: Int? = 90,
    @SerializedName("total_productos") val totalProductos: Int? = 0,
    @SerializedName("filtros") val filtros: FiltrosInventarioDto? = null,
    @SerializedName(value = "productos", alternate = ["items", "data", "productos_historial", "historial_productos", "historial", "list"]) val productos: List<ProductoInventarioDto>? = emptyList()
)

data class FiltrosInventarioDto(
    @SerializedName("jerarquia1") val jerarquia1: List<String>? = emptyList(),
    @SerializedName("jerarquia2") val jerarquia2: List<String>? = emptyList(),
    @SerializedName("jerarquia3") val jerarquia3: List<String>? = emptyList(),
    @SerializedName("categorias") val categorias: List<String>? = emptyList()
)

data class ProductoInventarioDto(
    @SerializedName(value = "producto_id", alternate = ["id", "codigo", "codigo_producto"]) val productoId: Long? = 0L,
    @SerializedName(value = "codigo", alternate = ["codigo_producto", "sku", "id"]) val codigo: String? = "",
    @SerializedName(value = "producto", alternate = ["producto_nombre", "nombre", "descripcion"]) val producto: String? = "Producto",
    @SerializedName(value = "categoria", alternate = ["categoria_nombre", "cat"]) val categoria: String? = "General",
    @SerializedName(value = "jerarquia1", alternate = ["jerarquia_1", "j1", "jerarquia_1_nombre"]) val jerarquia1: String? = null,
    @SerializedName(value = "jerarquia2", alternate = ["jerarquia_2", "j2", "subcategoria"]) val jerarquia2: String? = null,
    @SerializedName(value = "jerarquia3", alternate = ["jerarquia_3", "j3", "marca"]) val jerarquia3: String? = null,
    @SerializedName("frecuencia_compra") val frecuenciaCompra: Int? = 0,
    @SerializedName("monto_historico") val montoHistorico: Double? = 0.0,
    @SerializedName("total_comprado") val totalComprado: Int? = 0,
    @SerializedName("compras_desde_ultimo_stock") val comprasDesdeUltimoStock: Int? = 0,
    @SerializedName("stock_teorico_disponible") val stockTeoricoDisponible: Int? = null,
    @SerializedName(value = "ultima_compra", alternate = ["latest_purchase", "last_purchase"]) val ultimaCompra: CompraItemDto? = null,
    @SerializedName(value = "ultimo_stock", alternate = ["latest_stock", "last_stock"]) val ultimoStock: StockItemDto? = null,
    @SerializedName(value = "historial_compras", alternate = ["historial", "compras"]) val historialCompras: List<CompraItemDto>? = emptyList(),
    @SerializedName("historico_stocks") val historicoStocks: List<StockItemDto>? = emptyList()
)

val ProductoInventarioDto.historial: List<CompraItemDto>
    get() = historialCompras ?: emptyList()

typealias InventarioProductoHistorialDto = ProductoInventarioDto
typealias UltimaCompraDto = CompraItemDto
typealias HistorialItemDto = CompraItemDto

data class StockItemDto(
    @SerializedName(value = "fecha", alternate = ["created_at", "fecha_stock", "date"]) val fecha: String,
    @SerializedName(value = "stock_fisico", alternate = ["cant", "stock"]) val stockFisico: Int
)

data class CompraItemDto(
    @SerializedName(value = "fecha", alternate = ["created_at", "fecha_compra", "date"]) val fecha: String?,
    @SerializedName(value = "cantidad", alternate = ["cant", "cantidad_comprada", "unidades", "qty"]) val cantidad: Int?
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
