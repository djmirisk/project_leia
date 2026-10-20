package com.solaresenergy.sisfv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contratos")
data class ContratoEntity(
    @PrimaryKey
    val id: Int,
    val proyectoId: Int,
    val codigoContrato: String,
    val nombreContrato: String,
    val activo: Boolean = true,
    val ultimaSincronizacion: Long = System.currentTimeMillis()
)