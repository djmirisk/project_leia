package com.solaresenergy.sisfv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity

@Dao
interface SistemaSisfvDao {

    // =========================================================
    // OBTENER SISFV DE UN CONTRATO
    // =========================================================

    @Query("""
        SELECT * FROM sistemas_sisfv
        WHERE contratoId = :contratoId
        AND activo = 1
        ORDER BY nombreUsuario ASC
    """)
    suspend fun obtenerPorContrato(
        contratoId: Int
    ): List<SistemaSisfvEntity>


    // =========================================================
    // BUSCAR SISFV DENTRO DE UN CONTRATO
    //
    // comunidad y vereda son OPCIONALES.
    // Si vienen null o vacías, no filtran.
    // =========================================================

    @Query("""
        SELECT * FROM sistemas_sisfv
        WHERE contratoId = :contratoId
        AND activo = 1

        AND (
            :comunidad IS NULL
            OR :comunidad = ''
            OR comunidad = :comunidad
        )

        AND (
            :vereda IS NULL
            OR :vereda = ''
            OR vereda = :vereda
        )

       AND (
        :texto = ''
        OR codigoSisfv LIKE '%' || :texto || '%'
        OR documentoUsuario LIKE '%' || :texto || '%'
        OR nui LIKE '%' || :texto || '%'
        OR nombreUsuario LIKE '%' || :texto || '%'
        OR CAST(id AS TEXT) LIKE '%' || :texto || '%'
)

        ORDER BY nombreUsuario ASC
    """)
    suspend fun buscarPorContrato(
        contratoId: Int,
        texto: String,
        comunidad: String?,
        vereda: String?
    ): List<SistemaSisfvEntity>


    // =========================================================
    // OBTENER COMUNIDADES DE UN CONTRATO
    // =========================================================

    @Query("""
        SELECT DISTINCT comunidad
        FROM sistemas_sisfv
        WHERE contratoId = :contratoId
        AND activo = 1
        AND comunidad IS NOT NULL
        AND comunidad != ''
        ORDER BY comunidad ASC
    """)
    suspend fun obtenerComunidadesPorContrato(
        contratoId: Int
    ): List<String>


    // =========================================================
    // OBTENER VEREDAS DE UN CONTRATO
    //
    // Si se selecciona comunidad, se filtran sus veredas.
    // Si comunidad es null, devuelve todas las veredas.
    // =========================================================

    @Query("""
        SELECT DISTINCT vereda
        FROM sistemas_sisfv
        WHERE contratoId = :contratoId
        AND activo = 1

        AND (
            :comunidad IS NULL
            OR :comunidad = ''
            OR comunidad = :comunidad
        )

        AND vereda IS NOT NULL
        AND vereda != ''

        ORDER BY vereda ASC
    """)
    suspend fun obtenerVeredasPorContrato(
        contratoId: Int,
        comunidad: String?
    ): List<String>


    // =========================================================
    // OBTENER SISFV POR CÓDIGO
    // =========================================================

    @Query("""
        SELECT * FROM sistemas_sisfv
        WHERE codigoSisfv = :codigoSisfv
        AND contratoId = :contratoId
        AND activo = 1
        LIMIT 1
    """)
    suspend fun obtenerPorCodigo(
        codigoSisfv: String,
        contratoId: Int
    ): SistemaSisfvEntity?


    // =========================================================
    // OBTENER SISFV POR ID
    // =========================================================

    @Query("""
        SELECT * FROM sistemas_sisfv
        WHERE id = :id
        AND contratoId = :contratoId
        AND activo = 1
        LIMIT 1
    """)
    suspend fun obtenerPorId(
        id: Int,
        contratoId: Int
    ): SistemaSisfvEntity?


    // =========================================================
    // INSERTAR / ACTUALIZAR
    // =========================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(
        sistema: SistemaSisfvEntity
    )


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizarLista(
        sistemas: List<SistemaSisfvEntity>
    )


    // =========================================================
    // ELIMINAR SISFV DE UN PROYECTO
    // =========================================================

    @Query("""
        DELETE FROM sistemas_sisfv
        WHERE proyectoId = :proyectoId
    """)
    suspend fun eliminarPorProyecto(
        proyectoId: Int
    )
}