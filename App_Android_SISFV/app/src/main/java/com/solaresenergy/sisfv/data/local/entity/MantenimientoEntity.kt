package com.solaresenergy.sisfv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "mantenimientos_offline")
data class MantenimientoEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val codigoSisfv: String,

    // Técnico que realiza la visita
    val tecnicoId: Int,

    val nfcUid: String? = null,
    val tipoMantenimiento: String, // PREVENTIVO o CORRECTIVO
    val fechaVisita: Long = System.currentTimeMillis(),

    // Coordenadas GPS de Campo
    val latitudGps: Double,
    val longitudGps: Double,
    val precisionGps: Float,

    // Mediciones Eléctricas (ML-GT-001)
    val voltajePanelDc: Double,
    val corrientePanelDc: Double,
    val voltajeBateriaDc: Double,
    val corrienteBateriaDc: Double,
    val voltajeInversorAc: Double,

    // Estado de Protecciones
    val breakersStatus: String,
    val fusiblesStatus: String,
    val dpsStatus: String,
    val torqueBaterias: String,
    val torqueBarrajes: String,

    // Hallazgos y Pendientes
    val fallaCritica: Boolean = false,
    val observaciones: String? = null,
    val danoPendiente: String? = null,
    val repuestosRequeridos: String? = null,
    val justificacionNoReparacion: String? = null,

    // Ruta Local de Firmas Cifradas
    val firmaUsuarioPath: String? = null,
    val firmaTecnicoPath: String? = null,

    // Estado de Sincronización Offline-First
    val estadoSync: String = "PENDING_SYNC",

// Estado final del sistema al cerrar la visita
    val estadoFinalSisfv: String? = null,
    val observacionEstadoFinal: String? = null,
    val causaNoFuncional: String? = null,

// Persona que recibe la visita
    val nombreReceptorFirma: String? = null,
    val documentoReceptorFirma: String? = null,
    val receptorEsTitular: Boolean? = null,

// Estado del bombillo y funcionamiento del SISFV
    val bombilloEncendido: Boolean? = null,
    val resultadoVisita: String = "VISITA_EFECTIVA"
)
