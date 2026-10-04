package com.alvarosega.trackingventas.sanitization.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alvarosega.trackingventas.sanitization.data.local.entity.ClientAuditEntity

@Dao
interface ClientAuditDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: ClientAuditEntity): Long

    @Query("SELECT * FROM client_audits WHERE isSynced = 0 LIMIT :limit")
    suspend fun getUnsyncedAudits(limit: Int = 10): List<ClientAuditEntity>

    @Query("UPDATE client_audits SET isSynced = 1 WHERE id IN (:auditIds)")
    suspend fun markAsSynced(auditIds: List<Long>)
}