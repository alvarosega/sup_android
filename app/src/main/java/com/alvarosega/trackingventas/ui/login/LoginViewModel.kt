package com.alvarosega.trackingventas.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvarosega.trackingventas.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    data class Success(val role: String) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(username: String, pass: String) {
        if (username.isBlank() || pass.isBlank()) {
            _uiState.value = LoginUiState.Error("Debe completar todos los campos")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = authRepository.login(username, pass)
            result.onSuccess { role ->
                _uiState.value = LoginUiState.Success(role)
            }.onFailure { exception ->
                _uiState.value = LoginUiState.Error(exception.message ?: "Error desconocido")
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}