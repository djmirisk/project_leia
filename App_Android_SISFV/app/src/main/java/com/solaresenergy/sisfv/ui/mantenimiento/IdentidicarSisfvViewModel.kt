package com.solaresenergy.sisfv.ui.mantenimiento

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solaresenergy.sisfv.data.local.AppDatabase
import com.solaresenergy.sisfv.data.local.entity.ContratoEntity
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IdentificarSisfvUiState(
    val textoBusqueda: String = "",
    val contratos: List<ContratoEntity> = emptyList(),
    val contratoSeleccionado: ContratoEntity? = null,
    val comunidades: List<String> = emptyList(),
    val veredas: List<String> = emptyList(),
    val comunidadSeleccionada: String? = null,
    val veredaSeleccionada: String? = null,
    val resultados: List<SistemaSisfvEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class IdentificarSisfvViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)

    private val tecnicoContratoDao = db.tecnicoContratoDao()
    private val contratoDao = db.contratoDao()
    private val sistemaSisfvDao = db.sistemaSisfvDao()

    private val _uiState = MutableStateFlow(
        IdentificarSisfvUiState()
    )

    val uiState: StateFlow<IdentificarSisfvUiState> =
        _uiState.asStateFlow()


    // =========================================================
    // CARGAR CONTRATOS DEL TÉCNICO
    // =========================================================

    fun cargarContratosDelTecnico(tecnicoId: Int) {

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {

                val contratoIds =
                    tecnicoContratoDao.obtenerIdsContratosDeTecnico(
                        tecnicoId
                    )

                if (contratoIds.isEmpty()) {

                    _uiState.value = _uiState.value.copy(
                        contratos = emptyList(),
                        isLoading = false,
                        errorMessage =
                            "El técnico no tiene contratos asignados."
                    )

                    return@launch
                }

                val contratos =
                    contratoDao.obtenerContratosAsignados(
                        contratoIds
                    )

                _uiState.value = _uiState.value.copy(
                    contratos = contratos,
                    isLoading = false,
                    errorMessage = null
                )

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage =
                        "No fue posible cargar los contratos asignados."
                )
            }
        }
    }


    // =========================================================
    // COMPATIBILIDAD TEMPORAL
    // =========================================================

    fun cargarProyectosDelTecnico(tecnicoId: Int) {
        cargarContratosDelTecnico(tecnicoId)
    }


    // =========================================================
    // SELECCIONAR CONTRATO
    // =========================================================

    fun seleccionarContrato(
        contrato: ContratoEntity
    ) {

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                contratoSeleccionado = contrato,
                comunidadSeleccionada = null,
                veredaSeleccionada = null,
                comunidades = emptyList(),
                veredas = emptyList(),
                resultados = emptyList(),
                textoBusqueda = "",
                isLoading = true,
                errorMessage = null
            )

            try {

                val comunidades =
                    sistemaSisfvDao.obtenerComunidadesPorContrato(
                        contrato.id
                    )

                _uiState.value = _uiState.value.copy(
                    comunidades = comunidades,
                    isLoading = false
                )

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage =
                        "No fue posible cargar las comunidades."
                )
            }
        }
    }


    // =========================================================
    // SELECCIONAR COMUNIDAD
    // =========================================================

    fun seleccionarComunidad(
        comunidad: String?
    ) {

        val contrato =
            _uiState.value.contratoSeleccionado
                ?: return

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                comunidadSeleccionada = comunidad,
                veredaSeleccionada = null,
                veredas = emptyList(),
                resultados = emptyList(),
                errorMessage = null,
                isLoading = true
            )

            try {

                val veredas =
                    sistemaSisfvDao.obtenerVeredasPorContrato(
                        contratoId = contrato.id,
                        comunidad = comunidad
                    )

                _uiState.value = _uiState.value.copy(
                    veredas = veredas,
                    isLoading = false
                )

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage =
                        "No fue posible cargar las veredas."
                )
            }
        }
    }


    // =========================================================
    // SELECCIONAR VEREDA
    // =========================================================

    fun seleccionarVereda(
        vereda: String?
    ) {

        _uiState.value = _uiState.value.copy(
            veredaSeleccionada = vereda,
            resultados = emptyList(),
            errorMessage = null
        )
    }


    // =========================================================
    // ACTUALIZAR TEXTO DE BÚSQUEDA
    // =========================================================

    fun actualizarBusqueda(
        texto: String
    ) {

        _uiState.value = _uiState.value.copy(
            textoBusqueda = texto,
            errorMessage = null,
            resultados = emptyList()
        )
    }


    // =========================================================
    // EJECUTAR BÚSQUEDA
    // =========================================================

    fun ejecutarBusqueda() {

        val estado = _uiState.value

        val contrato =
            estado.contratoSeleccionado

        if (contrato == null) {

            _uiState.value = estado.copy(
                resultados = emptyList(),
                errorMessage =
                    "Seleccione primero un contrato."
            )

            return
        }

        val texto =
            estado.textoBusqueda.trim()

            buscar(
            contratoId = contrato.id,
            texto = texto,
            comunidad = estado.comunidadSeleccionada,
            vereda = estado.veredaSeleccionada
        )
    }


    // =========================================================
    // REALIZAR BÚSQUEDA
    // =========================================================

    private fun buscar(
        contratoId: Int,
        texto: String,
        comunidad: String?,
        vereda: String?
    ) {

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {

                val resultados =
                    sistemaSisfvDao.buscarPorContrato(
                        contratoId = contratoId,
                        texto = texto,
                        comunidad = comunidad,
                        vereda = vereda
                    )

                _uiState.value = _uiState.value.copy(
                    resultados = resultados,
                    isLoading = false,
                    errorMessage = null
                )

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    resultados = emptyList(),
                    isLoading = false,
                    errorMessage =
                        "Error realizando la búsqueda."
                )
            }
        }
    }


    // =========================================================
    // LIMPIAR BÚSQUEDA
    // =========================================================

    fun limpiarBusqueda() {

        _uiState.value = _uiState.value.copy(
            textoBusqueda = "",
            resultados = emptyList(),
            errorMessage = null
        )
    }


    // =========================================================
    // LIMPIAR FILTROS
    // =========================================================

    fun limpiarFiltros() {

        val contrato =
            _uiState.value.contratoSeleccionado

        if (contrato == null) {
            return
        }

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                comunidadSeleccionada = null,
                veredaSeleccionada = null,
                veredas = emptyList(),
                resultados = emptyList(),
                errorMessage = null,
                isLoading = true
            )

            try {

                val comunidades =
                    sistemaSisfvDao.obtenerComunidadesPorContrato(
                        contrato.id
                    )

                _uiState.value = _uiState.value.copy(
                    comunidades = comunidades,
                    isLoading = false
                )

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage =
                        "No fue posible cargar los filtros."
                )
            }
        }
    }
}