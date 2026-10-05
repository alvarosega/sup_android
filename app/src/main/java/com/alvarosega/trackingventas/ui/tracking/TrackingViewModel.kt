package com.alvarosega.trackingventas.ui.tracking

import androidx.lifecycle.ViewModel
import com.alvarosega.trackingventas.data.location.LocationClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val locationClient: LocationClient
) : ViewModel() {

    private val _isTrackingActive = MutableStateFlow(false)
    val isTrackingActive: StateFlow<Boolean> = _isTrackingActive.asStateFlow()

    fun isGpsEnabled(): Boolean = locationClient.isGpsEnabled()

    fun setTrackingActive(active: Boolean) {
        _isTrackingActive.value = active
    }
}