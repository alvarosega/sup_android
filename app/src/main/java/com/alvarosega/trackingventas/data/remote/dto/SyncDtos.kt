package com.alvarosega.trackingventas.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DeviceEventPayload(
    @SerializedName("local_id") val localId: Long,
    @SerializedName("event_type") val eventType: String,
    @SerializedName("details") val details: String?,
    @SerializedName("recorded_at") val recordedAt: String
)
