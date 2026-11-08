package com.alvarosega.trackingventas.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SugeridoResponseDto(
    @SerializedName(value = "status", alternate = ["success"]) val status: String? = "success",
    @SerializedName(value = "cliente_id", alternate = ["id"]) val clienteId: Long = 0L,
    @SerializedName("ruta") val ruta: String? = null,
    @SerializedName("dia_visita") val diaVisita: String? = null,
    @SerializedName("cuota_objetivo") val cuotaObjetivo: Double? = 0.0,
    @SerializedName("total_sugerido") val totalSugerido: Double? = 0.0,
    @SerializedName("total_unidades_sugeridas") val totalUnidadesSugeridas: Int? = 0,
    @SerializedName("total_skus_sugeridos") val totalSkusSugeridos: Int? = 0,
    @SerializedName(value = "sugeridos", alternate = ["items", "data"]) val sugeridos: List<ProductoSugeridoDto> = emptyList(),
    @SerializedName("resumen_cobertura") val resumenCobertura: ResumenCoberturaDto? = null
)

data class ProductoSugeridoDto(
    @SerializedName(value = "codigo", alternate = ["codigo_producto", "sku"]) val codigo: String = "",
    @SerializedName(value = "producto", alternate = ["producto_nombre", "nombre"]) val producto: String = "Producto",
    @SerializedName(value = "jerarquia1", alternate = ["categoria", "j1", "jerarquia_1", "categoria_nombre"]) val jerarquia1: String? = null,
    @SerializedName(value = "jerarquia2", alternate = ["subcategoria", "j2", "jerarquia_2"]) val jerarquia2: String? = null,
    @SerializedName(value = "jerarquia3", alternate = ["marca", "j3", "jerarquia_3"]) val jerarquia3: String? = null,
    @SerializedName("categoria") val categoria: String? = "General",
    @SerializedName(value = "cant_sugerida", alternate = ["cantidad_sugerida", "sugerido"]) val cantSugerida: Int = 0,
    @SerializedName(value = "precio_unitario", alternate = ["precio"]) val precioUnitario: Double = 0.0,
    @SerializedName(value = "monto_sugerido", alternate = ["monto_total"]) val montoSugerido: Double = 0.0,
    @SerializedName(value = "es_core_pareto", alternate = ["core_pareto"]) val esCorePareto: Boolean = false,
    @SerializedName(value = "compro_mes_actual", alternate = ["compro_mtd"]) val comproMesActual: Boolean = false,
    @SerializedName(value = "cumple_sugerido", alternate = ["cumplido"]) val cumpleSugerido: Boolean = false,
    @SerializedName("total_cantidad_3m") val totalCantidad3m: Int? = 0,
    @SerializedName("total_bs_3m") val totalBs3m: Double? = 0.0,
    @SerializedName("frecuencia_compra_3m") val frecuenciaCompra3m: Int? = 0
)

data class ResumenCoberturaDto(
    @SerializedName("total_skus_3m") val totalSkus3m: Int? = 0,
    @SerializedName("total_skus_cubiertos_mtd") val totalSkusCubiertosMtd: Int? = 0,
    @SerializedName("total_unidades_sugeridas") val totalUnidadesSugeridas: Int? = 0,
    @SerializedName("total_monto_sugerido_bs") val totalMontoSugeridoBs: Double? = 0.0,
    @SerializedName(value = "pct_cobertura_skus", alternate = ["pct_cobertura"]) val pctCoberturaSkus: Double? = 0.0
)
