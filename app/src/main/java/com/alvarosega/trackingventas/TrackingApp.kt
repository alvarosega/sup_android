package com.alvarosega.trackingventas

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.osmdroid.config.Configuration as OsmConfig

@HiltAndroidApp
class TrackingApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Inicialización nativa de OSMDroid
        OsmConfig.getInstance().load(this, getSharedPreferences("${packageName}_osm", MODE_PRIVATE))
        OsmConfig.getInstance().userAgentValue = packageName
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory) // <-- ESTO RESUELVE LA INSTANCIACIÓN DE HILT
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}