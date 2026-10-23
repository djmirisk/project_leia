package com.solaresenergy.sisfv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.solaresenergy.sisfv.data.local.entity.MantenimientoEntity

@Dao
interface MantenimientoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(
        mantenimiento: MantenimientoEntity
    )

    @Query("""
        SELECT COUNT(*)
        FROM mantenimientos_offline
        WHERE tecnicoId = :tecnicoId
        AND resultadoVisita = 'VISITA_EFECTIVA'
    """)
    suspend fun contarPorTecnico(
        tecnicoId: Int
    ): Int

    @Query("""
        SELECT COUNT(*)
        FROM mantenimientos_offline
        WHERE tecnicoId = :tecnicoId
        AND estadoSync = 'PENDING_SYNC'
    """)
    suspend fun contarPendientesPorTecnico(
        tecnicoId: Int
    ): Int

    @Query("SELECT COUNT(*) FROM mantenimientos_offline WHERE tecnicoId = :tecnicoId AND estadoSync = 'SYNCED' AND resultadoVisita = 'VISITA_EFECTIVA'")
    suspend fun contarSincronizadosPorTecnico(tecnicoId: Int): Int

    @Query("""
        SELECT * FROM mantenimientos_offline
        WHERE codigoSisfv = :codigoSisfv
        AND tecnicoId = :tecnicoId
        ORDER BY fechaVisita DESC
    """)
    suspend fun listarParaGaleria(
        codigoSisfv: String,
        tecnicoId: Int
    ): List<MantenimientoEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM mantenimientos_offline WHERE codigoSisfv = :codigoSisfv AND tecnicoId = :tecnicoId AND resultadoVisita = 'VISITA_EFECTIVA')")
    suspend fun tieneVisitaEfectiva(codigoSisfv: String, tecnicoId: Int): Boolean
}
