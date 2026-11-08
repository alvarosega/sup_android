package com.alvarosega.trackingventas.ui.pizarra

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvarosega.trackingventas.data.local.pref.SessionPreferences
import com.alvarosega.trackingventas.data.remote.VisitaApiService
import com.alvarosega.trackingventas.data.remote.dto.PizarraResponseDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PizarraViewModel @Inject constructor(
    private val apiService: VisitaApiService,
    private val sessionPreferences: SessionPreferences
) : ViewModel() {

    companion object {
        private const val TAG = "PIZARRA_DEBUG"
    }

    private val _pizarraState = MutableStateFlow<PizarraResponseDto?>(null)
    val pizarraState: StateFlow<PizarraResponseDto?> = _pizarraState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        fetchPizarraResumen()
    }

    fun fetchPizarraResumen(fecha: String? = null, ruta: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val response = apiService.getPizarraResumen(fecha = fecha, ruta = ruta)
                Log.d(TAG, "getPizarraResumen HTTP Code: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _pizarraState.value = body
                    _errorMessage.value = null
                    Log.d(TAG, "Pizarra cargada exitosamente: Score ${body.miRuta?.scoreTotal} pts")
                } else {
                    val err = response.errorBody()?.string()
                    Log.e(TAG, "Error HTTP ${response.code()} consultando Pizarra: $err")
                    _errorMessage.value = "Error al consultar la Pizarra (${response.code()})"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción consultando Pizarra", e)
                _errorMessage.value = "Error de red al consultar la Pizarra"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
