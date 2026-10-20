package com.solaresenergy.sisfv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.solaresenergy.sisfv.data.local.entity.MantenimientoActividadEntity

@Dao
interface MantenimientoActividadDao {

    @Query("""
        SELECT * 
        FROM mantenimiento_actividades
        WHERE mantenimientoId = :mantenimientoId
        ORDER BY codigoActividad ASC
    """)
    suspend fun obtenerPorMantenimiento(
        mantenimientoId: String
    ): List<MantenimientoActividadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(
        actividad: MantenimientoActividadEntity
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLista(
        actividades: List<MantenimientoActividadEntity>
    )

    @Query("""
        DELETE FROM mantenimiento_actividades
        WHERE mantenimientoId = :mantenimientoId
    """)
    suspend fun eliminarPorMantenimiento(
        mantenimientoId: String
    )
}