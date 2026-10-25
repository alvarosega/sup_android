package com.alvarosega.trackingventas.data.repository

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import com.alvarosega.trackingventas.data.local.ModuleConfigManager
import com.alvarosega.trackingventas.data.local.SessionManager
import com.alvarosega.trackingventas.data.local.pref.SessionPreferences
import com.alvarosega.trackingventas.data.remote.AuthApiService
import com.alvarosega.trackingventas.data.remote.dto.LoginRequestDto
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val apiService: AuthApiService,
    private val sessionManager: SessionManager,
    private val sessionPreferences: SessionPreferences,
    private val moduleConfigManager: ModuleConfigManager,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AUTH_REPO"
    }

    suspend fun login(usernameInput: String, passwordInput: String): Result<String> {
        val cleanUsername = usernameInput.trim()
        val cleanPassword = passwordInput.trim()

        return try {
            val response = apiService.login(LoginRequestDto(cleanUsername, cleanPassword))

            if (response.isSuccessful && response.body() != null) {
                val authData = response.body()!!.data
                val user = authData.user
                val userRuta = user.ruta?.ifBlank { user.username } ?: user.username
                val userRole = user.role?.name ?: "vendedor"

                sessionManager.saveSession(
                    token = authData.token,
                    userId = user.id,
                    username = user.username,
                    ruta = userRuta,
                    role = userRole
                )

                // Guarda la ruta activa para la UI y servicios
                sessionPreferences.saveUserRoute(userRuta)

                // Sincronizar permisos y módulos asignados a esta ruta
                syncAppModules()

                Log.d(TAG, "Login exitoso. User: ${user.username}, Ruta: $userRuta, Role: $userRole")
                Result.success(userRole)
            } else {
                val errorRaw = response.errorBody()?.string() ?: ""
                val cleanError = parseApiError(errorRaw, response.code())
                Log.e(TAG, "Login fallido HTTP ${response.code()}: $cleanError")
                Result.failure(Exception(cleanError))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en proceso de login", e)
            Result.failure(Exception(e.localizedMessage ?: "Fallo de conexión a la red"))
        }
    }

    suspend fun syncAppModules(): Boolean {
        return try {
            val response = apiService.getAppModules()
            if (response.isSuccessful && response.body()?.success == true) {
                val modules = response.body()!!.data
                moduleConfigManager.updateModules(modules)
                Log.d(TAG, "Módulos de la aplicación sincronizados correctamente: ${modules.size}")
                true
            } else {
                Log.e(TAG, "Error HTTP ${response.code()} al sincronizar módulos de app")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallo de red al consultar módulos de la aplicación", e)
            false
        }
    }

    suspend fun logout() {
        try {
            // Intenta notificar el logout al servidor Laravel
            apiService.logout()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo notificar el logout al servidor (posible falta de red)", e)
        } finally {
            // Cancela tareas en segundo plano de WorkManager
            try {
                val wm = WorkManager.getInstance(context)
                wm.cancelAllWork()
            } catch (e: Exception) {
                Log.w(TAG, "Error cancelando workers en logout", e)
            }

            // Limpia sesión y preferencias locales
            sessionManager.clearSession()
            sessionPreferences.clearSession()
        }
    }

    private fun parseApiError(rawJson: String, statusCode: Int): String {
        if (rawJson.isBlank()) {
            return when (statusCode) {
                401 -> "Credenciales incorrectas."
                422 -> "Datos de ingreso inválidos."
                else -> "Error en el servidor ($statusCode)."
            }
        }

        return try {
            val json = JSONObject(rawJson)

            // 1. Extraer del objeto 'errors' (ej: errors.username[0])
            if (json.has("errors")) {
                val errorsObj = json.optJSONObject("errors")
                if (errorsObj != null) {
                    val keys = errorsObj.keys()
                    if (keys.hasNext()) {
                        val firstKey = keys.next()
                        val arr = errorsObj.optJSONArray(firstKey)
                        if (arr != null && arr.length() > 0) {
                            return arr.getString(0)
                        }
                    }
                }
            }

            // 2. Extraer del campo 'message'
            if (json.has("message")) {
                val msg = json.getString("message")
                if (msg.isNotBlank()) return msg
            }

            "Credenciales incorrectas o usuario inactivo."
        } catch (e: Exception) {
            "Error de autenticación ($statusCode)."
        }
    }

    fun getUserRole(): String = sessionManager.getRole()
    fun isUserLoggedIn(): Boolean = !sessionManager.getToken().isNullOrBlank()
}
