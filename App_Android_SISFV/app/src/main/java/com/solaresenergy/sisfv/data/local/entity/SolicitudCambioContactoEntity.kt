package com.solaresenergy.sisfv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "solicitudes_cambio_contacto")
data class SolicitudCambioContactoEntity(

    @PrimaryKey
    val id: String,

    // Identificación del sistema y del usuario
    val sistemaId: Int,
    val codigoSisfv: String,
    val nombreUsuario: String,
    val documentoUsuario: String?,

    // Técnico que registra la solicitud
    val tecnicoId: Int,

    // Datos de contacto propuestos
    val telefonoPropuesto: String,
    val correoPropuesto: String?,

    // Control de la solicitud
    val fechaRegistro: Long,
    val estadoRevision: String,
    val estadoSync: String
)