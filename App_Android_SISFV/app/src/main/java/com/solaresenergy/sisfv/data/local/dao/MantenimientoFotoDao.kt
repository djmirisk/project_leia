package com.solaresenergy.sisfv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.solaresenergy.sisfv.data.local.entity.MantenimientoFotoEntity

@Dao
interface MantenimientoFotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(foto: MantenimientoFotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLista(fotos: List<MantenimientoFotoEntity>)

    @Query(
        """
        SELECT * FROM mantenimiento_fotos
        WHERE mantenimientoId = :mantenimientoId
        ORDER BY orden ASC
        """
    )
    suspend fun listarPorMantenimiento(
        mantenimientoId: String
    ): List<MantenimientoFotoEntity>

    @Query(
        """
        SELECT COUNT(*) FROM mantenimiento_fotos
        WHERE mantenimientoId = :mantenimientoId
        """
    )
    suspend fun contarPorMantenimiento(
        mantenimientoId: String
    ): Int
}