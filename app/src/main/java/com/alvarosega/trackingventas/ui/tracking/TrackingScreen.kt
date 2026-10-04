package com.alvarosega.trackingventas.ui.tracking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TrackingScreen(
    viewModel: TrackingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isTracking by viewModel.isTrackingActive.collectAsState()

    fun checkAndToggle() {
        if (!viewModel.isGpsEnabled()) {
            Toast.makeText(context, "Activa la ubicación del teléfono", Toast.LENGTH_SHORT).show()
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }
        viewModel.setTrackingActive(true)
    }

    val bgLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { checkAndToggle() }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBg = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasBg) checkAndToggle() else bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            checkAndToggle()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                checkAndToggle()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Switch(
            checked = isTracking,
            onCheckedChange = { enable ->
                if (!enable) {
                    viewModel.setTrackingActive(false)
                } else {
                    val hasFine = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasFine) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
                    ) {
                        bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    } else {
                        checkAndToggle()
                    }
                }
            }
        )
    }
}