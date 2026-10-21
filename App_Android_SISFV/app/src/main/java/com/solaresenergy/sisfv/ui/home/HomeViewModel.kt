package com.solaresenergy.sisfv.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solaresenergy.sisfv.data.local.AppDatabase
import com.solaresenergy.sisfv.data.local.entity.ContratoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val totalMantenimientos: Int = 0,
    val pendientesSync: Int = 0,
    val pendientesTitularidadSync: Int = 0,
    val contratosAsignados: List<ContratoEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)

    private val mantenimientoDao = db.mantenimientoDao()
    private val tecnicoContratoDao = db.tecnicoContratoDao()
    private val contratoDao = db.contratoDao()
    private val solicitudTitularidadDao = db.solicitudCambioTitularidadDao()

    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> =
        _uiState.asStateFlow()

    fun cargarDatos(tecnicoId: Int) {

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                // Indicadores del técnico
                val total =
                    mantenimientoDao.contarPorTecnico(tecnicoId)

                val pendientesMantenimiento =
                    mantenimientoDao.contarPendientesPorTecnico(tecnicoId)

                val pendientesTitularidad =
                    solicitudTitularidadDao.contarPendientesSyncPorTecnico(tecnicoId)

                // Consultar únicamente los contratos asignados
                // al técnico que inició sesión.
                val idsContratos =
                    tecnicoContratoDao
                        .obtenerIdsContratosDeTecnico(tecnicoId)

                val contratos =
                    if (idsContratos.isNotEmpty()) {
                        contratoDao.obtenerContratosAsignados(
                            idsContratos
                        )
                    } else {
                        emptyList()
                    }

                _uiState.value = HomeUiState(
                    totalMantenimientos = total,
                    pendientesSync = pendientesMantenimiento,
                    pendientesTitularidadSync = pendientesTitularidad,
                    contratosAsignados = contratos,
                    isLoading = false
                )

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage =
                        "No se pudieron cargar los datos locales."
                )
            }
        }
    }
}
