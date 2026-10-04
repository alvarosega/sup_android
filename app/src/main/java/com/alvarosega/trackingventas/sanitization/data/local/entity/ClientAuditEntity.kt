package com.alvarosega.trackingventas.sanitization.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "client_audits")
data class ClientAuditEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clientId: Long,
    val auditStatus: String,         // VALIDATED, NOT_FOUND, CLOSED_PERMANENT, DUPLICATE
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val comments: String,
    val photoPathsJson: String,      // Rutas locales de las fotos serializadas en JSON (máx 3)
    val auditedAt: String,
    val isSynced: Boolean = false
)