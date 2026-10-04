package com.alvarosega.trackingventas.data.time

import android.os.SystemClock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerTimeManager @Inject constructor() {

    @Volatile
    private var timeDifferenceMs: Long? = null

    /**
     * Sincroniza el tiempo base del servidor con el contador monótono de hardware.
     */
    fun syncWithServerTime(serverEpochMs: Long) {
        timeDifferenceMs = serverEpochMs - SystemClock.elapsedRealtime()
    }

    val isSynchronized: Boolean
        get() = timeDifferenceMs != null

    /**
     * Retorna la hora real inmutable.
     * Si no se ha sincronizado aún con el backend, recurre a System.currentTimeMillis()
     * como último recurso temporal hasta la primera respuesta HTTP.
     */
    fun getSecureCurrentTimeMillis(): Long {
        val diff = timeDifferenceMs
        return if (diff != null) {
            diff + SystemClock.elapsedRealtime()
        } else {
            System.currentTimeMillis()
        }
    }

    /**
     * Retorna el string formateado estrictamente para Bolivia (GMT-4).
     */
    fun getBoliviaTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("America/La_Paz")
        }
        return sdf.format(Date(getSecureCurrentTimeMillis()))
    }
}