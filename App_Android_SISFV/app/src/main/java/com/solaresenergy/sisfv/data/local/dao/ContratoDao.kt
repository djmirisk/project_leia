package com.solaresenergy.sisfv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.solaresenergy.sisfv.data.local.entity.ContratoEntity

@Dao
interface ContratoDao {

    @Query("""
        SELECT * FROM contratos
        WHERE id IN (:contratoIds)
        AND activo = 1
        ORDER BY nombreContrato ASC
    """)
    suspend fun obtenerContratosAsignados(
        contratoIds: List<Int>
    ): List<ContratoEntity>

    @Query("""
        SELECT * FROM contratos
        WHERE id = :contratoId
        AND activo = 1
        LIMIT 1
    """)
    suspend fun obtenerPorId(
        contratoId: Int
    ): ContratoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(
        contrato: ContratoEntity
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizarLista(
        contratos: List<ContratoEntity>
    )

    @Query("DELETE FROM contratos")
    suspend fun eliminarTodos()
}