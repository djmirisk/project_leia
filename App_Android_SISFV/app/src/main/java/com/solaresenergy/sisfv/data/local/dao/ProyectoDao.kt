package com.solaresenergy.sisfv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.solaresenergy.sisfv.data.local.entity.ProyectoEntity

@Dao
interface ProyectoDao {

    @Query("""
        SELECT * FROM proyectos
        WHERE activo = 1
        ORDER BY nombreProyecto ASC
    """)
    suspend fun obtenerTodosActivos(): List<ProyectoEntity>

    @Query("""
        SELECT * FROM proyectos
        WHERE id = :proyectoId
        LIMIT 1
    """)
    suspend fun obtenerPorId(proyectoId: Int): ProyectoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(
        proyecto: ProyectoEntity
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizarLista(
        proyectos: List<ProyectoEntity>
    )

    @Query("DELETE FROM proyectos")
    suspend fun eliminarTodos()
}