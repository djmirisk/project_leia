package com.solaresenergy.sisfv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.solaresenergy.sisfv.data.local.entity.SolicitudCambioContactoEntity

@Dao
interface SolicitudCambioContactoDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(
        solicitud: SolicitudCambioContactoEntity
    )

    @Query("""
        SELECT *
        FROM solicitudes_cambio_contacto
        ORDER BY fechaRegistro DESC
    """)
    suspend fun listarTodas(): List<SolicitudCambioContactoEntity>

    @Query("""
        SELECT *
        FROM solicitudes_cambio_contacto
        WHERE estadoRevision = :estado
        ORDER BY fechaRegistro DESC
    """)
    suspend fun listarPorEstado(
        estado: String
    ): List<SolicitudCambioContactoEntity>

    @Query("""
        SELECT COUNT(*)
        FROM solicitudes_cambio_contacto
        WHERE estadoRevision = :estado
    """)
    suspend fun contarPorEstado(
        estado: String
    ): Int
}