package com.alvarosega.trackingventas.service

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alvarosega.trackingventas.data.local.pref.SessionPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class TrackingKeepAliveWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface KeepAliveEntryPoint {
        fun sessionPreferences(): SessionPreferences
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            KeepAliveEntryPoint::class.java
        )
        val sessionPreferences = entryPoint.sessionPreferences()

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
        return Result.success()
    }
}