package com.alvarosega.trackingventas.ui.tracking

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import com.alvarosega.trackingventas.data.location.LocationClient
import com.alvarosega.trackingventas.service.TrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationClient: LocationClient
) : ViewModel() {

    private val _isTrackingActive = MutableStateFlow(false)
    val isTrackingActive: StateFlow<Boolean> = _isTrackingActive.asStateFlow()

    fun isGpsEnabled(): Boolean = locationClient.isGpsEnabled()

    fun setTrackingActive(active: Boolean) {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = if (active) TrackingService.ACTION_START else TrackingService.ACTION_STOP
        }
        if (active) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            context.startService(intent)
        }
        _isTrackingActive.value = active
    }
}