package com.solaresenergy.sisfv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "sistemas_sisfv")
data class SistemaSisfvEntity(

    @PrimaryKey
    val id: Int,

    // Proyecto al que pertenece este SISFV
    val proyectoId: Int,

    // contrato al que pertenece
    @ColumnInfo(defaultValue = "0")
    val contratoId: Int,

    // Identificadores de consulta
    val codigoSisfv: String,
    val documentoUsuario: String? = null,
    val nui: String? = null,

    // Información del usuario
    val nombreUsuario: String,

    // Ubicación
    val departamento: String? = null,
    val municipio: String? = null,
    val comunidad: String? = null,
    val vereda: String? = null,
    val direccion: String? = null,

    // Coordenadas registradas del sistema
    val latitud: Double? = null,
    val longitud: Double? = null,

    // Estado del SISFV
    val activo: Boolean = true,

    val ultimaSincronizacion: Long =
        System.currentTimeMillis()
)