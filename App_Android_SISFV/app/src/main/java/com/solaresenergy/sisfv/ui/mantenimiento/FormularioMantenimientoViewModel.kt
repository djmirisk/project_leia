package com.solaresenergy.sisfv.ui.mantenimiento

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.solaresenergy.sisfv.data.local.AppDatabase
import com.solaresenergy.sisfv.data.local.entity.MantenimientoActividadEntity
import com.solaresenergy.sisfv.data.local.entity.MantenimientoEntity
import com.solaresenergy.sisfv.data.local.entity.MantenimientoMedidorEntity
import com.solaresenergy.sisfv.data.local.entity.MantenimientoFotoEntity
import com.solaresenergy.sisfv.data.local.entity.SolicitudCambioContactoEntity
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

private val CODIGOS_FOTOS_OBLIGATORIAS = setOf(
    "PANORAMICA_VIVIENDA",
    "POSTE_PANELES",
    "PANELES_DESDE_ABAJO",
    "SPT_PUESTA_TIERRA",
    "GABINETE_CERRADO",
    "GABINETE_ABIERTO",
    "INVERSOR",
    "CONTROLADOR",
    "BATERIA",
    "PROTECCIONES",
    "MEDIDOR_GABINETE",
    "TABLERO_DISTRIBUCION_AC",
    "INTERRUPTOR_LUCES",
    "TOMACORRIENTES",
    "BOMBILLO_ENCENDIDO",
    "VOLTAJE_BATERIA_HERRAMIENTA",
    "VOLTAJE_PANEL_HERRAMIENTA",
    "VOLTAJE_TOMACORRIENTE_HERRAMIENTA"
)

