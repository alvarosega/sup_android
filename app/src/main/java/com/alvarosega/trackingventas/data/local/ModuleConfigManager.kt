package com.alvarosega.trackingventas.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.alvarosega.trackingventas.data.remote.dto.ModuleItemDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModuleConfigManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MODULE_CONFIG"
        private const val PREFS_NAME = "app_modules_prefs"
        private const val KEY_MODULES_ACTIVE_MAP = "key_modules_active_map"
        private const val KEY_MODULES_CONFIG_MAP = "key_modules_config_map"

        // Lista de módulos principales conocidos por defecto
        val DEFAULT_CORE_MODULES = setOf(
            "ruteo",
            "visitas",
            "pedidos_rechazados",
            "saneamiento"
        )
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gson = Gson()

    private val _activeModules = MutableStateFlow<Map<String, Boolean>>(loadActiveModulesFromPrefs())
    val activeModules: StateFlow<Map<String, Boolean>> = _activeModules.asStateFlow()

    private var moduleConfigsMap: Map<String, Map<String, Any>> = loadModuleConfigsFromPrefs()

    fun updateModules(modules: List<ModuleItemDto>) {
        val newActiveMap = mutableMapOf<String, Boolean>()
        val newConfigMap = mutableMapOf<String, Map<String, Any>>()

        modules.forEach { mod ->
            newActiveMap[mod.code] = mod.isActive
            mod.config?.let { newConfigMap[mod.code] = it }
        }

        _activeModules.value = newActiveMap
        moduleConfigsMap = newConfigMap

        saveToPrefs(newActiveMap, newConfigMap)
        Log.d(TAG, "Módulos actualizados localmente: ${newActiveMap.size} activos")
    }

    fun isModuleActive(code: String): Boolean {
        val currentMap = _activeModules.value
        if (currentMap.containsKey(code)) {
            return currentMap[code] ?: false
        }
        // Fallback offline por defecto para módulos núcleo si aún no se ha sincronizado
        return DEFAULT_CORE_MODULES.contains(code)
    }

    fun getIntConfig(moduleCode: String, key: String, default: Int): Int {
        val config = moduleConfigsMap[moduleCode] ?: return default
        val value = config[key] ?: return default
        return (value as? Number)?.toInt() ?: default
    }

    fun getStringConfig(moduleCode: String, key: String, default: String): String {
        val config = moduleConfigsMap[moduleCode] ?: return default
        val value = config[key] ?: return default
        return value.toString()
    }

    fun getBooleanConfig(moduleCode: String, key: String, default: Boolean): Boolean {
        val config = moduleConfigsMap[moduleCode] ?: return default
        val value = config[key] ?: return default
        return (value as? Boolean) ?: default
    }

    private fun saveToPrefs(activeMap: Map<String, Boolean>, configMap: Map<String, Map<String, Any>>) {
        try {
            val activeJson = gson.toJson(activeMap)
            val configJson = gson.toJson(configMap)
            prefs.edit()
                .putString(KEY_MODULES_ACTIVE_MAP, activeJson)
                .putString(KEY_MODULES_CONFIG_MAP, configJson)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar configuración de módulos en SharedPreferences", e)
        }
    }

    private fun loadActiveModulesFromPrefs(): Map<String, Boolean> {
        val json = prefs.getString(KEY_MODULES_ACTIVE_MAP, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, Boolean>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun loadModuleConfigsFromPrefs(): Map<String, Map<String, Any>> {
        val json = prefs.getString(KEY_MODULES_CONFIG_MAP, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, Map<String, Any>>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
