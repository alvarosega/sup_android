package com.alvarosega.trackingventas.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.alvarosega.trackingventas.data.local.pref.SessionPreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TrackingResurrectionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sessionPreferences: SessionPreferences

    override fun onReceive(context: Context, intent: Intent?) {
        // Solo resucita si la jornada comercial sigue activa en disco
        if (sessionPreferences.isOperationActive.value) {
            val serviceIntent = Intent(context, TrackingService::class.java).apply {
                action = TrackingService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}