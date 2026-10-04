package com.alvarosega.trackingventas.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tracking_session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_ROLE = "user_role"
    }

    fun saveSession(token: String, userId: Long, username: String, role: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_ROLE, role)
            .commit()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getRole(): String? = prefs.getString(KEY_ROLE, null)
    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)

    fun clearSession() {
        prefs.edit().clear().commit()
    }
}