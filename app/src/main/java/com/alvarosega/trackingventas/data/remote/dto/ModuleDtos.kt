package com.alvarosega.trackingventas.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ModulesResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("ruta") val ruta: String?,
    @SerializedName("data") val data: List<ModuleItemDto> = emptyList()
)

data class ModuleItemDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("order") val order: Int? = null,
    @SerializedName("config") val config: Map<String, Any>? = null
)
