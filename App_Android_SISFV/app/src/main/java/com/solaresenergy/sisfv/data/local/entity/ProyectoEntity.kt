package com.solaresenergy.sisfv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "proyectos")
data class ProyectoEntity(

    @PrimaryKey
    val id: Int,

    val codigoProyecto: String,

    val nombreProyecto: String,

    val municipio: String? = null,

    val departamento: String? = null,

    val activo: Boolean = true,

    val ultimaSincronizacion: Long = System.currentTimeMillis()
)