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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import com.solaresenergy.sisfv.ui.home.HomeScreen
import com.solaresenergy.sisfv.ui.login.LoginScreen
import com.solaresenergy.sisfv.ui.login.LoginViewModel
import com.solaresenergy.sisfv.ui.mantenimiento.IdentificarSisfvScreen
import com.solaresenergy.sisfv.ui.theme.SolaresSISFVTheme
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity
import com.solaresenergy.sisfv.data.local.AppDatabase
import com.solaresenergy.sisfv.data.local.SesionTecnico
import com.solaresenergy.sisfv.ui.mantenimiento.FormularioMantenimientoScreen
import com.solaresenergy.sisfv.ui.mantenimiento.CambioTitularidadScreen
import com.solaresenergy.sisfv.ui.galeria.GaleriaEvidenciasScreen
import com.solaresenergy.sisfv.ui.mantenimiento.CambioComponenteScreen
import com.solaresenergy.sisfv.ui.mantenimiento.VisitaNoEfectivaScreen
import com.solaresenergy.sisfv.ui.mantenimiento.ReporteHurtoScreen


class MainActivity : ComponentActivity() {

    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            SolaresSISFVTheme {
                val appContext = LocalContext.current

                var tecnicoActual by remember {
                    mutableStateOf<TecnicoEntity?>(null)
                }
                var tecnicoIdGuardado by rememberSaveable { mutableStateOf<Int?>(null) }
                var restaurandoNavegacion by remember { mutableStateOf(true) }

                var mostrarIdentificacion by rememberSaveable {
                    mutableStateOf(false)
                }

                var sistemaSeleccionado by remember {
                    mutableStateOf<SistemaSisfvEntity?>(null)
                }

                var sistemaSeleccionadoId by rememberSaveable { mutableStateOf<Int?>(null) }
                var sistemaSeleccionadoContratoId by rememberSaveable { mutableStateOf<Int?>(null) }
                var mostrarFormulario by rememberSaveable {
                    mutableStateOf(false)
                }
                var preguntarAdultoResponsable by rememberSaveable { mutableStateOf(false) }
                var mostrarVisitaNoEfectiva by rememberSaveable { mutableStateOf(false) }

                var mostrarBusquedaTitularidad by rememberSaveable { mutableStateOf(false) }
                var sistemaTitularidadSeleccionado by remember {
                    mutableStateOf<SistemaSisfvEntity?>(null)
                }
                var sistemaTitularidadId by rememberSaveable { mutableStateOf<Int?>(null) }
                var sistemaTitularidadContratoId by rememberSaveable { mutableStateOf<Int?>(null) }
                var mostrarFormularioTitularidad by rememberSaveable { mutableStateOf(false) }
                var mostrarGaleriaEvidencias by rememberSaveable { mutableStateOf(false) }
                var mostrarBusquedaHurto by rememberSaveable { mutableStateOf(false) }
                var sistemaHurtoSeleccionado by remember { mutableStateOf<SistemaSisfvEntity?>(null) }
                var sistemaHurtoId by rememberSaveable { mutableStateOf<Int?>(null) }
                var sistemaHurtoContratoId by rememberSaveable { mutableStateOf<Int?>(null) }
                var mostrarFormularioHurto by rememberSaveable { mutableStateOf(false) }
                var mostrarBusquedaCambioComponente by rememberSaveable { mutableStateOf(false) }
                var sistemaCambioComponenteSeleccionado by remember { mutableStateOf<SistemaSisfvEntity?>(null) }
                var sistemaCambioComponenteId by rememberSaveable { mutableStateOf<Int?>(null) }
                var sistemaCambioComponenteContratoId by rememberSaveable { mutableStateOf<Int?>(null) }
                var mostrarFormularioCambioComponente by rememberSaveable { mutableStateOf(false) }
                var correctivoDesdeCambioComponente by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val db = AppDatabase.getDatabase(appContext)

                    // Si el sistema cerró la app en segundo plano, se recupera la sesión
                    // guardada en preferencias para no obligar al técnico a entrar de nuevo.
                    val tecnicoIdSesion = tecnicoIdGuardado ?: SesionTecnico.leer(appContext)
                    if (tecnicoIdSesion != null) {
                        val tecnico = db.tecnicoDao().obtenerPorId(tecnicoIdSesion)
                        if (tecnico != null) {
                            tecnicoActual = tecnico
                            tecnicoIdGuardado = tecnico.id
                        } else {
                            // El técnico ya no existe en la base local: se descarta la sesión.
                            SesionTecnico.limpiar(appContext)
                            tecnicoIdGuardado = null
                        }
                    }

                    sistemaSeleccionadoId?.let { id ->
                        sistemaSeleccionadoContratoId?.let { contratoId ->
                            sistemaSeleccionado = db.sistemaSisfvDao().obtenerPorId(id, contratoId)
                        }
                    }
                    sistemaTitularidadId?.let { id -> sistemaTitularidadContratoId?.let { sistemaTitularidadSeleccionado = db.sistemaSisfvDao().obtenerPorId(id, it) } }
                    sistemaHurtoId?.let { id -> sistemaHurtoContratoId?.let { sistemaHurtoSeleccionado = db.sistemaSisfvDao().obtenerPorId(id, it) } }
                    sistemaCambioComponenteId?.let { id -> sistemaCambioComponenteContratoId?.let { sistemaCambioComponenteSeleccionado = db.sistemaSisfvDao().obtenerPorId(id, it) } }
                    restaurandoNavegacion = false
                }

                Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    // ------------------------------------------------
                    // 1. LOGIN
                    // ------------------------------------------------
                    if (restaurandoNavegacion) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }

                    else if (tecnicoActual == null) {

                        LoginScreen(
                            viewModel = loginViewModel,

                            onLoginSuccess = { tecnico ->

                                tecnicoActual = tecnico
                                tecnicoIdGuardado = tecnico.id
                                SesionTecnico.guardar(appContext, tecnico.id)

                                Toast.makeText(
                                    this@MainActivity,
                                    "Bienvenido ${tecnico.nombre}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                    }

                    else if (preguntarAdultoResponsable && sistemaSeleccionado != null) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { preguntarAdultoResponsable = false; sistemaSeleccionado = null; sistemaSeleccionadoId = null; sistemaSeleccionadoContratoId = null },
                            title = { androidx.compose.material3.Text("Adulto responsable") },
                            text = { androidx.compose.material3.Text("¿Hay un adulto responsable presente en la vivienda?") },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = {
                                    preguntarAdultoResponsable = false
                                    mostrarFormulario = true
                                }) { androidx.compose.material3.Text("SÍ, CONTINUAR") }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(onClick = {
                                    preguntarAdultoResponsable = false
                                    mostrarVisitaNoEfectiva = true
                                }) { androidx.compose.material3.Text("NO, REGISTRAR VISITA") }
                            }
                        )
                    }

                    else if (mostrarVisitaNoEfectiva && sistemaSeleccionado != null) {
                        VisitaNoEfectivaScreen(
                            tecnico = tecnicoActual!!,
                            sistema = sistemaSeleccionado!!,
                            onVolver = { mostrarVisitaNoEfectiva = false; sistemaSeleccionado = null; sistemaSeleccionadoId = null; sistemaSeleccionadoContratoId = null },
                            onGuardar = { mostrarVisitaNoEfectiva = false; sistemaSeleccionado = null; sistemaSeleccionadoId = null; sistemaSeleccionadoContratoId = null }
                        )
                    }

                    else if (mostrarFormularioHurto && sistemaHurtoSeleccionado != null) {
                        ReporteHurtoScreen(
                            tecnico = tecnicoActual!!,
                            sistema = sistemaHurtoSeleccionado!!,
                            onVolver = { mostrarFormularioHurto = false; sistemaHurtoSeleccionado = null; sistemaHurtoId = null; sistemaHurtoContratoId = null },
                            onGuardar = { mostrarFormularioHurto = false; sistemaHurtoSeleccionado = null; sistemaHurtoId = null; sistemaHurtoContratoId = null }
                        )
                    }

                    else if (mostrarBusquedaHurto) {
                        IdentificarSisfvScreen(
                            tecnico = tecnicoActual!!,
                            tituloScreen = "Búsqueda SISFV a reportar",
                            onVolver = { mostrarBusquedaHurto = false },
                            onSistemaSeleccionado = { sistema ->
                                sistemaHurtoSeleccionado = sistema
                                sistemaHurtoId = sistema.id
                                sistemaHurtoContratoId = sistema.contratoId
                                mostrarBusquedaHurto = false
                                mostrarFormularioHurto = true
                            }
                        )
                    }

                    // ------------------------------------------------
