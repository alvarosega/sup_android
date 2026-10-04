package com.alvarosega.trackingventas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alvarosega.trackingventas.data.local.entity.SaneamientoBaseEntity

@Dao
interface SaneamientoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaneamiento(item: SaneamientoBaseEntity): Long

    @Query("SELECT * FROM saneamiento_base_local WHERE isSynced = 0 ORDER BY id ASC")
    suspend fun getPendingSync(): List<SaneamientoBaseEntity>

    @Query("UPDATE saneamiento_base_local SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("SELECT * FROM saneamiento_base_local WHERE clienteId = :clientId ORDER BY id DESC LIMIT 1")
    suspend fun getLatestByClient(clientId: Long): SaneamientoBaseEntity?
}