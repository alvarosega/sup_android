package com.alvarosega.trackingventas.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `saneamiento_base_local` ADD COLUMN `zona` TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `plan_ruteo_local` ADD COLUMN `zona` TEXT DEFAULT NULL")
    }
}

val MIGRATION_2_4 = object : Migration(2, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `saneamiento_base_local` ADD COLUMN `zona` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `plan_ruteo_local` ADD COLUMN `zona` TEXT DEFAULT NULL")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `locations`")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `locations`")
        db.execSQL("ALTER TABLE `visitas` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `visitas` ADD COLUMN `distanceToClient` REAL")
        db.execSQL("ALTER TABLE `visitas` ADD COLUMN `isMockLocation` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `pedidos_rechazados_local` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `uuid` TEXT NOT NULL,
                `clienteId` TEXT NOT NULL,
                `clienteNombre` TEXT,
                `nroPreventa` TEXT,
                `fechaPreventa` TEXT,
                `fechaRechazo` TEXT NOT NULL,
                `motivo` TEXT NOT NULL,
                `tipoRechazo` TEXT NOT NULL,
                `comentarios` TEXT,
                `latitude` REAL,
                `longitude` REAL,
                `accuracy` REAL,
                `isMockLocation` INTEGER NOT NULL DEFAULT 0,
                `localPhotoPath` TEXT,
                `itemsJson` TEXT NOT NULL,
                `isSynced` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
    }
}
