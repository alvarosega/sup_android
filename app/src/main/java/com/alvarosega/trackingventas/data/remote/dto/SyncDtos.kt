package com.alvarosega.trackingventas.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SyncLocationsRequest(
    @SerializedName("locations") val locations: List<LocationPayload>,
    @SerializedName("events") val events: List<DeviceEventPayload>
)

data class LocationPayload(
    @SerializedName("local_id") val localId: Long,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Float,
    @SerializedName("speed") val speed: Float,
    @SerializedName("battery_level") val batteryLevel: Int,
    @SerializedName("is_mock") val isMock: Boolean,
    @SerializedName("is_moving") val isMoving: Boolean,
    @SerializedName("step_count") val stepCount: Int,
    @SerializedName("motion_variance") val motionVariance: Float,
    @SerializedName("recorded_at") val recordedAt: String
)

data class DeviceEventPayload(
    @SerializedName("local_id") val localId: Long,
    @SerializedName("event_type") val eventType: String,
    @SerializedName("details") val details: String?,
    @SerializedName("recorded_at") val recordedAt: String
)

data class SyncResponse(
    @SerializedName("message") val message: String,
    @SerializedName("synced_locations") val syncedLocations: List<Long>,
    @SerializedName("synced_events") val syncedEvents: List<Long>
)