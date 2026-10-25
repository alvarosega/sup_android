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
    if (requiereSaneamiento || saneamientoObligatorio) return true

    // 1. Cliente: No vacío, sin símbolos extraños (#, $, +, %, etc.)
    val clienteLimpio = clientName.isNotBlank() &&
            clientName != "Cliente Sin Nombre" &&
            !Regex("[#$+%_=<>{}\\[\\]\\^]").containsMatchIn(clientName)

    // 2. Tipo de negocio: Obligatorio
    val tipoNegocioValido = !tipoNegocio.isNullOrBlank()

    // 3. Zona: Obligatoria y distinta de placeholders
    val zonaValida = !zona.isNullOrBlank() &&
            !zona.equals("SIN ZONA", ignoreCase = true)

    // 4. Celular: Obligatorio, estrictamente numérico, 8 dígitos (o mínimo 7)
    val celLimpio = celular?.trim() ?: ""
    val celularValido = celLimpio.isNotBlank() &&
            celLimpio.all { it.isDigit() } &&
            celLimpio.length in 7..8

    // 5. Teléfono: Opcional, pero si existe debe ser numérico
    val telLimpio = telefono?.trim() ?: ""
    val telefonoValido = telLimpio.isBlank() || (telLimpio.all { it.isDigit() } && telLimpio.length >= 7)

    // 6. Nombre de Factura: Obligatorio
    val nombreFacturaValido = !nombreFactura.isNullOrBlank()

    // 7. NIT: Obligatorio, puramente numérico (admite "0")
    val nitLimpio = nit?.trim() ?: ""
    val nitValido = nitLimpio.isNotBlank() && nitLimpio.all { it.isDigit() }

    val datosCompletos = clienteLimpio &&
            tipoNegocioValido &&
            zonaValida &&
            celularValido &&
            telefonoValido &&
            nombreFacturaValido &&
            nitValido

    return !datosCompletos
}

// Alias de compatibilidad para pantallas existentes como VisitaRegistrationScreen
fun PlanRuteoEntity.tieneDatosIncompletos(): Boolean = requiereCorreccion()
