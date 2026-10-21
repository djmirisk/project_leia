package com.solaresenergy.sisfv.ui.galeria

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solaresenergy.sisfv.data.local.AppDatabase
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FotoGaleria(
    val titulo: String,
    val ruta: String,
    val fecha: Long,
    val disponible: Boolean
)

data class GrupoFotosGaleria(
    val titulo: String,
    val detalle: String,
    val fecha: Long,
    val fotos: List<FotoGaleria>
)

data class GaleriaUiState(
    val sistemas: List<SistemaSisfvEntity> = emptyList(),
    val sistemaSeleccionado: SistemaSisfvEntity? = null,
    val grupos: List<GrupoFotosGaleria> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class GaleriaEvidenciasViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val _uiState = MutableStateFlow(GaleriaUiState())
    val uiState: StateFlow<GaleriaUiState> = _uiState.asStateFlow()

    fun cargarSistemasAsignados(tecnicoId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val contratosIds = db.tecnicoContratoDao().obtenerIdsContratosDeTecnico(tecnicoId)
                val contratos = if (contratosIds.isEmpty()) emptyList() else
                    db.contratoDao().obtenerContratosAsignados(contratosIds)
                val sistemas = contratos.flatMap { contrato ->
                    db.sistemaSisfvDao().obtenerPorContrato(contrato.id)
                }.distinctBy { it.id to it.contratoId }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nui ?: it.codigoSisfv })

                _uiState.value = GaleriaUiState(sistemas = sistemas)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "No se pudieron consultar los SISFV asignados."
                )
            }
        }
    }

    fun seleccionarSistema(sistema: SistemaSisfvEntity, tecnicoId: Int) {
        viewModelScope.launch {
            val perteneceAlTecnico = _uiState.value.sistemas.any {
                it.id == sistema.id && it.contratoId == sistema.contratoId
            }
            if (!perteneceAlTecnico) {
                _uiState.value = _uiState.value.copy(errorMessage = "El SISFV no pertenece a un contrato asignado a este técnico.")
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                sistemaSeleccionado = sistema,
                grupos = emptyList(),
                isLoading = true,
                errorMessage = null
            )
            try {
                val grupos = mutableListOf<GrupoFotosGaleria>()
                val mantenimientos = db.mantenimientoDao().listarParaGaleria(sistema.codigoSisfv, tecnicoId)
                mantenimientos.forEach { mantenimiento ->
                    val fotos = db.mantenimientoFotoDao()
                        .listarPorMantenimiento(mantenimiento.id)
                        .map { foto ->
                            val ruta = foto.rutaConMarcaAgua?.takeIf { it.isNotBlank() } ?: foto.rutaOriginal
                            FotoGaleria(
                                titulo = foto.titulo,
                                ruta = ruta,
                                fecha = foto.fechaCaptura,
                                disponible = File(ruta).isFile
                            )
                        }
                    if (fotos.isNotEmpty()) {
                        grupos += GrupoFotosGaleria(
                            titulo = "Mantenimiento ${mantenimiento.tipoMantenimiento}",
                            detalle = "Registrado el ${fechaLegible(mantenimiento.fechaVisita)}",
                            fecha = mantenimiento.fechaVisita,
                            fotos = fotos
                        )
                    }
                }

                db.solicitudCambioTitularidadDao()
                    .listarParaGaleria(sistema.id, tecnicoId)
                    .forEach { solicitud ->
                        val fotos = listOf(
                            "Documento · frente" to solicitud.fotoDocumentoFrentePath,
                            "Documento · reverso" to solicitud.fotoDocumentoReversoPath,
                            "Huella dactilar" to solicitud.fotoHuellaPath
                        ).map { (titulo, ruta) ->
                            FotoGaleria(titulo, ruta, solicitud.fechaRegistro, File(ruta).isFile)
                        }
                        grupos += GrupoFotosGaleria(
                            titulo = "Solicitud de titularidad",
                            detalle = "${fechaLegible(solicitud.fechaRegistro)} · Nuevo usuario: ${solicitud.nombreUsuarioNuevo} · ${solicitud.estadoRevision}",
                            fecha = solicitud.fechaRegistro,
                            fotos = fotos
                        )
                    }

                _uiState.value = _uiState.value.copy(
                    grupos = grupos.sortedByDescending { it.fecha },
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "No se pudieron cargar las fotografías locales."
                )
            }
        }
    }

    fun volverALista() {
        _uiState.value = _uiState.value.copy(sistemaSeleccionado = null, grupos = emptyList(), errorMessage = null)
    }

    private fun fechaLegible(fecha: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(fecha))
}
