package com.alvarosega.trackingventas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.alvarosega.trackingventas.data.local.entity.PlanRuteoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanRuteoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(planes: List<PlanRuteoEntity>)

    @Query("SELECT * FROM plan_ruteo_local")
    fun getPendingClientsByDay(): Flow<List<PlanRuteoEntity>>

    @Query("SELECT * FROM plan_ruteo_local WHERE clientId = :clientId LIMIT 1")
    suspend fun getClientById(clientId: Long): PlanRuteoEntity?

    @Query("SELECT clientId FROM plan_ruteo_local WHERE isVisited = 1")
    suspend fun getVisitedClientIds(): List<Long>

    @Query("UPDATE plan_ruteo_local SET isVisited = 1 WHERE clientId = :clientId")
    suspend fun markClientVisited(clientId: Long)

    @Query("DELETE FROM plan_ruteo_local")
    suspend fun clearPlanRuteo()

    @Transaction
    suspend fun refreshPreservingVisited(remotePlanes: List<PlanRuteoEntity>) {
        val visitedIds = getVisitedClientIds().toSet()
        val merged = remotePlanes.map { plan ->
            if (visitedIds.contains(plan.clientId)) {
                plan.copy(isVisited = true)
            } else {
                plan
            }
        }
        clearPlanRuteo()
        insertIfNotExists(merged)
    }
}