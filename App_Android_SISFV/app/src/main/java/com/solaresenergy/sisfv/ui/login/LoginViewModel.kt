package com.solaresenergy.sisfv.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solaresenergy.sisfv.data.local.AppDatabase
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val documento: String = "",
    val passwordPin: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val tecnicoLogueado: TecnicoEntity? = null,
    val errorMessage: String? = null,
    val infoMessage: String = "Modo Offline Activo (Base de Datos Local)"
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val tecnicoDao = db.tecnicoDao()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onDocumentoChanged(nuevoDocumento: String) {
        _uiState.update { it.copy(documento = nuevoDocumento, errorMessage = null) }
    }

    fun onPasswordPinChanged(nuevoPassword: String) {
        _uiState.update { it.copy(passwordPin = nuevoPassword, errorMessage = null) }
    }

    /**
     * Iniciar Sesión buscando en la Base de Datos Local de Técnicos (Offline-First)
     */
    fun iniciarSesion() {
        val doc = _uiState.value.documento.trim()
        val pass = _uiState.value.passwordPin.trim()

        if (doc.isEmpty() || pass.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Por favor ingrese cédula y clave/PIN.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val tecnico = tecnicoDao.obtenerPorDocumento(doc)
            if (tecnico == null) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "Técnico no encontrado en la base de datos local. Verifique la cédula."
                    ) 
                }
            } else if (tecnico.passwordHash != pass) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "Contraseña o PIN incorrecto."
                    ) 
                }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        tecnicoLogueado = tecnico,
                        errorMessage = null
                    ) 
                }
            }
        }
    }

    /**
     * Sincronización Manual o Inicial de la Lista de Técnicos (Suministrada por Servidor o JSON)
     */
    fun sincronizarBaseDatosTecnicos(listaNuevosTecnicos: List<TecnicoEntity>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            tecnicoDao.insertarOActualizarLista(listaNuevosTecnicos)
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    infoMessage = "Base de datos de técnicos actualizada (${listaNuevosTecnicos.size} registros)."
                ) 
            }
        }
    }

    /**
     * Cerrar Sesión y reiniciar estado de Login para permitir nuevo ingreso
     */
    fun cerrarSesion() {
        _uiState.update {
            LoginUiState()
        }
    }
}
