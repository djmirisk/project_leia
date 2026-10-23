package com.solaresenergy.sisfv.ui.mantenimiento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import com.solaresenergy.sisfv.ui.theme.SolarGreen
import com.solaresenergy.sisfv.ui.theme.SolarPrimary

@Composable
fun IdentificarSisfvScreen(
    tecnico: TecnicoEntity,
    onVolver: () -> Unit,
    onSistemaSeleccionado: (SistemaSisfvEntity) -> Unit,
    tituloScreen: String = "Identificar SISFV",
    resaltarMantenimientoEfectivo: Boolean = false
) {
    val viewModel: IdentificarSisfvViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    BackHandler(onBack = onVolver)

    var mostrarContratos by remember { mutableStateOf(false) }
    var mostrarComunidades by remember { mutableStateOf(false) }
    var mostrarVeredas by remember { mutableStateOf(false) }

    LaunchedEffect(tecnico.id) {
        viewModel.cargarContratosDelTecnico(tecnico.id)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tituloScreen,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SolarGreen
                )

                TextButton(
                    onClick = onVolver
                ) {
                    Text("VOLVER")
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // =====================================================
            // CONTRATO
            // =====================================================

            Text(
                text = "1. Seleccionar contrato",
                fontWeight = FontWeight.Bold,
                color = SolarGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Seleccione el contrato donde realizará la visita.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {

                Button(
                    onClick = {
                        mostrarContratos = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading &&
                            uiState.contratos.isNotEmpty()
                ) {
                    Text(
                        text = uiState.contratoSeleccionado?.let {
                            "${it.codigoContrato} - ${it.nombreContrato}"
                        } ?: "SELECCIONAR CONTRATO"
                    )
                }

                DropdownMenu(
                    expanded = mostrarContratos,
                    onDismissRequest = {
                        mostrarContratos = false
                    }
                ) {

                    uiState.contratos.forEach { contrato ->

                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${contrato.codigoContrato} - ${contrato.nombreContrato}"
                                )
                            },
                            onClick = {

                                mostrarContratos = false
                                mostrarComunidades = false
                                mostrarVeredas = false

                                viewModel.seleccionarContrato(
                                    contrato
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =====================================================
            // FILTROS
            // =====================================================

            if (uiState.contratoSeleccionado != null) {

                Text(
                    text = "2. Filtros opcionales",
                    fontWeight = FontWeight.Bold,
                    color = SolarGreen
                )

                Spacer(modifier = Modifier.height(10.dp))

                // -------------------------------------------------
                // COMUNIDAD
                // -------------------------------------------------

                Text(
                    text = "Comunidad",
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Button(
                        onClick = {
                            mostrarComunidades = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            uiState.comunidadSeleccionada
                                ?: "Todas las comunidades"
                        )
                    }

                    DropdownMenu(
                        expanded = mostrarComunidades,
                        onDismissRequest = {
                            mostrarComunidades = false
                        }
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text("Todas las comunidades")
                            },
                            onClick = {

                                mostrarComunidades = false

                                viewModel.seleccionarComunidad(
                                    null
                                )
                            }
                        )

                        uiState.comunidades.forEach { comunidad ->

                            DropdownMenuItem(
                                text = {
                                    Text(comunidad)
                                },
                                onClick = {

                                    mostrarComunidades = false

                                    viewModel.seleccionarComunidad(
                                        comunidad
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // -------------------------------------------------
                // VEREDA
                // -------------------------------------------------

                Text(
                    text = "Vereda",
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Button(
                        onClick = {
                            mostrarVeredas = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.comunidadSeleccionada != null &&
                                uiState.veredas.isNotEmpty()
                    ) {
                        Text(
                            uiState.veredaSeleccionada
                                ?: if (uiState.comunidadSeleccionada == null) {
                                    "Seleccione una comunidad primero"
                                } else {
                                    "Todas las veredas"
                                }
                        )
                    }

                    DropdownMenu(
                        expanded = mostrarVeredas,
                        onDismissRequest = {
                            mostrarVeredas = false
                        }
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text("Todas las veredas")
                            },
                            onClick = {

                                mostrarVeredas = false

                                viewModel.seleccionarVereda(
                                    null
                                )
                            }
                        )

                        uiState.veredas.forEach { vereda ->

                            DropdownMenuItem(
                                text = {
                                    Text(vereda)
                                },
                                onClick = {

                                    mostrarVeredas = false

                                    viewModel.seleccionarVereda(
                                        vereda
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // =================================================
                // BÚSQUEDA
                // =================================================

                Text(
                    text = "3. Buscar SISFV",
                    fontWeight = FontWeight.Bold,
                    color = SolarGreen
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Puede buscar por código SISFV, documento, NUI, nombre o ID.",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.textoBusqueda,
                    onValueChange = {
                        viewModel.actualizarBusqueda(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Dato de búsqueda")
                    },
                    placeholder = {
                        Text("Ej. SISFV-001, Juan, NUI...")
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        viewModel.ejecutarBusqueda()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("BUSCAR SISFV")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // =====================================================
            // ESTADOS Y RESULTADOS
            // =====================================================

            if (uiState.isLoading) {

                CircularProgressIndicator()

            } else if (uiState.errorMessage != null) {

                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error
                )

            } else if (
                uiState.contratoSeleccionado != null &&
                uiState.textoBusqueda.isNotBlank() &&
                uiState.resultados.isEmpty()
            ) {

                Text(
                    text = "No se encontraron SISFV con los criterios seleccionados."
                )

            } else if (uiState.resultados.isNotEmpty()) {

                Text(
                    text = "Resultados encontrados: ${uiState.resultados.size}",
                    fontWeight = FontWeight.Bold,
                    color = SolarGreen
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.resultados.forEach { sistema ->
                        SistemaSisfvCard(
                            sistema = sistema,
                            tieneMantenimientoEfectivo = if (resaltarMantenimientoEfectivo) {
                                sistema.id in uiState.resultadosEfectivos
                            } else null,
                            onClick = { onSistemaSeleccionado(sistema) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SistemaSisfvCard(
    sistema: SistemaSisfvEntity,
    tieneMantenimientoEfectivo: Boolean?,
    onClick: () -> Unit
) {

    val forma = RoundedCornerShape(12.dp)
    val sombra = when (tieneMantenimientoEfectivo) {
        true -> Color(0x664CAF50)
        false -> Color(0x66EF5350)
        null -> Color.Transparent
    }
    val fondo = when (tieneMantenimientoEfectivo) {
        true -> Color(0xFFE8F5E9)
        false -> Color(0xFFFFEBEE)
        null -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth().then(
            if (tieneMantenimientoEfectivo != null) {
                Modifier.shadow(5.dp, forma, ambientColor = sombra, spotColor = sombra)
            } else Modifier
        ),
        onClick = onClick,
        shape = forma,
        colors = CardDefaults.cardColors(containerColor = fondo),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = sistema.codigoSisfv,
                fontWeight = FontWeight.Bold,
                color = SolarGreen
            )

            if (tieneMantenimientoEfectivo != null) {
                Text(
                    text = if (tieneMantenimientoEfectivo) "Mantenimiento efectivo registrado" else "Sin mantenimiento efectivo registrado",
                    color = if (tieneMantenimientoEfectivo) SolarGreen else Color(0xFFC62828),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = sistema.nombreUsuario,
                fontWeight = FontWeight.Bold
            )

            sistema.documentoUsuario?.let {
                Text("Documento: $it")
            }

            sistema.nui?.let {
                Text("NUI: $it")
            }

            sistema.comunidad?.let {
                Text("Comunidad: $it")
            }

            sistema.vereda?.let {
                Text("Vereda: $it")
            }

            Text(
                text = "${sistema.municipio ?: ""} - ${sistema.departamento ?: ""}"
            )
        }
    }
}
