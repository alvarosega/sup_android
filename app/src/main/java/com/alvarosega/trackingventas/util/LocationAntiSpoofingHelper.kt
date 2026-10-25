package com.alvarosega.trackingventas.util

import android.content.Context
import android.location.Location
import android.os.Build
import android.provider.Settings
import android.util.Log

object LocationAntiSpoofingHelper {

    private const val TAG = "ANTI_SPOOFING"

    private val KNOWN_FAKE_GPS_PACKAGES = setOf(
        "com.lexa.fakegps",
        "com.lexa.fakegpsdonate",
        "com.theappninjas.fakegps",
        "com.theappninjas.gpsjoystick",
        "com.incorporateapps.fakegps.fre",
        "com.incorporateapps.fakegps",
        "com.fakegps.mock",
        "com.location.fakegps",
        "com.ondrik.fakegps",
        "org.maptask.fakegps",
        "com.mobi.fake.gps",
        "com.lbe.parallel",
        "com.roaming.fakegps"
    )

    /**
     * Evalúa si una lectura de ubicación proviene de un proveedor simulado o aplicación de Fake GPS.
     * Retorna TRUE si la ubicación es FALSA / SIMULADA.
     */
    fun isMockLocation(location: Location?, context: Context): Boolean {
        if (location == null) return false

        // 1. Verificación oficial de API Android (Android 12+ y anteriores)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (location.isMock) {
                Log.w(TAG, "Ubicación detectada como MOCK vía Android 12+ location.isMock")
                return true
            }
        } else {
            @Suppress("DEPRECATION")
            if (location.isFromMockProvider) {
                Log.w(TAG, "Ubicación detectada como MOCK vía location.isFromMockProvider")
                return true
            }
        }

        // 2. Verificación de coordenadas vacías o corruptas (0,0)
        if (location.latitude == 0.0 && location.longitude == 0.0) {
            Log.w(TAG, "Ubicación corrupta (0,0)")
            return true
        }

        // 3. Verificación de ajustes del sistema (ALLOW_MOCK_LOCATION en versiones antiguas)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                @Suppress("DEPRECATION")
                val mockSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ALLOW_MOCK_LOCATION)
                if (mockSetting != null && mockSetting != "0") {
                    Log.w(TAG, "Ajuste ALLOW_MOCK_LOCATION activo")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error leyendo ajuste de mock location", e)
            }
        }

        // 4. Detección de paquetes conocidos de aplicaciones Fake GPS instaladas
        if (isFakeGpsAppInstalled(context)) {
            Log.w(TAG, "Aplicación de Fake GPS detectada en el sistema")
            return true
        }

        return false
    }

    /**
     * Verifica si el dispositivo tiene instalada alguna aplicación conocida de Fake GPS.
     */
    fun isFakeGpsAppInstalled(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(0)
            installedApps.any { appInfo ->
                KNOWN_FAKE_GPS_PACKAGES.contains(appInfo.packageName)
            }
        } catch (e: Exception) {
            false
        }
    }
}
