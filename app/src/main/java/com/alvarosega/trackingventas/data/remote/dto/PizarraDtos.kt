package com.alvarosega.trackingventas.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PizarraResponseDto(
    @SerializedName(value = "status", alternate = ["success"]) val status: String? = "success",
    @SerializedName("fecha") val fecha: String? = null,
    @SerializedName("dia_semana") val diaSemana: String? = null,
    @SerializedName("mi_ruta") val miRuta: MiRutaPizarraDto? = null,
    @SerializedName(value = "resumen_kpis", alternate = ["kpis_globales"]) val resumenKpis: ResumenKpisDto? = null,
    @SerializedName(value = "ranking", alternate = ["rutas", "data"]) val ranking: List<RankingRutaDto> = emptyList()
)

data class MiRutaPizarraDto(
    @SerializedName("ruta") val ruta: String? = null,
    @SerializedName("vendedor") val vendedor: String? = null,
    @SerializedName("canal") val canal: String? = null,
    @SerializedName("score_total") val scoreTotal: Int? = 0,
    @SerializedName("aprobado") val aprobado: Boolean? = false,
    @SerializedName("rank") val rank: Int? = 0,
    @SerializedName("total_rutas") val totalRutas: Int? = 0,
    @SerializedName("kpis") val kpis: KpisDetalleDto? = null
)

data class KpisDetalleDto(
    @SerializedName("kpi1_horario") val kpi1Horario: KpiHorarioDto? = null,
    @SerializedName("kpi2_visitas") val kpi2Visitas: KpiVisitasDto? = null,
    @SerializedName("kpi3_objetivo") val kpi3Objetivo: KpiObjetivoDto? = null,
    @SerializedName("kpi4_efectividad") val kpi4Efectividad: KpiEfectividadDto? = null,
    @SerializedName("kpi5_compra_minima") val kpi5CompraMinima: KpiCompraMinimaDto? = null,
    @SerializedName("kpi6_reposicion") val kpi6Reposicion: KpiReposicionDto? = null,
    @SerializedName("kpi7_ampliacion") val kpi7Ampliacion: KpiAmpliacionDto? = null,
    @SerializedName("kpi8_covers") val kpi8Covers: KpiCoversDto? = null
)

data class KpiHorarioDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("puntos") val puntos: Int? = 0,
    @SerializedName("puntos_max") val puntosMax: Int? = 0,
    @SerializedName("cumple") val cumple: Boolean? = false,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("primer_checkin") val primerCheckin: String? = null,
    @SerializedName("hora_web") val horaWeb: String? = null,
    @SerializedName("hora_movil") val horaMovil: String? = null,
    @SerializedName("umbral") val umbral: String? = null,
    @SerializedName("retraso_minutos") val retrasoMinutos: Int? = 0
)

data class KpiVisitasDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("puntos") val puntos: Int? = 0,
    @SerializedName("puntos_max") val puntosMax: Int? = 0,
    @SerializedName("cumple") val cumple: Boolean? = false,
    @SerializedName("cartera_plan") val carteraPlan: Int? = 0,
    @SerializedName("visitados_activos") val visitadosActivos: Int? = 0,
    @SerializedName("visitados_web") val visitadosWeb: Int? = 0,
    @SerializedName("visitados_app") val visitadosApp: Int? = 0,
    @SerializedName("total_visitados") val totalVisitados: Int? = 0,
    @SerializedName("cobertura_pct") val coberturaPct: Double? = 0.0
)

data class KpiObjetivoDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("puntos") val puntos: Int? = 0,
    @SerializedName("puntos_max") val puntosMax: Int? = 0,
    @SerializedName("cumple") val cumple: Boolean? = false,
    @SerializedName("cuota_diaria") val cuotaDiaria: Double? = 0.0,
    @SerializedName("venta_hoy") val ventaHoy: Double? = 0.0,
    @SerializedName("alcance_pct") val alcancePct: Double? = 0.0,
    @SerializedName("meta_mensual") val metaMensual: Double? = 0.0,
    @SerializedName("facturado_previo") val facturadoPrevio: Double? = 0.0,
    @SerializedName("brecha") val brecha: Double? = 0.0,
    @SerializedName("dias_habiles_restantes") val diasHabilesRestantes: Int? = 0
)

