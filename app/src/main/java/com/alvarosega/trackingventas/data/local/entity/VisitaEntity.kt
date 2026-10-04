package com.alvarosega.trackingventas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visitas")
data class VisitaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clientId: Long?,
    val route: String,
    val status: String,
    val isOpportunity: Boolean,
    val opportunityClientName: String?,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val photoPath: String,
    val comments: String?,
    val visitedAt: String,
    val isSynced: Boolean = false
)