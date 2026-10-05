package com.alvarosega.trackingventas.data.remote

import com.alvarosega.trackingventas.data.remote.dto.LoginRequestDto
import com.alvarosega.trackingventas.data.remote.dto.LoginResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// DTOs para la respuesta de jornada laboral
data class WorkdayResponseDto(
    val status: String,
    val message: String? = null,
    val is_active: Boolean? = null,
    val action: String? = null,
    val limit_time: String? = null,
    val workday: WorkdayPayloadDto? = null
)

data class WorkdayPayloadDto(
    val id: Long? = null,
    val user_id: Long? = null,
    val work_date: String? = null,
    val started_at: String? = null,
    val ended_at: String? = null,
    val status: String? = null
)

data class WorkdayStatusResponseDto(
    val is_active: Boolean,
    val status: String,
    val action: String? = null,
    val limit_time: String? = null,
    val within_schedule: Boolean? = null,
    val work_date: String? = null,
    val started_at: String? = null,
    val ended_at: String? = null,
    val close_reason: String? = null
)

interface AuthApiService {

    @POST("api/login")
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

    @GET("api/workday/status")
    suspend fun getWorkdayStatus(): Response<WorkdayStatusResponseDto>

    // Control de Jornada Laboral
    @POST("api/workday/start")
    suspend fun startWorkday(): Response<WorkdayResponseDto>

    @POST("api/workday/stop")
    suspend fun stopWorkday(): Response<WorkdayResponseDto>
}