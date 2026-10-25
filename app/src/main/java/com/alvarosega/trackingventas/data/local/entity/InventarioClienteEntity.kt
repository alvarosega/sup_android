package com.alvarosega.trackingventas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventario_cliente_local")
data class InventarioClienteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val uuid: String,
    val clienteId: String,
    val codigoCliente: String? = null,
    val clienteNombre: String?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Float?,
    val isMockLocation: Boolean = false,
    val comments: String?,
    val itemsJson: String, // Array JSON de productos y stock físico stringificado
    val createdAt: String,
    val isSynced: Boolean = false
)
