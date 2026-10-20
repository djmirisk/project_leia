package com.solaresenergy.sisfv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.solaresenergy.sisfv.data.local.entity.TecnicoContratoEntity

@Dao
interface TecnicoContratoDao {

    @Query("""
        SELECT contratoId
        FROM tecnicos_contratos
        WHERE tecnicoId = :tecnicoId
        AND activo = 1
    """)
    suspend fun obtenerIdsContratosDeTecnico(
        tecnicoId: Int
    ): List<Int>

    @Query("""
        SELECT * FROM tecnicos_contratos
        WHERE tecnicoId = :tecnicoId
        AND activo = 1
    """)
    suspend fun obtenerAsignacionesDeTecnico(
        tecnicoId: Int
    ): List<TecnicoContratoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(
        asignacion: TecnicoContratoEntity
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizarLista(
        asignaciones: List<TecnicoContratoEntity>
    )

    @Query("""
        DELETE FROM tecnicos_contratos
        WHERE tecnicoId = :tecnicoId
    """)
    suspend fun eliminarAsignacionesDeTecnico(
        tecnicoId: Int
    )
}