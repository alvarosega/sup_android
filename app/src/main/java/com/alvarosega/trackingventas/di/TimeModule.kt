package com.alvarosega.trackingventas.di

import android.content.Context
import com.lyft.kronos.AndroidClockFactory
import com.lyft.kronos.KronosClock
import com.lyft.kronos.SyncListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    @Singleton
    fun provideKronosClock(
        @ApplicationContext context: Context
    ): KronosClock {
        val clock = AndroidClockFactory.createKronosClock(
            context = context,
            syncListener = object : SyncListener {
                override fun onStartSync(host: String) {}
                override fun onSuccess(ticksDelta: Long, responseTimeMs: Long) {}
                override fun onError(host: String, throwable: Throwable) {}
            },
            ntpHosts = listOf("time.google.com", "pool.ntp.org", "time.cloudflare.com")
        )
        // En Kronos el método de arranque en segundo plano es syncInBackground()
        clock.syncInBackground()
        return clock
    }
}