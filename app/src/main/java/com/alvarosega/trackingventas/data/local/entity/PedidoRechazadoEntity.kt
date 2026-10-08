package com.alvarosega.trackingventas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pedidos_rechazados_local")
data class PedidoRechazadoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val uuid: String,
    val clienteId: String,
    val clienteNombre: String?,
    val nroPreventa: String?,
    val fechaPreventa: String?,
    val fechaRechazo: String,
    val motivo: String,
    val tipoRechazo: String, // "TOTAL" o "PARCIAL"
    val comentarios: String?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Float?,
    val isMockLocation: Boolean = false,
    val localPhotoPath: String?,
    val itemsJson: String, // Array JSON de productos rechazados stringificado
    val isSynced: Boolean = false
)