data class FormularioMantenimientoUiState(
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

data class FormularioMantenimientoData(
    val sistemaId: Int,
    val codigoSisfv: String,
    val nombreUsuario: String,
    val documentoUsuario: String?,
    val tecnicoId: Int,
    val telefonoContactoPropuesto: String = "",
    val correoContactoPropuesto: String = "",

    val tipoMantenimiento: String,

    // Ubicación GPS validada de la visita
    val latitudGps: Double,
    val longitudGps: Double,
    val precisionGps: Float,

    val voltajePanelDc: Double,
    val corrientePanelDc: Double,
    val voltajeBateriaDc: Double,
    val corrienteBateriaDc: Double,
    val voltajeInversorAc: Double,

    val breakerVerificado: Boolean,
    val breakerConforme: Boolean,
    val breakerNovedad: String,

    val fusibleVerificado: Boolean,
    val fusibleConforme: Boolean,
    val fusibleNovedad: String,

    val dpsVerificado: Boolean,
    val dpsConforme: Boolean,
    val dpsNovedad: String,

    val torqueBateria: Boolean,
    val torqueBateriaConforme: Boolean,
    val torqueBateriaNovedad: String,

    val torqueBarrajePositivo: Boolean,
    val torqueBarrajePositivoConforme: Boolean,
    val torqueBarrajePositivoNovedad: String,

    val torqueBarrajeNegativo: Boolean,
    val torqueBarrajeNegativoConforme: Boolean,
    val torqueBarrajeNegativoNovedad: String,

    val torqueBarrajeNeutro: Boolean,
    val torqueBarrajeNeutroConforme: Boolean,
    val torqueBarrajeNeutroNovedad: String,

    val medidorAplica: Boolean,
    val medidorNormal: Boolean,
    val codigoActivo: String,

    val observaciones: String,

    // Lectura y evidencias del Excelec PPKW
    val energiaConsumidaKwh: Double? = null,
    val evidenciaLecturaPath: String? = null,
    val evidenciaCodigoPath: String? = null,
    val fechaHoraLectura: Long? = null,

    // La evaluación del bombillo afecta el estado de todo el SISFV
    val bombilloEncendido: Boolean? = null,

    // 18 fotografías finales con marca de agua y GPS
    val fotos: List<FotoMantenimientoCapturada> = emptyList(),

    // Datos de recibido y firmas manuscritas
    val nombreReceptorFirma: String? = null,
    val documentoReceptorFirma: String? = null,
    val receptorEsTitular: Boolean? = null,
    val firmaUsuarioPath: String? = null,
    val firmaTecnicoPath: String? = null
)

class FormularioMantenimientoViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)

    private val mantenimientoDao =
        db.mantenimientoDao()

    private val actividadDao =
        db.mantenimientoActividadDao()

    private val medidorDao =
        db.mantenimientoMedidorDao()

    private val _uiState =
        MutableStateFlow(
            FormularioMantenimientoUiState()
        )

    val uiState: StateFlow<FormularioMantenimientoUiState> =
        _uiState.asStateFlow()

    fun guardarMantenimiento(
        datos: FormularioMantenimientoData
    ) {

        viewModelScope.launch {

            _uiState.value =
                FormularioMantenimientoUiState(
                    isSaving = true
                )

            try {

                val telefonoPropuesto = datos.telefonoContactoPropuesto.trim()
                val correoPropuesto = datos.correoContactoPropuesto.trim()
                if (correoPropuesto.isNotEmpty() && telefonoPropuesto.isEmpty()) {
                    throw IllegalArgumentException(
                        "Para solicitar un cambio de correo también debe ingresar el nuevo teléfono."
                    )
                }
                if (telefonoPropuesto.isNotEmpty() &&
                    (!telefonoPropuesto.matches(Regex("^[+0-9() .-]+$")) ||
                            telefonoPropuesto.count(Char::isDigit) !in 7..15)
                ) {
                    throw IllegalArgumentException("Ingrese un teléfono válido de 7 a 15 dígitos.")
                }
                if (correoPropuesto.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(correoPropuesto).matches()) {
                    throw IllegalArgumentException("Ingrese un correo electrónico válido.")
                }

                // =================================================
                // VALIDACIÓN DE LAS 18 FOTOGRAFÍAS OBLIGATORIAS
                // =================================================
                if (datos.bombilloEncendido == null) {
                    throw IllegalArgumentException(
                        "Indique si el bombillo enciende o no durante la prueba."
                    )
                }
                if (!datos.latitudGps.isFinite() || !datos.longitudGps.isFinite() ||
                    !datos.precisionGps.isFinite() || datos.precisionGps < 0f) {
                    throw IllegalArgumentException(
                        "La ubicación GPS de la visita no es válida. Verifique la ubicación y vuelva a guardar."
                    )
                }

                if (datos.nombreReceptorFirma.isNullOrBlank()) {
                    throw IllegalArgumentException("Falta el nombre de la persona que recibe la visita.")
                }
                if (datos.documentoReceptorFirma.isNullOrBlank()) {
                    throw IllegalArgumentException("Falta el documento de la persona que recibe la visita.")
                }
                if (datos.receptorEsTitular == null) {
                    throw IllegalArgumentException("Indique si la persona que recibe es el titular del SISFV.")
                }
                listOf(
                    "firma de la persona que recibe" to datos.firmaUsuarioPath,
                    "firma del técnico" to datos.firmaTecnicoPath
                ).forEach { (descripcion, ruta) ->
                    val archivo = ruta?.let { File(it) }
                    if (archivo == null || !archivo.isFile || archivo.length() <= 0L) {
                        throw IllegalArgumentException("Debe guardar una imagen válida para la $descripcion.")
                    }
                }

                val codigosRecibidos = datos.fotos.map { it.codigoFoto }
                val faltantes = CODIGOS_FOTOS_OBLIGATORIAS - codigosRecibidos.toSet()
                if (datos.fotos.size != 18 || faltantes.isNotEmpty() || codigosRecibidos.toSet().size != datos.fotos.size) {
                    throw IllegalArgumentException(
                        if (faltantes.isEmpty()) {
                            "Debe registrar exactamente las 18 fotografías obligatorias."
                        } else {
                            "Faltan fotografías obligatorias: ${faltantes.joinToString(", ")}."
                        }
                    )
                }

                datos.fotos.forEach { foto ->
                    val archivo = File(foto.rutaEvidencia)
                    if (!archivo.exists() || archivo.length() == 0L) {
                        throw IllegalArgumentException(
                            "La fotografía ${foto.titulo} no está disponible. Vuelva a capturarla."
                        )
                    }
                    if (foto.hashSha256.isBlank()) {
                        throw IllegalArgumentException(
                            "No se pudo verificar la fotografía ${foto.titulo}. Vuelva a capturarla."
                        )
                    }
                }

                // =================================================
                // VALIDACIÓN DEL MEDIDOR
                // =================================================

                if (
                    datos.medidorAplica &&
                    !datos.medidorNormal &&
                    datos.codigoActivo.isBlank()
                ) {

                    throw IllegalArgumentException(
                        "Debe ingresar el código activo del medidor."
                    )
                }

                // =================================================
                // ID DEL MANTENIMIENTO
                // =================================================

                val mantenimientoId =
                    UUID.randomUUID().toString()

                // =================================================
                // MANTENIMIENTO PRINCIPAL
                // =================================================

                val mantenimiento =
                    MantenimientoEntity(

                        id = mantenimientoId,

                        codigoSisfv =
                            datos.codigoSisfv,

                        tecnicoId =
                            datos.tecnicoId,

                        nfcUid = null,

                        tipoMantenimiento =
                            datos.tipoMantenimiento,

                        latitudGps = datos.latitudGps,

                        longitudGps = datos.longitudGps,

                        precisionGps = datos.precisionGps,

                        voltajePanelDc =
                            datos.voltajePanelDc,

                        corrientePanelDc =
                            datos.corrientePanelDc,

                        voltajeBateriaDc =
                            datos.voltajeBateriaDc,

                        corrienteBateriaDc =
                            datos.corrienteBateriaDc,

                        voltajeInversorAc =
                            datos.voltajeInversorAc,

                        breakersStatus =
                            obtenerEstado(
                                datos.breakerVerificado,
                                datos.breakerConforme
                            ),

                        fusiblesStatus =
                            obtenerEstado(
                                datos.fusibleVerificado,
                                datos.fusibleConforme
                            ),

                        dpsStatus =
                            obtenerEstado(
                                datos.dpsVerificado,
                                datos.dpsConforme
                            ),

                        torqueBaterias =
                            obtenerEstado(
                                datos.torqueBateria,
                                datos.torqueBateriaConforme
                            ),

                        torqueBarrajes =
                            obtenerEstadoTorqueBarrajes(
                                datos
                            ),

                        fallaCritica = datos.bombilloEncendido == false,

                        observaciones =
                            datos.observaciones
                                .trim()
                                .ifBlank {
                                    null
                                },

                        danoPendiente = null,

                        repuestosRequeridos = null,

                        justificacionNoReparacion = null,

                        firmaUsuarioPath = datos.firmaUsuarioPath,

                        firmaTecnicoPath = datos.firmaTecnicoPath,

                        estadoSync = "PENDING_SYNC",

                        // Si el bombillo no enciende, se reporta todo el SISFV como NO FUNCIONAL.
                        estadoFinalSisfv = if (datos.bombilloEncendido == false) "NO_FUNCIONAL" else null,
                        observacionEstadoFinal = if (datos.bombilloEncendido == false) {
                            "El bombillo no enciende; se registra todo el SISFV como NO FUNCIONAL."
                        } else null,
                        causaNoFuncional = if (datos.bombilloEncendido == false) {
                            "Bombillo apagado durante la prueba de funcionamiento."
                        } else null,
                        bombilloEncendido = datos.bombilloEncendido,
                        nombreReceptorFirma = datos.nombreReceptorFirma?.trim(),
                        documentoReceptorFirma = datos.documentoReceptorFirma?.trim(),
                        receptorEsTitular = datos.receptorEsTitular
                    )

                // =================================================
                // ACTIVIDADES
                // =================================================

                val actividades =
                    construirActividades(
                        mantenimientoId,
                        datos
                    )

                // =================================================
                // MEDIDOR
                // =================================================

                val medidor =
                    MantenimientoMedidorEntity(

                        mantenimientoId =
                            mantenimientoId,

                        aplica =
                            datos.medidorAplica,

                        estado =
                            if (!datos.medidorAplica) {
                                "NO_INSTALADO"
                            } else if (datos.medidorNormal) {
                                "NORMAL"
                            } else {
                                "CODIGO_ACTIVO"
                            },

                        codigoActivo =
                            if (
                                datos.medidorAplica &&
                                !datos.medidorNormal
                            ) {
                                datos.codigoActivo
                                    .trim()
                            } else {
                                null
                            },
                        energiaConsumidaKwh =
                            datos.energiaConsumidaKwh,

                        evidenciaLecturaPath =
                            datos.evidenciaLecturaPath,

                        evidenciaCodigoPath =
                            datos.evidenciaCodigoPath,

                        fechaHoraLectura =
                            datos.fechaHoraLectura
                    )

                // =================================================
                // PREPARAR REGISTROS DE LAS FOTOGRAFÍAS
                // Cada ruta apunta al único archivo final ya marcado.
                // Las columnas históricas de ruta conservan la compatibilidad con Room v7.
                // =================================================
                val fotosParaGuardar = datos.fotos.map { foto ->
                    MantenimientoFotoEntity(
                        mantenimientoId = mantenimientoId,
                        codigoFoto = foto.codigoFoto,
                        orden = foto.orden,
                        titulo = foto.titulo,
                        rutaOriginal = foto.rutaEvidencia,
                        rutaConMarcaAgua = foto.rutaEvidencia,
                        fechaCaptura = foto.fechaCaptura,
                        estadoSync = "PENDING_SYNC",
                        latitudGps = foto.latitudGps,
                        longitudGps = foto.longitudGps,
                        precisionGps = foto.precisionGps,
                        hashSha256 = foto.hashSha256
                    )
                }

                val solicitudCambioContacto = telefonoPropuesto.takeIf { it.isNotEmpty() }?.let {
                    SolicitudCambioContactoEntity(
                        id = UUID.randomUUID().toString(),
                        sistemaId = datos.sistemaId,
                        codigoSisfv = datos.codigoSisfv,
                        nombreUsuario = datos.nombreUsuario,
                        documentoUsuario = datos.documentoUsuario,
                        tecnicoId = datos.tecnicoId,
                        telefonoPropuesto = it,
                        correoPropuesto = correoPropuesto.ifBlank { null },
                        fechaRegistro = System.currentTimeMillis(),
                        estadoRevision = "PENDIENTE_REVISION",
                        estadoSync = "PENDING_SYNC"
                    )
                }

                // =================================================
                // GUARDADO EN UNA SOLA TRANSACCIÓN
                // =================================================

                db.withTransaction {

                    mantenimientoDao.insertar(
                        mantenimiento
                    )

                    if (actividades.isNotEmpty()) {

                        actividadDao.insertarLista(
                            actividades
                        )
                    }

                    medidorDao.insertar(
                        medidor
                    )

                    db.mantenimientoFotoDao().insertarLista(fotosParaGuardar)

                    solicitudCambioContacto?.let {
                        db.solicitudCambioContactoDao().insertar(it)
                    }
                }

                _uiState.value =
                    FormularioMantenimientoUiState(
                        isSaving = false,
                        saveSuccess = true
                    )

            } catch (e: Exception) {

                _uiState.value =
                    FormularioMantenimientoUiState(
                        isSaving = false,
                        errorMessage =
                            e.message
                                ?: "No fue posible guardar el mantenimiento."
                    )
            }
        }
    }

    private fun construirActividades(
        mantenimientoId: String,
        datos: FormularioMantenimientoData
    ): List<MantenimientoActividadEntity> {

        val actividades =
            mutableListOf<MantenimientoActividadEntity>()

        if (datos.breakerVerificado) {

            actividades.add(
                crearActividad(
                    mantenimientoId,
                    "BREAKER",
                    datos.breakerConforme,
                    datos.breakerNovedad
                )
            )
        }

        if (datos.fusibleVerificado) {

            actividades.add(
                crearActividad(
                    mantenimientoId,
                    "FUSIBLES",
                    datos.fusibleConforme,
                    datos.fusibleNovedad
                )
            )
        }

        if (datos.dpsVerificado) {

            actividades.add(
                crearActividad(
                    mantenimientoId,
                    "DPS",
                    datos.dpsConforme,
                    datos.dpsNovedad
                )
            )
        }

        if (datos.torqueBateria) {

            actividades.add(
                crearActividad(
                    mantenimientoId,
                    "TORQUE_BATERIA",
                    datos.torqueBateriaConforme,
                    datos.torqueBateriaNovedad
                )
            )
        }

        if (datos.torqueBarrajePositivo) {

            actividades.add(
                crearActividad(
                    mantenimientoId,
                    "TORQUE_BARRAJE_POSITIVO",
                    datos.torqueBarrajePositivoConforme,
                    datos.torqueBarrajePositivoNovedad
                )
            )
        }

        if (datos.torqueBarrajeNegativo) {

            actividades.add(
                crearActividad(
                    mantenimientoId,
                    "TORQUE_BARRAJE_NEGATIVO",
                    datos.torqueBarrajeNegativoConforme,
                    datos.torqueBarrajeNegativoNovedad
                )
            )
        }

        if (datos.torqueBarrajeNeutro) {

            actividades.add(
                crearActividad(
                    mantenimientoId,
                    "TORQUE_BARRAJE_NEUTRO",
                    datos.torqueBarrajeNeutroConforme,
                    datos.torqueBarrajeNeutroNovedad
                )
            )
        }

        return actividades
    }

    private fun crearActividad(
        mantenimientoId: String,
        codigo: String,
        conforme: Boolean,
        observacion: String
    ): MantenimientoActividadEntity {

        return MantenimientoActividadEntity(

            mantenimientoId =
                mantenimientoId,

            codigoActividad =
                codigo,

            realizada = true,

            resultado =
                if (conforme) {
                    "CONFORME"
                } else {
                    "NOVEDAD"
                },

            observacion =
                observacion
                    .trim()
                    .ifBlank {
                        null
                    }
        )
    }

    private fun obtenerEstado(
        verificado: Boolean,
        conforme: Boolean
    ): String {

        if (!verificado) {
            return "NO_VERIFICADO"
        }

        return if (conforme) {
            "CONFORME"
        } else {
            "NOVEDAD"
        }
    }

    private fun obtenerEstadoTorqueBarrajes(
        datos: FormularioMantenimientoData
    ): String {

        val algunoVerificado =
            datos.torqueBarrajePositivo ||
                    datos.torqueBarrajeNegativo ||
                    datos.torqueBarrajeNeutro

        if (!algunoVerificado) {
            return "NO_VERIFICADO"
        }

        val hayNovedad =
            (
                    datos.torqueBarrajePositivo &&
                            !datos.torqueBarrajePositivoConforme
                    ) ||
                    (
                            datos.torqueBarrajeNegativo &&
                                    !datos.torqueBarrajeNegativoConforme
                            ) ||
                    (
                            datos.torqueBarrajeNeutro &&
                                    !datos.torqueBarrajeNeutroConforme
                            )

        return if (hayNovedad) {
            "NOVEDAD"
        } else {
            "CONFORME"
        }
    }

    fun limpiarEstado() {

        _uiState.value =
            FormularioMantenimientoUiState()
    }
}
