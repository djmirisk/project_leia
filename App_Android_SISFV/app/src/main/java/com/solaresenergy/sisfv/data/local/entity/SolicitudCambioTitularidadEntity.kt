package com.solaresenergy.sisfv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "solicitudes_cambio_titularidad")
data class SolicitudCambioTitularidadEntity(
    @PrimaryKey
    val id: String,
    val sistemaId: Int,
    val codigoSisfv: String,
    val contratoId: Int,
    val proyectoId: Int,
    val nombreUsuarioActual: String,
    val documentoUsuarioActual: String?,
    val nombreUsuarioNuevo: String,
    val documentoUsuarioNuevo: String,
    val telefonoUsuarioNuevo: String,
    val correoUsuarioNuevo: String?,
    val motivo: String,
    val fotoDocumentoFrentePath: String,
    val fotoDocumentoReversoPath: String,
    val fotoHuellaPath: String,
    val firmaTecnicoPath: String,
    val firmaNuevoUsuarioPath: String,
    val tecnicoId: Int,
    val fechaRegistro: Long,
    val estadoRevision: String,
    val estadoSync: String
)
