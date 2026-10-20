package com.solaresenergy.sisfv.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "tecnicos_contratos",
    primaryKeys = ["tecnicoId", "contratoId"]
)
data class TecnicoContratoEntity(
    val tecnicoId: Int,
    val contratoId: Int,
    val activo: Boolean = true,
    val fechaAsignacion: Long = System.currentTimeMillis()
)