package com.solaresenergy.sisfv.ui.galeria

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solaresenergy.sisfv.data.local.AppDatabase
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity
import com.solaresenergy.sisfv.ui.mantenimiento.codigosFotosHurto
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
    val categoria: String,
    val sistemaId: Int,
    val codigoSisfv: String,
    val nombreUsuario: String,
    val titulo: String,
    val detalle: String,
    val fecha: Long,
    val fotos: List<FotoGaleria>
)

data class GaleriaUiState(
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

                val grupos = sistemas.flatMap { sistema -> cargarGruposFotosSistema(sistema, tecnicoId) }
                    .sortedWith(
                        compareBy<GrupoFotosGaleria> { ordenCategoria(it.categoria) }
                            .thenByDescending { it.fecha }
                    )
                _uiState.value = GaleriaUiState(grupos = grupos)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "No se pudieron consultar las fotografías guardadas."
                )
            }
        }
    }

    private suspend fun cargarGruposFotosSistema(
        sistema: SistemaSisfvEntity,
        tecnicoId: Int
    ): List<GrupoFotosGaleria> {
        val grupos = mutableListOf<GrupoFotosGaleria>()
        val referenciaSistema = "${sistema.codigoSisfv} · ${sistema.nombreUsuario}"

        val mantenimientos = db.mantenimientoDao().listarParaGaleria(sistema.codigoSisfv, tecnicoId)
        mantenimientos.forEach { mantenimiento ->
            val esHurto = mantenimiento.tipoMantenimiento == "REPORTE_HURTO"
            val fotosRegistradas = db.mantenimientoFotoDao().listarPorMantenimiento(mantenimiento.id)
            // El hurto guarda las fotos en el orden en que se tomaron; se muestran en el orden del formulario.
            val fotosOrdenadas = if (esHurto) {
                fotosRegistradas.sortedBy { foto ->
                    codigosFotosHurto.indexOf(foto.codigoFoto).takeIf { it >= 0 } ?: Int.MAX_VALUE
                }
            } else {
                fotosRegistradas
            }
            val fotos = fotosOrdenadas
                .mapNotNull { foto ->
                    val ruta = foto.rutaConMarcaAgua?.takeIf { it.isNotBlank() } ?: foto.rutaOriginal
                    if (!File(ruta).isFile) return@mapNotNull null
                    FotoGaleria(foto.titulo, ruta, foto.fechaCaptura, true)
                }
            if (fotos.isNotEmpty()) {
                val categoria = if (esHurto) "Reportes de hurto" else "Mantenimientos"
                val titulo = when (mantenimiento.tipoMantenimiento) {
                    "REPORTE_HURTO" -> "Reporte de hurto"
                    "VISITA_NO_EFECTIVA" -> "Mantenimiento no efectivo"
                    else -> "Mantenimiento ${mantenimiento.tipoMantenimiento}"
                }
                val reportante = mantenimiento.nombreReceptorFirma?.takeIf { esHurto && it.isNotBlank() }
                grupos += GrupoFotosGaleria(
                    categoria = categoria,
                    sistemaId = sistema.id,
                    codigoSisfv = sistema.codigoSisfv,
                    nombreUsuario = sistema.nombreUsuario,
                    titulo = titulo,
                    detalle = listOfNotNull(
                        referenciaSistema,
                        fechaLegible(mantenimiento.fechaVisita),
                        reportante?.let { "Reportado por $it" }
                    ).joinToString(" · "),
                    fecha = mantenimiento.fechaVisita,
                    fotos = fotos
                )
            }
        }

        db.solicitudCambioTitularidadDao().listarParaGaleria(sistema.id, tecnicoId).forEach { solicitud ->
            val fotos = listOf(
                "Documento · frente" to solicitud.fotoDocumentoFrentePath,
                "Documento · reverso" to solicitud.fotoDocumentoReversoPath,
                "Huella dactilar" to solicitud.fotoHuellaPath
            ).mapNotNull { (titulo, ruta) ->
                if (!File(ruta).isFile) return@mapNotNull null
                FotoGaleria(titulo, ruta, solicitud.fechaRegistro, true)
            }
            if (fotos.isNotEmpty()) {
                grupos += GrupoFotosGaleria(
                    categoria = "Cambios de titularidad",
                    sistemaId = sistema.id,
                    codigoSisfv = sistema.codigoSisfv,
                    nombreUsuario = sistema.nombreUsuario,
                    titulo = "Solicitud de titularidad",
                    detalle = "$referenciaSistema · ${fechaLegible(solicitud.fechaRegistro)} · ${solicitud.estadoRevision}",
                    fecha = solicitud.fechaRegistro,
                    fotos = fotos
                )
            }
        }

        db.cambioComponenteDao().listarParaGaleria(sistema.id, tecnicoId).forEach { cambio ->
            val esPaneles = cambio.componente == "Paneles"
            val fotos = listOfNotNull(
                (if (esPaneles) "Panel viejo 1" else "Componente malo") to cambio.fotoComponenteMaloPath,
                (if (esPaneles) "Serial del panel viejo 1" else "Serial del componente malo") to cambio.fotoSerialMaloPath,
                (if (esPaneles) "Panel nuevo 1" else "Componente nuevo") to cambio.fotoComponenteNuevoPath,
                (if (esPaneles) "Serial del panel nuevo 1" else "Serial del componente nuevo") to cambio.fotoSerialNuevoPath,
                cambio.fotoSegundoComponenteMaloPath?.let { (if (esPaneles) "Panel viejo 2" else "Segundo componente malo") to it },
                cambio.fotoSegundoSerialMaloPath?.let { (if (esPaneles) "Serial del panel viejo 2" else "Segundo serial malo") to it },
                cambio.fotoSegundoComponenteNuevoPath?.let { (if (esPaneles) "Panel nuevo 2" else "Segundo componente nuevo") to it },
                cambio.fotoSegundoSerialNuevoPath?.let { (if (esPaneles) "Serial del panel nuevo 2" else "Segundo serial nuevo") to it }
            ).mapNotNull { (titulo, ruta) ->
                if (!File(ruta).isFile) return@mapNotNull null
                FotoGaleria(titulo, ruta, cambio.fechaRegistro, true)
            }
            if (fotos.isNotEmpty()) {
                grupos += GrupoFotosGaleria(
                    categoria = "Cambios de componentes",
                    sistemaId = sistema.id,
                    codigoSisfv = sistema.codigoSisfv,
                    nombreUsuario = sistema.nombreUsuario,
                    titulo = "Cambio de ${cambio.componente}",
                    detalle = "$referenciaSistema · ${fechaLegible(cambio.fechaRegistro)} · ${cambio.estadoRevision} · ${cambio.causaFallo}",
                    fecha = cambio.fechaRegistro,
                    fotos = fotos
                )
            }
        }
        return grupos
    }

    private fun ordenCategoria(categoria: String): Int = when (categoria) {
        "Cambios de titularidad" -> 0
        "Cambios de componentes" -> 1
        "Reportes de hurto" -> 3
        else -> 2
    }

    private fun fechaLegible(fecha: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(fecha))
}
