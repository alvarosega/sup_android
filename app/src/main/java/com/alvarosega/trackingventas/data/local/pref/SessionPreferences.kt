package com.alvarosega.trackingventas.data.local.pref

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("tracking_session_prefs", Context.MODE_PRIVATE)

    private val _isOperationActive = MutableStateFlow(isOperationActiveDirect())
    val isOperationActive: StateFlow<Boolean> = _isOperationActive.asStateFlow()

    private val _activeRoute = MutableStateFlow(getActiveRouteDirect())
    val activeRoute: StateFlow<String> = _activeRoute.asStateFlow()

    fun isOperationActiveDirect(): Boolean {
        return prefs.getBoolean(KEY_OPERATION_ACTIVE, false)
    }

    fun getActiveRouteDirect(): String {
        return prefs.getString(KEY_ASSIGNED_ROUTE, "") ?: ""
    }

    fun saveUserRoute(route: String) {
        prefs.edit()
            .putString(KEY_ASSIGNED_ROUTE, route)
            .commit()
        _activeRoute.value = route
    }

    fun setOperationState(active: Boolean) {
        prefs.edit()
            .putBoolean(KEY_OPERATION_ACTIVE, active)
            .commit()
        _isOperationActive.value = active
    }

    fun clearSession() {
        prefs.edit()
            .putBoolean(KEY_OPERATION_ACTIVE, false)
            .putString(KEY_ASSIGNED_ROUTE, "")
            .remove(KEY_LIMIT_TIME)
            .commit()
        _isOperationActive.value = false
        _activeRoute.value = ""
    }

    private val _workdayLimitTime = MutableStateFlow(getWorkdayLimitTimeDirect())
    val workdayLimitTime: StateFlow<String?> = _workdayLimitTime.asStateFlow()

    fun setWorkdayLimitTime(timeStr: String?) {
        prefs.edit().putString(KEY_LIMIT_TIME, timeStr).apply()
        _workdayLimitTime.value = timeStr
    }

    fun getWorkdayLimitTimeDirect(): String? {
        return prefs.getString(KEY_LIMIT_TIME, null)
    }

    companion object {
        private const val KEY_OPERATION_ACTIVE = "key_operation_active"
        private const val KEY_ASSIGNED_ROUTE = "key_assigned_route"
        private const val KEY_LIMIT_TIME = "key_workday_limit_time"
    }
}