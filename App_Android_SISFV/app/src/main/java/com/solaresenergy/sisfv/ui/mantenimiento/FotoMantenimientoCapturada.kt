package com.solaresenergy.sisfv.ui.mantenimiento

/** Evidencia final ya marcada con fecha, hora, técnico y posición GPS. */
data class FotoMantenimientoCapturada(
    val codigoFoto: String,
    val orden: Int,
    val titulo: String,
    val rutaEvidencia: String,
    val fechaCaptura: Long,
    val latitudGps: Double,
    val longitudGps: Double,
    val precisionGps: Float,
    val hashSha256: String
)
