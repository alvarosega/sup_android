package com.alvarosega.trackingventas.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "device_events",
    indices = [Index(value = ["is_synced"])]
)
data class DeviceEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "event_type") val eventType: String,
    val details: String?,
    @ColumnInfo(name = "recorded_at") val recordedAt: String,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false
)