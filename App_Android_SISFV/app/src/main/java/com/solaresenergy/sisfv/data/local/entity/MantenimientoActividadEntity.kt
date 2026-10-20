package com.solaresenergy.sisfv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "mantenimiento_actividades")
data class MantenimientoActividadEntity(

    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /**
     * ID del mantenimiento al que pertenece esta actividad.
     */
    val mantenimientoId: String,

    /**
     * Código interno de la actividad.
     *
     * Ejemplos:
     * BREAKER
     * FUSIBLES
     * DPS
     * TORQUE_BATERIA
     * TORQUE_BARRAJE_POSITIVO
     * TORQUE_BARRAJE_NEGATIVO
     * TORQUE_BARRAJE_NEUTRO
     */
    val codigoActividad: String,

    /**
     * Indica si el técnico realizó/verificó la actividad.
     */
    val realizada: Boolean = false,

    /**
     * Resultado de la actividad.
     *
     * Valores previstos:
     * CONFORME
     * NOVEDAD
     */
    val resultado: String? = null,

    /**
     * Descripción de la novedad cuando corresponda.
     */
    val observacion: String? = null
)