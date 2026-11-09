package com.alvarosega.trackingventas.data.remote

import com.alvarosega.trackingventas.data.remote.dto.OportunidadesResponse
import com.alvarosega.trackingventas.data.remote.dto.PlanRuteoDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface VisitaApiService {

    @GET("api/plan-ruteo")
    suspend fun getPlanRuteo(): Response<List<PlanRuteoDto>>

    @GET("api/visitas/clientes")
    suspend fun getVisitasClientes(
        @Query("dia") dia: String? = null
    ): Response<List<PlanRuteoDto>>

    @GET("api/pizarra")
    suspend fun getPizarraResumen(
        @Query("fecha") fecha: String? = null,
        @Query("ruta") ruta: String? = null
    ): Response<com.alvarosega.trackingventas.data.remote.dto.PizarraResponseDto>

    @GET("api/visitas/clientes/{clienteId}/sugerido")
    suspend fun getSugeridoCliente(
        @retrofit2.http.Path("clienteId") clienteId: Long,
        @Query("ruta") ruta: String? = null,
        @Query("dia") dia: String? = null
    ): Response<com.alvarosega.trackingventas.data.remote.dto.SugeridoResponseDto>

    @GET("api/fronteras/ruta")
    suspend fun getFronteraRuta(
        @Query("ruta") rutaOpcional: String? = null
    ): Response<ResponseBody>

    @GET("api/oportunidades")
    suspend fun getOportunidades(
        @Query("dia") dia: String? = null
    ): Response<OportunidadesResponse>

    @Headers(
        "Accept: application/json",
        "User-Agent: Mozilla/5.0 (Android; Mobile; TrackingVentas)"
    )
    @POST("api/visitas")
    suspend fun storeVisita(
        @Body body: MultipartBody
    ): Response<ResponseBody>

    // 1. ALTA: Envío MultipartBody dinámico y limpio (inmune a bloqueos WAF de headers binarios)
    @Headers(
        "Accept: application/json",
        "User-Agent: Mozilla/5.0 (Android; Mobile; TrackingVentas)"
    )
    @POST("api/saneamiento")
    suspend fun storeSaneamientoMultipart(
        @Body body: MultipartBody
    ): Response<ResponseBody>

    // 2. EDICIÓN: Envío FormUrlEncoded estándar (inmune a reglas WAF de subida de archivos)
    @Headers(
        "Accept: application/json",
        "User-Agent: Mozilla/5.0 (Android; Mobile; TrackingVentas)"
    )
    @FormUrlEncoded
    @POST("api/saneamiento")
    suspend fun storeSaneamientoForm(
        @Field("tipo_registro") tipoRegistro: String,
        @Field("route") route: String,
        @Field("cliente") cliente: String,
        @Field("tipo_negocio") tipoNegocio: String,
        @Field("zona") zona: String,
        @Field("cliente_id") clientId: Long,
        @Field("contacto") contacto: String?,
        @Field("telefono") telefono: String?,
        @Field("celular") celular: String,
        @Field("direccion") direccion: String?,
        @Field("referencia") referencia: String?,
        @Field("nombre_factura") nombreFactura: String,
        @Field("nit") nit: String,
        @Field("created_at") createdAt: String?
    ): Response<ResponseBody>

    // ----------------------------------------------------
    // PEDIDOS RECHAZADOS ENDPOINTS
    // ----------------------------------------------------

    @GET("api/pedidos-rechazados/motivos")
    suspend fun getMotivosRechazo(): Response<com.alvarosega.trackingventas.data.remote.dto.MotivosResponseDto>

    @GET("api/pedidos-rechazados/distribuidores")
    suspend fun getDistribuidores(): Response<com.alvarosega.trackingventas.data.remote.dto.DistribuidoresResponseDto>

    @GET("api/pedidos-rechazados/preventas-pendientes")
    suspend fun getPreventasPendientes(
        @Query("cliente_id") clienteId: String? = null,
        @Query("dias") dias: Int? = null,
        @Query("fecha_preventa") fechaPreventa: String? = null,
        @Query("fecha_desde") fechaDesde: String? = null,
        @Query("fecha_hasta") fechaHasta: String? = null,
        @Query("ruta") ruta: String? = null,
        @Query("incluir_hoy") incluirHoy: Boolean? = null,
        @Query("nro_preventa") nroPreventa: Long? = null,
        @Query("incluir_justificados") incluirJustificados: Boolean? = null
    ): Response<com.alvarosega.trackingventas.data.remote.dto.PreventasPendientesResponseDto>

    @Headers(
        "Accept: application/json",
        "User-Agent: Mozilla/5.0 (Android; Mobile; TrackingVentas)"
    )
    @POST("api/pedidos-rechazados")
    suspend fun storePedidoRechazadoMultipart(
        @Body body: MultipartBody
    ): Response<ResponseBody>

    @GET("api/pedidos-rechazados/historial")
    suspend fun getHistorialRechazos(
        @Query("fecha") fecha: String? = null
    ): Response<com.alvarosega.trackingventas.data.remote.dto.HistorialResponseDto>

    // ----------------------------------------------------
    // INVENTARIO EN PDV ENDPOINTS
    // ----------------------------------------------------

    @GET("api/inventarios-cliente/historial-productos/{cliente_id}")
    suspend fun getHistorialProductosCliente(
        @retrofit2.http.Path("cliente_id") clienteId: String,
        @Query("dias_historial") diasHistorial: Int? = 30
    ): Response<com.alvarosega.trackingventas.data.remote.dto.HistorialProductosResponseDto>

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
        "User-Agent: Mozilla/5.0 (Android; Mobile; TrackingVentas)"
    )
    @POST("api/inventarios-cliente")
    suspend fun storeInventarioClienteJson(
        @Body request: com.alvarosega.trackingventas.data.remote.dto.InventarioRequestDto
    ): Response<com.alvarosega.trackingventas.data.remote.dto.InventarioResponseDto>

    @Headers(
        "Accept: application/json",
        "User-Agent: Mozilla/5.0 (Android; Mobile; TrackingVentas)"
    )
    @POST("api/inventarios-cliente")
    suspend fun storeInventarioClienteMultipart(
        @Body body: MultipartBody
    ): Response<ResponseBody>
}