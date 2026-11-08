package com.alvarosega.trackingventas.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SugeridoResponseDto(
    @SerializedName(value = "status", alternate = ["success"]) val status: String? = "success",
    @SerializedName("cliente") val cliente: String? = null,
    @SerializedName(value = "cliente_id", alternate = ["id"]) val clienteId: Long = 0L,
    @SerializedName("ruta") val ruta: String? = null,
    @SerializedName("dia_visita") val diaVisita: String? = null,
    @SerializedName("total_sugerido") val totalSugerido: Double? = 0.0,
    @SerializedName("total_skus_sugeridos") val totalSkusSugeridos: Int? = 0,
    @SerializedName(value = "sugeridos", alternate = ["items", "data"]) val sugeridos: List<ProductoSugeridoDto> = emptyList()
)

data class ProductoSugeridoDto(
    @SerializedName(value = "codigo", alternate = ["codigo_producto", "sku"]) val codigo: String = "",
    @SerializedName(value = "producto", alternate = ["producto_nombre", "nombre"]) val producto: String = "Producto",
    @SerializedName(value = "jerarquia_1", alternate = ["jerarquia1", "j1", "categoria"]) val jerarquia1: String? = null,
    @SerializedName(value = "cant_sugerida", alternate = ["cantidad_sugerida", "sugerido"]) val cantSugerida: Int = 0,
    @SerializedName(value = "compro_mes_actual", alternate = ["compro_mtd"]) val comproMesActual: Boolean = false,
    @SerializedName(value = "cantidad_comprada_mes_actual", alternate = ["comprado_mes"]) val cantidadCompradaMesActual: Int = 0
)
