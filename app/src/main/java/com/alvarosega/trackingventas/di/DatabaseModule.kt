package com.alvarosega.trackingventas.di

import android.content.Context
import androidx.room.Room
import com.alvarosega.trackingventas.data.local.AppDatabase
import com.alvarosega.trackingventas.data.local.dao.DeviceEventDao
import com.alvarosega.trackingventas.data.local.dao.PlanRuteoDao
import com.alvarosega.trackingventas.data.local.dao.VisitaDao
import com.alvarosega.trackingventas.sanitization.data.local.dao.ClientAuditDao
import com.alvarosega.trackingventas.sanitization.data.local.dao.ReferenceClientDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.alvarosega.trackingventas.data.local.dao.SaneamientoDao
import com.alvarosega.trackingventas.data.local.MIGRATION_2_3
import com.alvarosega.trackingventas.data.local.MIGRATION_3_4
import com.alvarosega.trackingventas.data.local.MIGRATION_2_4
import com.alvarosega.trackingventas.data.local.MIGRATION_4_5
import com.alvarosega.trackingventas.data.local.MIGRATION_5_6

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tracking_ventas_db"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_2_4, MIGRATION_4_5, MIGRATION_5_6)
            .enableMultiInstanceInvalidation()
            .build()
    }

    @Provides
    fun provideSaneamientoDao(database: AppDatabase): SaneamientoDao {
        return database.saneamientoDao()
    }
    @Provides
    fun provideDeviceEventDao(database: AppDatabase): DeviceEventDao {
        return database.deviceEventDao()
    }

    @Provides
    fun providePlanRuteoDao(database: AppDatabase): PlanRuteoDao {
        return database.planRuteoDao()
    }

    @Provides
    fun provideVisitaDao(database: AppDatabase): VisitaDao {
        return database.visitaDao()
    }

    @Provides
    fun provideReferenceClientDao(database: AppDatabase): ReferenceClientDao {
        return database.referenceClientDao()
    }

    @Provides
    fun provideClientAuditDao(database: AppDatabase): ClientAuditDao {
        return database.clientAuditDao()
    }
}