data class KpiEfectividadDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("puntos") val puntos: Int? = 0,
    @SerializedName("puntos_max") val puntosMax: Int? = 0,
    @SerializedName("cumple") val cumple: Boolean? = false,
    @SerializedName("cartera_plan") val carteraPlan: Int? = 0,
    @SerializedName("compradores") val compradores: Int? = 0,
    @SerializedName("compradores_sobre_70") val compradoresSobre70: Int? = 0,
    @SerializedName("compradores_bajo_70") val compradoresBajo70: Int? = 0,
    @SerializedName("efectividad_pct") val efectividadPct: Double? = 0.0,
    @SerializedName("umbral_pct") val umbralPct: Double? = 0.0
)

data class KpiCompraMinimaDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("puntos") val puntos: Int? = 0,
    @SerializedName("puntos_max") val puntosMax: Int? = 0,
    @SerializedName("cumple") val cumple: Boolean? = false,
    @SerializedName("compradores") val compradores: Int? = 0,
    @SerializedName("compradores_bajo_umbral") val compradoresBajoUmbral: Int? = 0,
    @SerializedName("compradores_sobre_umbral") val compradoresSobreUmbral: Int? = 0,
    @SerializedName("monto_minimo_estandar") val montoMinimoEstandar: Double? = 0.0
)

data class KpiReposicionDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("puntos") val puntos: Int? = 0,
    @SerializedName("puntos_max") val puntosMax: Int? = 0,
    @SerializedName("cumple") val cumple: Boolean? = false,
    @SerializedName("evaluado") val evaluado: Boolean? = false,
    @SerializedName("compradores") val compradores: Int? = 0,
    @SerializedName("evaluables") val evaluables: Int? = 0,
    @SerializedName("tasa_pct") val tasaPct: Double? = 0.0,
    @SerializedName("umbral_pct") val umbralPct: Double? = 0.0,
    @SerializedName("skus_match") val skusMatch: Int? = 0,
    @SerializedName("skus_esperados") val skusEsperados: Int? = 0
)

data class KpiAmpliacionDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("puntos") val puntos: Int? = 0,
    @SerializedName("puntos_max") val puntosMax: Int? = 0,
    @SerializedName("cumple") val cumple: Boolean? = false,
    @SerializedName("evaluado") val evaluado: Boolean? = false,
    @SerializedName("pendiente_desarrollo") val pendienteDesarrollo: Boolean? = false,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("tasa_pct") val tasaPct: Double? = 0.0,
    @SerializedName("umbral_pct") val umbralPct: Double? = 0.0,
    @SerializedName("skus_ampliacion") val skusAmpliacion: Int? = 0,
    @SerializedName("total_skus") val totalSkus: Int? = 0
)

data class KpiCoversDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("puntos") val puntos: Int? = 0,
    @SerializedName("puntos_max") val puntosMax: Int? = 0,
    @SerializedName("cumple") val cumple: Boolean? = false,
    @SerializedName("evaluado") val evaluado: Boolean? = false,
    @SerializedName("total_compradores") val totalCompradores: Int? = 0,
    @SerializedName("cumplen") val cumplen: Int? = 0,
    @SerializedName("tasa_pct") val tasaPct: Double? = 0.0,
    @SerializedName("umbral_pct") val umbralPct: Double? = 0.0
)

