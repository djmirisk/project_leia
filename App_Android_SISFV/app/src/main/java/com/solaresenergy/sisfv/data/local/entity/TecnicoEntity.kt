package com.solaresenergy.sisfv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tecnicos")
data class TecnicoEntity(
    @PrimaryKey
    val id: Int,
    val documento: String, // Cédula o número de documento
    val nombre: String,
    val email: String,
    val passwordHash: String, // Hash o PIN cifrado
    val rol: String = "TECNICO", // TECNICO, COORDINADOR, GESTOR
    val activo: Boolean = true,
    val ultimaSincronizacion: Long = System.currentTimeMillis()
)
