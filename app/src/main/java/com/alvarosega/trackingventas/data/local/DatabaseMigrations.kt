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
