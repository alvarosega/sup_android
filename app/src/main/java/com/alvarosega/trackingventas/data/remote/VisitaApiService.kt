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
}