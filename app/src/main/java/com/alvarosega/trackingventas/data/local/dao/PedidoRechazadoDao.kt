package com.alvarosega.trackingventas.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alvarosega.trackingventas.data.local.entity.PedidoRechazadoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PedidoRechazadoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPedidoRechazado(pedido: PedidoRechazadoEntity): Long

    @Query("SELECT * FROM pedidos_rechazados_local WHERE isSynced = 0")
    suspend fun getPendingSync(): List<PedidoRechazadoEntity>

    @Query("UPDATE pedidos_rechazados_local SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("SELECT * FROM pedidos_rechazados_local ORDER BY id DESC")
    fun getAllLocalRechazos(): Flow<List<PedidoRechazadoEntity>>
}
