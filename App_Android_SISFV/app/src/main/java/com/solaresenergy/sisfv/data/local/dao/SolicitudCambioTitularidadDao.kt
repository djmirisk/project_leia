package com.solaresenergy.sisfv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.solaresenergy.sisfv.data.local.entity.SolicitudCambioTitularidadEntity

@Dao
interface SolicitudCambioTitularidadDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(solicitud: SolicitudCambioTitularidadEntity)

    @Query("""
        SELECT * FROM solicitudes_cambio_titularidad
        ORDER BY fechaRegistro DESC
    """)
    suspend fun listarTodas(): List<SolicitudCambioTitularidadEntity>

    @Query("""
        SELECT COUNT(*) FROM solicitudes_cambio_titularidad
        WHERE tecnicoId = :tecnicoId AND estadoSync = 'PENDING_SYNC'
    """)
    suspend fun contarPendientesSyncPorTecnico(tecnicoId: Int): Int

    @Query("""
        SELECT * FROM solicitudes_cambio_titularidad
        WHERE sistemaId = :sistemaId AND tecnicoId = :tecnicoId
        ORDER BY fechaRegistro DESC
    """)
    suspend fun listarParaGaleria(
        sistemaId: Int,
        tecnicoId: Int
    ): List<SolicitudCambioTitularidadEntity>
}
