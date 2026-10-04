package com.alvarosega.trackingventas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alvarosega.trackingventas.data.local.entity.DeviceEventEntity

@Dao
interface DeviceEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: DeviceEventEntity): Long

    @Query("SELECT * FROM device_events WHERE is_synced = 0 ORDER BY id ASC LIMIT :limit")
    suspend fun getUnsyncedEvents(limit: Int = 50): List<DeviceEventEntity>

    @Query("UPDATE device_events SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("DELETE FROM device_events WHERE is_synced = 1 AND recorded_at < :cutoffDate")
    suspend fun purgeOldSyncedEvents(cutoffDate: String): Int
}