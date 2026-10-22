package com.solaresenergy.sisfv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.solaresenergy.sisfv.data.local.entity.CambioComponenteEntity

@Dao
interface CambioComponenteDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(cambio: CambioComponenteEntity)

    @Query("SELECT * FROM cambios_componente WHERE tecnicoId = :tecnicoId ORDER BY fechaRegistro DESC")
    suspend fun listarPorTecnico(tecnicoId: Int): List<CambioComponenteEntity>

    @Query("SELECT * FROM cambios_componente WHERE sistemaId = :sistemaId AND tecnicoId = :tecnicoId ORDER BY fechaRegistro DESC")
    suspend fun listarParaGaleria(sistemaId: Int, tecnicoId: Int): List<CambioComponenteEntity>

    @Query("SELECT COUNT(*) FROM cambios_componente WHERE tecnicoId = :tecnicoId AND estadoSync = 'PENDING_SYNC'")
    suspend fun contarPendientesSyncPorTecnico(tecnicoId: Int): Int

    @Query("SELECT COUNT(*) FROM cambios_componente WHERE tecnicoId = :tecnicoId")
    suspend fun contarPorTecnico(tecnicoId: Int): Int

    @Query("SELECT COUNT(*) FROM cambios_componente WHERE tecnicoId = :tecnicoId AND estadoSync = 'SYNCED'")
    suspend fun contarSincronizadosPorTecnico(tecnicoId: Int): Int
}
