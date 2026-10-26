package com.alvarosega.trackingventas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "plan_ruteo_local")
data class PlanRuteoEntity(
    @PrimaryKey
    val clientId: Long,
    val clientName: String,
    val route: String,
    val day: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val isVisited: Boolean = false,
    val tipoNegocio: String? = null,
    val zona: String? = null,
    val contacto: String? = null,
    val telefono: String? = null,
    val celular: String? = null,
    val referencia: String? = null,
    val nombreFactura: String? = null,
    val nit: String? = null,
    val hasPendingReview: Boolean = false,
    val requiereSaneamiento: Boolean = false,
    val saneamientoObligatorio: Boolean = false,
    val saneamientoAlertasJson: String? = null,
    val saneamientoCamposJson: String? = null,
    val calidadScore: Int? = null
)

fun PlanRuteoEntity.getSaneamientoAlertas(): List<String> {
    if (saneamientoAlertasJson.isNullOrBlank()) return emptyList()
    return try {
        val type = object : TypeToken<List<String>>() {}.type
        Gson().fromJson(saneamientoAlertasJson, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * Evalúa si el registro requiere saneamiento/corrección de datos.
 */
fun PlanRuteoEntity.requiereCorreccion(): Boolean {
    // 1. Cliente: No vacío
    val clienteValido = clientName.isNotBlank() && clientName != "Cliente Sin Nombre"

    // 2. Tipo de negocio: No vacío
    val tipoNegocioValido = !tipoNegocio.isNullOrBlank()

    // 3. Zona: No vacía
    val zonaValida = !zona.isNullOrBlank()

    // 4. Celular: No vacío (mínimo 7 dígitos)
    val celDigits = celular?.filter { it.isDigit() } ?: ""
    val celularValido = celDigits.length >= 7

    // 5. Dirección: No vacía (símbolos #, /, ,, -, . son 100% VÁLIDOS)
    val direccionValida = !address.isNullOrBlank()

    // 6. Nombre de Factura: No vacío (o fallback a cliente)
    val nombreFacturaValido = !nombreFactura.isNullOrBlank() || clientName.isNotBlank()

    // 7. NIT: No vacío
    val nitValido = !nit.isNullOrBlank()

    val datosCompletos = clienteValido &&
            tipoNegocioValido &&
            zonaValida &&
            celularValido &&
            direccionValida &&
            nombreFacturaValido &&
            nitValido

    // Si los datos están físicamente completos, NO requiere corrección (Pin Azul o Verde)
    if (datosCompletos) return false

    // Si falta alguno de los datos obligatorios, requiere saneamiento (Pin Naranja)
    return true
}

// Alias de compatibilidad para pantallas existentes como VisitaRegistrationScreen
fun PlanRuteoEntity.tieneDatosIncompletos(): Boolean = requiereCorreccion()
