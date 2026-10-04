package com.alvarosega.trackingventas.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequestDto(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class LoginResponseDto(
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: AuthDataDto
)

data class AuthDataDto(
    @SerializedName("user") val user: UserDto,
    @SerializedName("token") val token: String
)

data class UserDto(
    @SerializedName("id") val id: Long,
    @SerializedName("role_id") val roleId: Long,
    @SerializedName("username") val username: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("role") val role: RoleDto
)

data class RoleDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?
)