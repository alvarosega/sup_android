package com.alvarosega.trackingventas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alvarosega.trackingventas.data.local.entity.InventarioClienteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventarioClienteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventario(inventario: InventarioClienteEntity): Long

    @Query("SELECT * FROM inventario_cliente_local WHERE isSynced = 0")
    suspend fun getPendingSync(): List<InventarioClienteEntity>

    @Query("UPDATE inventario_cliente_local SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("SELECT * FROM inventario_cliente_local ORDER BY id DESC")
    fun getAllLocalInventarios(): Flow<List<InventarioClienteEntity>>
}
