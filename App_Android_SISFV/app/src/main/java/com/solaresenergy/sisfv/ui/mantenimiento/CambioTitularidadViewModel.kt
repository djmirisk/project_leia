package com.solaresenergy.sisfv.ui.mantenimiento

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.solaresenergy.sisfv.data.local.AppDatabase
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity
import com.solaresenergy.sisfv.data.local.entity.SolicitudCambioTitularidadEntity
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CambioTitularidadUiState(
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

data class CambioTitularidadData(
    val nombreNuevo: String,
    val documentoNuevo: String,
    val telefonoNuevo: String,
    val correoNuevo: String,
    val motivo: String,
    val fotoDocumentoFrentePath: String?,
    val fotoDocumentoReversoPath: String?,
    val fotoHuellaPath: String?,
    val firmaTecnicoPath: String?,
    val firmaNuevoUsuarioPath: String?
)

class CambioTitularidadViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val _uiState = MutableStateFlow(CambioTitularidadUiState())
    val uiState: StateFlow<CambioTitularidadUiState> = _uiState.asStateFlow()

    fun guardarSolicitud(
        sistemaSeleccionado: SistemaSisfvEntity,
        tecnico: TecnicoEntity,
        datos: CambioTitularidadData
    ) {
        viewModelScope.launch {
            _uiState.value = CambioTitularidadUiState(isSaving = true)
            try {
                val nombreNuevo = datos.nombreNuevo.trim()
                val documentoNuevo = datos.documentoNuevo.trim()
                val telefonoNuevo = datos.telefonoNuevo.trim()
                val correoNuevo = datos.correoNuevo.trim()
                val motivo = datos.motivo.trim()

                if (nombreNuevo.isBlank()) {
                    throw IllegalArgumentException("Ingrese el nombre completo del nuevo usuario.")
                }
                if (documentoNuevo.isBlank()) {
                    throw IllegalArgumentException("Ingrese el número de documento del nuevo usuario.")
                }
                if (telefonoNuevo.isBlank() ||
                    !telefonoNuevo.matches(Regex("^[+0-9() .-]+$")) ||
                    telefonoNuevo.count(Char::isDigit) !in 7..15
                ) {
                    throw IllegalArgumentException("Ingrese un teléfono válido de 7 a 15 dígitos.")
                }
                if (correoNuevo.isNotBlank() &&
                    !Patterns.EMAIL_ADDRESS.matcher(correoNuevo).matches()
                ) {
                    throw IllegalArgumentException("Ingrese un correo electrónico válido o deje el campo vacío.")
                }
                if (motivo.isBlank()) {
                    throw IllegalArgumentException("Describa el motivo de la solicitud de cambio de titularidad.")
                }

                val fotoFrente = validarArchivo(datos.fotoDocumentoFrentePath, "la foto del documento por delante")
                val fotoReverso = validarArchivo(datos.fotoDocumentoReversoPath, "la foto del documento por detrás")
                val fotoHuella = validarArchivo(datos.fotoHuellaPath, "la foto de la huella en papel blanco")
                val firmaTecnico = validarArchivo(datos.firmaTecnicoPath, "la firma del técnico")
                val firmaNuevoUsuario = validarArchivo(datos.firmaNuevoUsuarioPath, "la firma del nuevo usuario")

                val contratosAsignados = db.tecnicoContratoDao()
                    .obtenerIdsContratosDeTecnico(tecnico.id)
                if (sistemaSeleccionado.contratoId !in contratosAsignados) {
                    throw IllegalArgumentException("El SISFV seleccionado no pertenece a un contrato asignado a este técnico.")
                }

                val sistemaActual = db.sistemaSisfvDao().obtenerPorId(
                    id = sistemaSeleccionado.id,
                    contratoId = sistemaSeleccionado.contratoId
                ) ?: throw IllegalArgumentException("El SISFV ya no está disponible en la base local.")

                val solicitud = SolicitudCambioTitularidadEntity(
                    id = UUID.randomUUID().toString(),
                    sistemaId = sistemaActual.id,
                    codigoSisfv = sistemaActual.codigoSisfv,
                    contratoId = sistemaActual.contratoId,
                    proyectoId = sistemaActual.proyectoId,
                    nombreUsuarioActual = sistemaActual.nombreUsuario,
                    documentoUsuarioActual = sistemaActual.documentoUsuario,
                    nombreUsuarioNuevo = nombreNuevo,
                    documentoUsuarioNuevo = documentoNuevo,
                    telefonoUsuarioNuevo = telefonoNuevo,
                    correoUsuarioNuevo = correoNuevo.ifBlank { null },
                    motivo = motivo,
                    fotoDocumentoFrentePath = fotoFrente,
                    fotoDocumentoReversoPath = fotoReverso,
                    fotoHuellaPath = fotoHuella,
                    firmaTecnicoPath = firmaTecnico,
                    firmaNuevoUsuarioPath = firmaNuevoUsuario,
                    tecnicoId = tecnico.id,
                    fechaRegistro = System.currentTimeMillis(),
                    estadoRevision = "PENDIENTE_REVISION",
                    estadoSync = "PENDING_SYNC"
                )

                db.withTransaction {
                    db.solicitudCambioTitularidadDao().insertar(solicitud)
                }

                _uiState.value = CambioTitularidadUiState(saveSuccess = true)
            } catch (e: Exception) {
                _uiState.value = CambioTitularidadUiState(
                    errorMessage = e.message ?: "No fue posible guardar la solicitud localmente."
                )
            }
        }
    }

    private fun validarArchivo(ruta: String?, descripcion: String): String {
        val archivo = ruta?.let(::File)
        if (archivo == null || !archivo.isFile || archivo.length() <= 0L) {
            throw IllegalArgumentException("Debe registrar $descripcion.")
        }
        return archivo.absolutePath
    }

    fun limpiarEstado() {
        _uiState.value = CambioTitularidadUiState()
    }
}
