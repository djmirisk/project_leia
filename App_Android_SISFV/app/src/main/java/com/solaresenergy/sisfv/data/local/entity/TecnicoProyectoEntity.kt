package com.solaresenergy.sisfv.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "tecnicos_proyectos",
    primaryKeys = ["tecnicoId", "proyectoId"]
)
data class TecnicoProyectoEntity(

    val tecnicoId: Int,

    val proyectoId: Int,

    val activo: Boolean = true,

    val fechaAsignacion: Long = System.currentTimeMillis()
)