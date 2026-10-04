package com.alvarosega.trackingventas.data.repository

import com.alvarosega.trackingventas.data.local.SessionManager
import com.alvarosega.trackingventas.data.local.pref.SessionPreferences
import com.alvarosega.trackingventas.data.remote.AuthApiService
import com.alvarosega.trackingventas.data.remote.dto.LoginRequestDto
import org.json.JSONObject
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val apiService: AuthApiService,
    private val sessionManager: SessionManager,
    private val sessionPreferences: SessionPreferences
) {
    suspend fun login(username: String, password: String): Result<String> {
        return try {
            val response = apiService.login(LoginRequestDto(username.trim(), password))
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!.data
                sessionManager.saveSession(
                    token = data.token,
                    userId = data.user.id,
                    username = data.user.username,
                    role = data.user.role.name
                )
                // Guarda la ruta activa de forma persistente para la interfaz y el tracking
                sessionPreferences.saveUserRoute(data.user.username)

                Result.success(data.user.role.name)
            } else {
                val errorMsg = response.errorBody()?.string()?.let {
                    try {
                        JSONObject(it).optString("message", "Error de credenciales")
                    } catch (e: Exception) {
                        "Credenciales incorrectas o error en el servidor"
                    }
                } ?: "Error al autenticar"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Fallo de conexión a la red"))
        }
    }

    fun getUserRole(): String? = sessionManager.getRole()
    fun isUserLoggedIn(): Boolean = !sessionManager.getToken().isNullOrBlank()
}