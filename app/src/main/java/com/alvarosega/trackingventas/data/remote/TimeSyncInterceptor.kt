package com.alvarosega.trackingventas.data.remote

import com.alvarosega.trackingventas.data.time.ServerTimeManager
import okhttp3.Interceptor
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeSyncInterceptor @Inject constructor(
    private val serverTimeManager: ServerTimeManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        // Extrae la cabecera estándar HTTP "Date" que todo servidor web (Nginx/Apache) envía
        val dateHeader = response.header("Date")
        if (!dateHeader.isNullOrBlank()) {
            try {
                // Formato estándar RFC 1123 / HTTP: "Sun, 13 Sep 2026 15:35:00 GMT"
                val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("GMT")
                }
                val serverDate = format.parse(dateHeader)
                if (serverDate != null) {
                    serverTimeManager.syncWithServerTime(serverDate.time)
                }
            } catch (_: Exception) {
                // Si el formato difiere, continúa sin interrumpir el flujo HTTP
            }
        }

        return response
    }
}