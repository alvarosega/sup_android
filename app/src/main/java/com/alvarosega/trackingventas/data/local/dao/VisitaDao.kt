package com.alvarosega.trackingventas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alvarosega.trackingventas.data.local.entity.VisitaEntity

@Dao
interface VisitaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisita(visita: VisitaEntity): Long

    @Query("SELECT * FROM visitas WHERE isSynced = 0")
    suspend fun getPendingSyncVisitas(): List<VisitaEntity>

    @Query("UPDATE visitas SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)
}