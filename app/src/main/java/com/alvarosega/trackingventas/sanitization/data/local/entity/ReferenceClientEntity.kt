package com.alvarosega.trackingventas.sanitization.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reference_clients")
data class ReferenceClientEntity(
    @PrimaryKey
    val clientId: Long,              // id_cliente
    val route: String,               // ruta
    val day: String,                 // día
    val address: String,             // direccion
    val referenceLatitude: Double?,   // latitud referencial
    val referenceLongitude: Double?,  // longitud referencial
    val isAudited: Boolean = false   // Bandera local para saber si ya fue procesado
)