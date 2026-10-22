package com.solaresenergy.sisfv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cambios_componente")
data class CambioComponenteEntity(
    @PrimaryKey val id: String,
    val sistemaId: Int,
    val codigoSisfv: String,
    val contratoId: Int,
    val proyectoId: Int,
    val nombreUsuario: String,
    val documentoUsuario: String?,
    val componente: String,
    val causaFallo: String,
    val fotoComponenteMaloPath: String,
    val fotoSerialMaloPath: String,
    val fotoComponenteNuevoPath: String,
    val fotoSerialNuevoPath: String,
    val fotoSegundoComponenteMaloPath: String?,
    val fotoSegundoSerialMaloPath: String?,
    val fotoSegundoComponenteNuevoPath: String?,
    val fotoSegundoSerialNuevoPath: String?,
    val nombreReceptor: String,
    val documentoReceptor: String,
    val receptorEsTitular: Boolean,
    val firmaUsuarioPath: String,
    val firmaTecnicoPath: String,
    val tecnicoId: Int,
    val fechaRegistro: Long,
    val estadoRevision: String,
    val estadoSync: String
)
