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
    val mantenimientosSincronizados: Int = 0,
    val titularidadesSincronizadas: Int = 0,
    val componentesSincronizados: Int = 0,
    val totalCambiosTitularidad: Int = 0,
    val totalCambiosComponente: Int = 0,
    val totalReportesHurto: Int = 0,
    val reportesHurtoSincronizados: Int = 0,
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
    private val cambioComponenteDao = db.cambioComponenteDao()

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

                val mantenimientosSincronizados = mantenimientoDao.contarSincronizadosPorTecnico(tecnicoId)

                val totalTitularidades = solicitudTitularidadDao.contarPorTecnico(tecnicoId)
                val titularidadesSincronizadas = solicitudTitularidadDao.contarSincronizadosPorTecnico(tecnicoId)

                val totalCambiosComponente = cambioComponenteDao.contarPorTecnico(tecnicoId)
                val componentesSincronizados = cambioComponenteDao.contarSincronizadosPorTecnico(tecnicoId)

                val totalReportesHurto = mantenimientoDao.contarReportesHurtoPorTecnico(tecnicoId)
                val reportesHurtoSincronizados = mantenimientoDao.contarReportesHurtoSincronizadosPorTecnico(tecnicoId)

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
                    mantenimientosSincronizados = mantenimientosSincronizados,
                    titularidadesSincronizadas = titularidadesSincronizadas,
                    componentesSincronizados = componentesSincronizados,
                    totalCambiosTitularidad = totalTitularidades,
                    totalCambiosComponente = totalCambiosComponente,
                    totalReportesHurto = totalReportesHurto,
                    reportesHurtoSincronizados = reportesHurtoSincronizados,
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
