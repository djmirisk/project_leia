package com.solaresenergy.sisfv.ui.mantenimiento

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.solaresenergy.sisfv.data.local.AppDatabase
import com.solaresenergy.sisfv.data.local.entity.CambioComponenteEntity
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CambioComponenteUiState(
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

data class CambioComponenteData(
    val componente: String,
    val causaFallo: String,
    val fotoComponenteMaloPath: String?,
    val fotoSerialMaloPath: String?,
    val fotoComponenteNuevoPath: String?,
    val fotoSerialNuevoPath: String?,
    val fotoSegundoComponenteMaloPath: String?,
    val fotoSegundoSerialMaloPath: String?,
    val fotoSegundoComponenteNuevoPath: String?,
    val fotoSegundoSerialNuevoPath: String?,
    val nombreReceptor: String,
    val documentoReceptor: String,
    val receptorEsTitular: Boolean?,
    val firmaUsuarioPath: String?,
    val firmaTecnicoPath: String?
)

class CambioComponenteViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val _uiState = MutableStateFlow(CambioComponenteUiState())
    val uiState: StateFlow<CambioComponenteUiState> = _uiState.asStateFlow()

    fun guardar(sistema: SistemaSisfvEntity, tecnico: TecnicoEntity, datos: CambioComponenteData) {
        viewModelScope.launch {
            _uiState.value = CambioComponenteUiState(isSaving = true)
            try {
                val componente = datos.componente.trim()
                val causa = datos.causaFallo.trim()
                if (componente.isBlank()) throw IllegalArgumentException("Seleccione el componente mayor que fue reemplazado.")
                if (causa.isBlank()) throw IllegalArgumentException("Indique causa del fallo.")
                val esTitular = datos.receptorEsTitular
                    ?: throw IllegalArgumentException("Indique si la persona que recibe es el titular del SISFV.")

                // Si quien recibe es el titular no se diligencia nada: se registran sus datos maestros.
                // Si no lo es, nombre y documento son obligatorios y deben diferir del titular.
                val nombreTitular = sistema.nombreUsuario.trim()
                val documentoTitular = sistema.documentoUsuario?.trim().orEmpty()
                val nombre = if (esTitular) nombreTitular else datos.nombreReceptor.trim()
                val documento = if (esTitular) documentoTitular else datos.documentoReceptor.trim()
                if (!esTitular) {
                    if (nombre.isBlank()) {
                        throw IllegalArgumentException("Ingrese el nombre de la persona responsable de la visita.")
                    }
                    if (documento.isBlank()) {
                        throw IllegalArgumentException("Ingrese el documento de la persona responsable de la visita.")
                    }
                    if (nombre.equals(nombreTitular, ignoreCase = true)) {
                        throw IllegalArgumentException(
                            "El nombre del receptor no puede ser el del titular registrado. Si quien recibe es el titular, seleccione «Sí»."
                        )
                    }
                    if (documentoTitular.isNotEmpty() && documento == documentoTitular) {
                        throw IllegalArgumentException(
                            "El documento del receptor no puede ser el del titular registrado. Si quien recibe es el titular, seleccione «Sí»."
                        )
                    }
                }
                val fotos = listOf(
                    validarArchivo(datos.fotoComponenteMaloPath, "la fotografía del componente malo"),
                    validarArchivo(datos.fotoSerialMaloPath, "la fotografía del serial del componente malo"),
                    validarArchivo(datos.fotoComponenteNuevoPath, "la fotografía del componente nuevo"),
                    validarArchivo(datos.fotoSerialNuevoPath, "la fotografía del serial del componente nuevo")
                )
                val fotosSegundoPanel = if (componente == "Paneles") {
                    listOf(
                        validarArchivo(datos.fotoSegundoComponenteMaloPath, "la fotografía del segundo panel viejo"),
                        validarArchivo(datos.fotoSegundoSerialMaloPath, "la fotografía del segundo serial viejo"),
                        validarArchivo(datos.fotoSegundoComponenteNuevoPath, "la fotografía del segundo panel nuevo"),
                        validarArchivo(datos.fotoSegundoSerialNuevoPath, "la fotografía del segundo serial nuevo")
                    )
                } else listOf(null, null, null, null)
                val firmaUsuario = validarArchivo(datos.firmaUsuarioPath, "la firma de conformidad del usuario")
                val firmaTecnico = validarArchivo(datos.firmaTecnicoPath, "la firma del técnico")
                val contratos = db.tecnicoContratoDao().obtenerIdsContratosDeTecnico(tecnico.id)
                if (sistema.contratoId !in contratos) {
                    throw IllegalArgumentException("El SISFV seleccionado no pertenece a un contrato asignado a este técnico.")
                }
                val actual = db.sistemaSisfvDao().obtenerPorId(sistema.id, sistema.contratoId)
                    ?: throw IllegalArgumentException("El SISFV ya no está disponible en la base local.")
                val cambio = CambioComponenteEntity(
                    id = UUID.randomUUID().toString(), sistemaId = actual.id,
                    codigoSisfv = actual.codigoSisfv, contratoId = actual.contratoId,
                    proyectoId = actual.proyectoId, nombreUsuario = actual.nombreUsuario,
                    documentoUsuario = actual.documentoUsuario, componente = componente,
                    causaFallo = causa, fotoComponenteMaloPath = fotos[0],
                    fotoSerialMaloPath = fotos[1], fotoComponenteNuevoPath = fotos[2],
                    fotoSerialNuevoPath = fotos[3],
                    fotoSegundoComponenteMaloPath = fotosSegundoPanel[0],
                    fotoSegundoSerialMaloPath = fotosSegundoPanel[1],
                    fotoSegundoComponenteNuevoPath = fotosSegundoPanel[2],
                    fotoSegundoSerialNuevoPath = fotosSegundoPanel[3],
                    nombreReceptor = nombre,
                    documentoReceptor = documento, receptorEsTitular = esTitular,
                    firmaUsuarioPath = firmaUsuario, firmaTecnicoPath = firmaTecnico,
                    tecnicoId = tecnico.id, fechaRegistro = System.currentTimeMillis(),
                    estadoRevision = "PENDIENTE_REVISION", estadoSync = "PENDING_SYNC"
                )
                db.withTransaction { db.cambioComponenteDao().insertar(cambio) }
                _uiState.value = CambioComponenteUiState(saveSuccess = true)
            } catch (e: Exception) {
                _uiState.value = CambioComponenteUiState(errorMessage = e.message ?: "No fue posible guardar el cambio localmente.")
            }
        }
    }

    private fun validarArchivo(path: String?, descripcion: String): String {
        val file = path?.let(::File)
        if (file == null || !file.isFile || file.length() <= 0L) {
            throw IllegalArgumentException("Debe registrar $descripcion.")
        }
        return file.absolutePath
    }

    fun limpiarEstado() { _uiState.value = CambioComponenteUiState() }
}
