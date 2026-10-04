package com.alvarosega.trackingventas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float,
    val batteryLevel: Int,
    val isMock: Boolean,
    val isMoving: Boolean = false,
    val stepCount: Int = 0,
    val motionVariance: Float = 0f,
    val recordedAt: String,
    val isSynced: Boolean = false
)