package com.solaresenergy.sisfv.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "mantenimiento_fotos",
    primaryKeys = ["mantenimientoId", "codigoFoto"]
)
data class MantenimientoFotoEntity(
    val mantenimientoId: String,
    val codigoFoto: String,
    val orden: Int,
    val titulo: String,

    // Las dos columnas históricas apuntarán al mismo archivo definitivo ya marcado.
    // Así se conserva compatibilidad con la tabla creada en Room v7 sin duplicar imágenes.
    val rutaOriginal: String,
    val rutaConMarcaAgua: String? = null,

    val fechaCaptura: Long,
    val estadoSync: String = "PENDING_SYNC",

    // Datos GPS asociados a la fotografía
    val latitudGps: Double? = null,
    val longitudGps: Double? = null,
    val precisionGps: Float? = null,

    // Huella SHA-256 del archivo final con marca de agua
    val hashSha256: String? = null
)
