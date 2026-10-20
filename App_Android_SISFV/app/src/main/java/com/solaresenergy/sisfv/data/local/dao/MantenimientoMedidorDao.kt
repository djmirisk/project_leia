package com.solaresenergy.sisfv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.solaresenergy.sisfv.data.local.entity.MantenimientoMedidorEntity

@Dao
interface MantenimientoMedidorDao {

    @Query("""
        SELECT *
        FROM mantenimiento_medidor
        WHERE mantenimientoId = :mantenimientoId
        LIMIT 1
    """)
    suspend fun obtenerPorMantenimiento(
        mantenimientoId: String
    ): MantenimientoMedidorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(
        medidor: MantenimientoMedidorEntity
    )

    @Query("""
        DELETE FROM mantenimiento_medidor
        WHERE mantenimientoId = :mantenimientoId
    """)
    suspend fun eliminarPorMantenimiento(
        mantenimientoId: String
    )
}