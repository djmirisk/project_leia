package com.solaresenergy.sisfv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity

@Dao
interface TecnicoDao {

    @Query("SELECT * FROM tecnicos WHERE documento = :documento AND activo = 1 LIMIT 1")
    suspend fun obtenerPorDocumento(documento: String): TecnicoEntity?

    @Query("SELECT * FROM tecnicos WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Int): TecnicoEntity?

    @Query("SELECT * FROM tecnicos WHERE activo = 1")
    suspend fun obtenerTodos(): List<TecnicoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizarLista(tecnicos: List<TecnicoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(tecnico: TecnicoEntity)

    @Query("DELETE FROM tecnicos")
    suspend fun limpiarTodos()
}