// 2. FORMULARIO DE MANTENIMIENTO
// ------------------------------------------------
                    else if (correctivoDesdeCambioComponente && sistemaSeleccionado != null) {
                        FormularioMantenimientoScreen(
                            tecnico = tecnicoActual!!,
                            sistema = sistemaSeleccionado!!,
                            onVolver = {
                                correctivoDesdeCambioComponente = false
                                sistemaSeleccionado = null
                                sistemaSeleccionadoId = null
                                sistemaSeleccionadoContratoId = null
                            },
                            onGuardar = {
                                correctivoDesdeCambioComponente = false
                                sistemaSeleccionado = null
                                sistemaSeleccionadoId = null
                                sistemaSeleccionadoContratoId = null
                            },
                            correctivoObligatorio = true
                        )
                    }

                    else if (mostrarFormulario && sistemaSeleccionado != null) {

                        FormularioMantenimientoScreen(

                            tecnico = tecnicoActual!!,

                            sistema = sistemaSeleccionado!!,

                            onVolver = {

                                mostrarFormulario = false
                                sistemaSeleccionado = null
                                sistemaSeleccionadoId = null
                                sistemaSeleccionadoContratoId = null
                            },

                            onGuardar = {

                                // Por ahora solo regresamos al dashboard.
                                // Después conectaremos este botón con Room.
                                mostrarFormulario = false
                                sistemaSeleccionado = null
                                sistemaSeleccionadoId = null
                                sistemaSeleccionadoContratoId = null
                            }
                        )
                    }

                    else if (mostrarFormularioCambioComponente && sistemaCambioComponenteSeleccionado != null) {
                        CambioComponenteScreen(
                            tecnico = tecnicoActual!!,
                            sistema = sistemaCambioComponenteSeleccionado!!,
                            onVolver = { mostrarFormularioCambioComponente = false; sistemaCambioComponenteSeleccionado = null; sistemaCambioComponenteId = null; sistemaCambioComponenteContratoId = null },
                            onContinuarMantenimiento = { sistema ->
                                sistemaCambioComponenteSeleccionado = null
                                sistemaCambioComponenteId = null
                                sistemaCambioComponenteContratoId = null
                                mostrarFormularioCambioComponente = false
                                sistemaSeleccionado = sistema
                                sistemaSeleccionadoId = sistema.id
                                sistemaSeleccionadoContratoId = sistema.contratoId
                                correctivoDesdeCambioComponente = true
                            }
                        )
                    }

                    else if (mostrarFormularioTitularidad && sistemaTitularidadSeleccionado != null) {
                        CambioTitularidadScreen(
                            tecnico = tecnicoActual!!,
                            sistema = sistemaTitularidadSeleccionado!!,
                            onVolver = { mostrarFormularioTitularidad = false; sistemaTitularidadSeleccionado = null; sistemaTitularidadId = null; sistemaTitularidadContratoId = null },
                            onGuardar = {
                                mostrarFormularioTitularidad = false
                                sistemaTitularidadSeleccionado = null
                                sistemaTitularidadId = null
                                sistemaTitularidadContratoId = null
                            }
                        )
                    }

                    else if (mostrarGaleriaEvidencias) {
                        GaleriaEvidenciasScreen(
                            tecnico = tecnicoActual!!,
                            onVolver = { mostrarGaleriaEvidencias = false }
                        )
                    }

                    else if (mostrarBusquedaTitularidad) {
                        IdentificarSisfvScreen(
                            tecnico = tecnicoActual!!,
                            tituloScreen = "Buscar SISFV para cambio de titularidad",
                            onVolver = { mostrarBusquedaTitularidad = false },
                            onSistemaSeleccionado = { sistema ->
                                sistemaTitularidadSeleccionado = sistema
                                sistemaTitularidadId = sistema.id
                                sistemaTitularidadContratoId = sistema.contratoId
                                mostrarBusquedaTitularidad = false
                                mostrarFormularioTitularidad = true
                            }
                        )
                    }

                    else if (mostrarBusquedaCambioComponente) {
                        IdentificarSisfvScreen(
                            tecnico = tecnicoActual!!,
                            tituloScreen = "Buscar SISFV para cambio de componente",
                            onVolver = { mostrarBusquedaCambioComponente = false },
                            onSistemaSeleccionado = { sistema ->
                                sistemaCambioComponenteSeleccionado = sistema
                                sistemaCambioComponenteId = sistema.id
                                sistemaCambioComponenteContratoId = sistema.contratoId
                                mostrarBusquedaCambioComponente = false
                                mostrarFormularioCambioComponente = true
                            }
                        )
                    }

