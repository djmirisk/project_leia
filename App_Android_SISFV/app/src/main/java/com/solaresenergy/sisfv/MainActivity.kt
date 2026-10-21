package com.solaresenergy.sisfv

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import com.solaresenergy.sisfv.ui.home.HomeScreen
import com.solaresenergy.sisfv.ui.login.LoginScreen
import com.solaresenergy.sisfv.ui.login.LoginViewModel
import com.solaresenergy.sisfv.ui.mantenimiento.IdentificarSisfvScreen
import com.solaresenergy.sisfv.ui.theme.SolaresSISFVTheme
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity
import com.solaresenergy.sisfv.ui.mantenimiento.FormularioMantenimientoScreen
import com.solaresenergy.sisfv.ui.mantenimiento.CambioTitularidadScreen


class MainActivity : ComponentActivity() {

    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            SolaresSISFVTheme {

                var tecnicoActual by remember {
                    mutableStateOf<TecnicoEntity?>(null)
                }

                var mostrarIdentificacion by remember {
                    mutableStateOf(false)
                }

                var sistemaSeleccionado by remember {
                    mutableStateOf<SistemaSisfvEntity?>(null)
                }

                var mostrarFormulario by remember {
                    mutableStateOf(false)
                }

                var mostrarBusquedaTitularidad by remember { mutableStateOf(false) }
                var sistemaTitularidadSeleccionado by remember {
                    mutableStateOf<SistemaSisfvEntity?>(null)
                }
                var mostrarFormularioTitularidad by remember { mutableStateOf(false) }

                Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    // ------------------------------------------------
                    // 1. LOGIN
                    // ------------------------------------------------
                    if (tecnicoActual == null) {

                        LoginScreen(
                            viewModel = loginViewModel,

                            onLoginSuccess = { tecnico ->

                                tecnicoActual = tecnico

                                Toast.makeText(
                                    this@MainActivity,
                                    "Bienvenido ${tecnico.nombre}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                    }

                    // ------------------------------------------------
// 2. FORMULARIO DE MANTENIMIENTO
// ------------------------------------------------
                    else if (mostrarFormulario && sistemaSeleccionado != null) {

                        FormularioMantenimientoScreen(

                            tecnico = tecnicoActual!!,

                            sistema = sistemaSeleccionado!!,

                            onVolver = {

                                mostrarFormulario = false
                            },

                            onGuardar = {

                                // Por ahora solo regresamos al dashboard.
                                // Después conectaremos este botón con Room.
                                mostrarFormulario = false
                                sistemaSeleccionado = null
                            }
                        )
                    }

                    else if (mostrarFormularioTitularidad && sistemaTitularidadSeleccionado != null) {
                        CambioTitularidadScreen(
                            tecnico = tecnicoActual!!,
                            sistema = sistemaTitularidadSeleccionado!!,
                            onVolver = { mostrarFormularioTitularidad = false },
                            onGuardar = {
                                mostrarFormularioTitularidad = false
                                sistemaTitularidadSeleccionado = null
                            }
                        )
                    }

                    else if (mostrarBusquedaTitularidad) {
                        IdentificarSisfvScreen(
                            tecnico = tecnicoActual!!,
                            tituloScreen = "Buscar SISFV para cambio de titularidad",
                            onVolver = { mostrarBusquedaTitularidad = false },
                            onSistemaSeleccionado = { sistema ->
                                sistemaTitularidadSeleccionado = sistema
                                mostrarBusquedaTitularidad = false
                                mostrarFormularioTitularidad = true
                            }
                        )
                    }

// ------------------------------------------------
// 3. IDENTIFICAR SISFV
// ------------------------------------------------
                    else if (mostrarIdentificacion) {

                        IdentificarSisfvScreen(

                            tecnico = tecnicoActual!!,

                            onVolver = {
                                mostrarIdentificacion = false
                            },

                            onSistemaSeleccionado = { sistema ->

                                sistemaSeleccionado = sistema

                                mostrarIdentificacion = false

                                mostrarFormulario = true
                            }
                        )
                    }

// ------------------------------------------------
// 4. DASHBOARD
// ------------------------------------------------
                    else {

                        HomeScreen(

                            tecnico = tecnicoActual!!,

                            onNuevaVisitaClick = {

                                mostrarIdentificacion = true
                            },

                            onCambioTitularidadClick = {
                                mostrarBusquedaTitularidad = true
                            },

                            onCerrarSesionClick = {

                                loginViewModel.cerrarSesion()

                                tecnicoActual = null

                                mostrarIdentificacion = false

                                mostrarFormulario = false

                                mostrarBusquedaTitularidad = false
                                mostrarFormularioTitularidad = false

                                sistemaSeleccionado = null
                                sistemaTitularidadSeleccionado = null
                            }
                        )
                    }
                }
            }
        }
    }
}
