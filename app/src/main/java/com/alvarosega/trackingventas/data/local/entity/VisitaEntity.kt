package com.alvarosega.trackingventas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "visitas")
data class VisitaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val clientId: Long?,
    val route: String,
    val status: String,
    val isOpportunity: Boolean = false,
    val opportunityClientName: String? = null,
    val distanceToClient: Double? = null,
    val isMockLocation: Boolean = false,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val photoPath: String,
    val comments: String?,
    val visitedAt: String,
    val isSynced: Boolean = false
) {
    fun getPhotoPathList(): List<String> {
        return if (photoPath.isBlank()) emptyList()
        else photoPath.split("|").filter { it.isNotBlank() }
    }
}
