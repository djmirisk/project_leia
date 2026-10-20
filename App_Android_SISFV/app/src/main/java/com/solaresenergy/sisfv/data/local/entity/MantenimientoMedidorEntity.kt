package com.solaresenergy.sisfv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mantenimiento_medidor")
data class MantenimientoMedidorEntity(

    @PrimaryKey
    val mantenimientoId: String,

    // Indica si el medidor aplica a la visita
    val aplica: Boolean = false,

    // NORMAL o CODIGO_ACTIVO
    val estado: String? = null,

    // Código mostrado por el medidor, si existe
    val codigoActivo: String? = null,

    // Energía consumida registrada en kWh
    // Puede quedar nula si no hay lectura disponible
    val energiaConsumidaKwh: Double? = null,

    // Ruta local de la fotografía de la lectura
    val evidenciaLecturaPath: String? = null,

    // Ruta local de la fotografía del código activo
    val evidenciaCodigoPath: String? = null,

    // Fecha y hora de la lectura, en milisegundos
    val fechaHoraLectura: Long? = null
)