data class ResumenKpisDto(
    @SerializedName("kpi1_horario") val kpi1Horario: ResumenHorarioDto? = null,
    @SerializedName("kpi2_visitas") val kpi2Visitas: ResumenVisitasDto? = null,
    @SerializedName("kpi3_objetivo") val kpi3Objetivo: ResumenObjetivoDto? = null,
    @SerializedName("kpi4_efectividad") val kpi4Efectividad: ResumenEfectividadDto? = null,
    @SerializedName("kpi5_compra_minima") val kpi5CompraMinima: ResumenCompraMinimaDto? = null,
    @SerializedName("kpi6_reposicion") val kpi6Reposicion: ResumenReposicionDto? = null,
    @SerializedName("kpi7_ampliacion") val kpi7Ampliacion: ResumenAmpliacionDto? = null,
    @SerializedName("kpi8_covers") val kpi8Covers: ResumenCoversDto? = null
)

data class ResumenHorarioDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("puntos_max") val puntosMax: Int? = 0,
    @SerializedName("rutas_puntuales") val rutasPuntuales: Int? = 0,
    @SerializedName("rutas_atrasadas") val rutasAtrasadas: Int? = 0,
    @SerializedName("rutas_sin_marcar") val rutasSinMarcar: Int? = 0
)

data class ResumenVisitasDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("puntos_max") val puntosMax: Int? = 0,
    @SerializedName("cartera_total") val carteraTotal: Int? = 0,
    @SerializedName("visitados_activos") val visitadosActivos: Int? = 0,
    @SerializedName("visitados_web") val visitadosWeb: Int? = 0,
    @SerializedName("visitados_app") val visitadosApp: Int? = 0,
    @SerializedName("cobertura_promedio_pct") val coberturaPromedioPct: Double? = 0.0
)

data class ResumenObjetivoDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("puntos_max") val puntosMax: Int? = 0,
    @SerializedName("cuota_diaria_total") val cuotaDiariaTotal: Double? = 0.0,
    @SerializedName("venta_total") val ventaTotal: Double? = 0.0,
    @SerializedName("alcance_promedio_pct") val alcancePromedioPct: Double? = 0.0
)

data class ResumenEfectividadDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("puntos_max") val puntosMax: Int? = 0,
    @SerializedName("cartera_total") val carteraTotal: Int? = 0,
    @SerializedName("total_compradores") val totalCompradores: Int? = 0,
    @SerializedName("efectividad_global_pct") val efectividadGlobalPct: Double? = 0.0
)

data class ResumenCompraMinimaDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("puntos_max") val puntosMax: Int? = 0,
    @SerializedName("compradores_bajo_70") val compradoresBajo70: Int? = 0,
    @SerializedName("compradores_sobre_70") val compradoresSobre70: Int? = 0,
    @SerializedName("monto_minimo_estandar") val montoMinimoEstandar: Double? = 0.0
)

data class ResumenReposicionDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("puntos_max") val puntosMax: Int? = 0,
    @SerializedName("rutas_cumplen") val rutasCumplen: Int? = 0,
    @SerializedName("tasa_promedio_pct") val tasaPromedioPct: Double? = 0.0
)

data class ResumenAmpliacionDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("puntos_max") val puntosMax: Int? = 0,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("pendiente_desarrollo") val pendienteDesarrollo: Boolean? = false
)

data class ResumenCoversDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("puntos_max") val puntosMax: Int? = 0,
    @SerializedName("rutas_cumplen") val rutasCumplen: Int? = 0,
    @SerializedName("tasa_promedio_pct") val tasaPromedioPct: Double? = 0.0
)

data class RankingRutaDto(
    @SerializedName("rank") val rank: Int? = 0,
    @SerializedName("ruta") val ruta: String? = null,
    @SerializedName("vendedor") val vendedor: String? = null,
    @SerializedName("canal") val canal: String? = null,
    @SerializedName("score_total") val scoreTotal: Int? = 0,
    @SerializedName("aprobado") val aprobado: Boolean? = false,
    @SerializedName("es_mi_ruta") val esMiRuta: Boolean? = false
)
