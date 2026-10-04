package com.alvarosega.trackingventas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alvarosega.trackingventas.data.local.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity): Long

    @Query("SELECT * FROM locations WHERE isSynced = 0 ORDER BY recordedAt ASC LIMIT :limit")
    suspend fun getUnsyncedLocations(limit: Int): List<LocationEntity>

    @Query("UPDATE locations SET isSynced = 1 WHERE id IN (:locationIds)")
    suspend fun markAsSynced(locationIds: List<Long>)

    @Query("DELETE FROM locations WHERE isSynced = 1 AND recordedAt < :cutoffTimestamp")
    suspend fun purgeOldSyncedLocations(cutoffTimestamp: String)

    // Observa el último punto insertado por el tracking en background
    @Query("SELECT * FROM locations ORDER BY id DESC LIMIT 1")
    fun getLatestLocation(): Flow<LocationEntity?>
}