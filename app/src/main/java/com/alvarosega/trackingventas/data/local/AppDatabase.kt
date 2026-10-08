package com.alvarosega.trackingventas.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.alvarosega.trackingventas.data.local.dao.DeviceEventDao
import com.alvarosega.trackingventas.data.local.dao.PlanRuteoDao
import com.alvarosega.trackingventas.data.local.dao.VisitaDao
import com.alvarosega.trackingventas.data.local.entity.DeviceEventEntity
import com.alvarosega.trackingventas.data.local.entity.PlanRuteoEntity
import com.alvarosega.trackingventas.data.local.entity.VisitaEntity
import com.alvarosega.trackingventas.sanitization.data.local.dao.ClientAuditDao
import com.alvarosega.trackingventas.sanitization.data.local.dao.ReferenceClientDao
import com.alvarosega.trackingventas.sanitization.data.local.entity.ClientAuditEntity
import com.alvarosega.trackingventas.sanitization.data.local.entity.ReferenceClientEntity
import com.alvarosega.trackingventas.data.local.dao.SaneamientoDao
import com.alvarosega.trackingventas.data.local.entity.SaneamientoBaseEntity
import com.alvarosega.trackingventas.data.local.dao.PedidoRechazadoDao
import com.alvarosega.trackingventas.data.local.entity.PedidoRechazadoEntity


@Database(
    entities = [
        DeviceEventEntity::class,
        PlanRuteoEntity::class,
        VisitaEntity::class,
        ReferenceClientEntity::class,
        ClientAuditEntity::class,
        SaneamientoBaseEntity::class,
        PedidoRechazadoEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceEventDao(): DeviceEventDao
    abstract fun planRuteoDao(): PlanRuteoDao
    abstract fun visitaDao(): VisitaDao
    abstract fun referenceClientDao(): ReferenceClientDao
    abstract fun clientAuditDao(): ClientAuditDao
    abstract fun saneamientoDao(): SaneamientoDao
    abstract fun pedidoRechazadoDao(): PedidoRechazadoDao
}