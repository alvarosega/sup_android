package com.alvarosega.trackingventas.sanitization.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alvarosega.trackingventas.sanitization.data.local.entity.ReferenceClientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReferenceClientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<ReferenceClientEntity>)

    @Query("SELECT * FROM reference_clients WHERE day = :day ORDER BY isAudited ASC, clientId ASC")
    fun getClientsByDay(day: String): Flow<List<ReferenceClientEntity>>

    @Query("SELECT * FROM reference_clients WHERE clientId = :clientId LIMIT 1")
    suspend fun getClientById(clientId: Long): ReferenceClientEntity?

    @Query("UPDATE reference_clients SET isAudited = :isAudited WHERE clientId = :clientId")
    suspend fun updateAuditedStatus(clientId: Long, isAudited: Boolean)

    @Query("SELECT COUNT(*) FROM reference_clients")
    suspend fun countClients(): Int
}