// ------------------------------------------------
// 3. IDENTIFICAR SISFV
// ------------------------------------------------
                    else if (mostrarIdentificacion) {

                        IdentificarSisfvScreen(

                            tecnico = tecnicoActual!!,
                            resaltarMantenimientoEfectivo = true,

                            onVolver = {
                                mostrarIdentificacion = false
                            },

                            onSistemaSeleccionado = { sistema ->

                                sistemaSeleccionado = sistema
                                sistemaSeleccionadoId = sistema.id
                                sistemaSeleccionadoContratoId = sistema.contratoId
                                sistemaSeleccionadoId = sistema.id
                                sistemaSeleccionadoContratoId = sistema.contratoId

                                mostrarIdentificacion = false

                                preguntarAdultoResponsable = true
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

                            onCambioComponenteClick = { mostrarBusquedaCambioComponente = true },

                            onReporteHurtoClick = { mostrarBusquedaHurto = true },

                            onGaleriaClick = { mostrarGaleriaEvidencias = true },

                            onCerrarSesionClick = {

                                loginViewModel.cerrarSesion()
                                SesionTecnico.limpiar(appContext)

                                tecnicoActual = null
                                tecnicoIdGuardado = null

                                mostrarIdentificacion = false

                                mostrarFormulario = false

                                mostrarBusquedaTitularidad = false
                                mostrarFormularioTitularidad = false
                                mostrarGaleriaEvidencias = false
                                mostrarBusquedaHurto = false
                                mostrarFormularioHurto = false
                                mostrarBusquedaCambioComponente = false
                                mostrarFormularioCambioComponente = false
                                correctivoDesdeCambioComponente = false

                                sistemaSeleccionado = null
                                sistemaTitularidadSeleccionado = null
                                sistemaCambioComponenteSeleccionado = null
                                sistemaHurtoSeleccionado = null
                                sistemaHurtoId = null
                                sistemaHurtoContratoId = null
                                sistemaSeleccionadoId = null
                                sistemaSeleccionadoContratoId = null
                                sistemaTitularidadId = null
                                sistemaTitularidadContratoId = null
                                sistemaCambioComponenteId = null
                                sistemaCambioComponenteContratoId = null
                            }
                        )
                    }
                }
            }
        }
    }
}
