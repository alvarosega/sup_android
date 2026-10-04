package com.alvarosega.trackingventas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saneamiento_base_local")
data class SaneamientoBaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val clienteId: Long?,
    val tipoRegistro: String, // "ALTA" o "EDICION"
    val route: String,
    val cliente: String,
    val tipoNegocio: String,
    val zona: String, // <-- NUEVO CAMPO OBLIGATORIO
    val contacto: String?,
    val telefono: String?,
    val celular: String?,
    val direccion: String?,
    val referencia: String?,
    val nombreFactura: String?,
    val nit: String?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Float?,
    val localPhotoPath: String?,
    val createdAt: String,
    val isSynced: Boolean = false
)