package com.solaresenergy.sisfv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.solaresenergy.sisfv.data.local.entity.TecnicoProyectoEntity

@Dao
interface TecnicoProyectoDao {

    @Query("""
        SELECT * FROM tecnicos_proyectos
        WHERE tecnicoId = :tecnicoId
        AND activo = 1
    """)
    suspend fun obtenerAsignacionesDeTecnico(
        tecnicoId: Int
    ): List<TecnicoProyectoEntity>

    @Query("""
        SELECT proyectoId
        FROM tecnicos_proyectos
        WHERE tecnicoId = :tecnicoId
        AND activo = 1
    """)
    suspend fun obtenerIdsProyectosDeTecnico(
        tecnicoId: Int
    ): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(
        asignacion: TecnicoProyectoEntity
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizarLista(
        asignaciones: List<TecnicoProyectoEntity>
    )

    @Query("""
        DELETE FROM tecnicos_proyectos
        WHERE tecnicoId = :tecnicoId
    """)
    suspend fun eliminarAsignacionesDeTecnico(
        tecnicoId: Int
    )
}