package com.solaresenergy.sisfv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import com.solaresenergy.sisfv.ui.theme.SolarAccent
import com.solaresenergy.sisfv.ui.theme.SolarGreen
import com.solaresenergy.sisfv.ui.theme.SolarPrimary
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solaresenergy.sisfv.ui.theme.SolarAccent


@Composable
fun HomeScreen(
    tecnico: TecnicoEntity,
    viewModel: HomeViewModel = viewModel(),
    onNuevaVisitaClick: () -> Unit,
    onCerrarSesionClick: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsState()

    /*
     * Carga los datos del técnico cuando se abre el Dashboard.
     */
    LaunchedEffect(tecnico.id) {
        viewModel.cargarDatos(tecnico.id)
    }

    Scaffold(
        topBar = {
            Surface(
                color = SolarGreen,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column {

                        Text(
                            text = "SOLARES ENERGY GROUP",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "Sistema SISFV ZNI",
                            fontSize = 12.sp,
                            color = SolarAccent
                        )
                    }

                    TextButton(
                        onClick = onCerrarSesionClick
                    ) {
                        Text(
                            text = "SALIR",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Column {

                // =========================================
                // INFORMACIÓN DEL TÉCNICO
                // =========================================

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "Bienvenido,",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )

                        Text(
                            text = tecnico.nombre,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SolarPrimary
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "Rol: ${tecnico.rol} | Cédula: ${tecnico.documento}",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Text(
                            text = "Contratos asignados (${uiState.contratosAsignados.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SolarPrimary
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        if (uiState.contratosAsignados.isEmpty()) {
                            Text(
                                text = "No hay contratos asignados disponibles en el equipo.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        } else {
                            uiState.contratosAsignados.forEach { contrato ->
                                Text(
                                    text = "• ${contrato.codigoContrato} — ${contrato.nombreContrato}",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                // =========================================
                // TÍTULO DE OPERACIONES
                // =========================================

                Text(
                    text = "Operaciones de Campo ML-GT-001",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SolarGreen,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // =========================================
                // NUEVO MANTENIMIENTO
                // =========================================

                Button(
                    onClick = onNuevaVisitaClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SolarGreen
                    )
                ) {

                    Text(
                        text = "⚡ NUEVO MANTENIMIENTO SISFV",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                // =========================================
                // INDICADORES
                // =========================================

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // MIS MANTENIMIENTOS
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "📋",
                                fontSize = 24.sp
                            )

                            Text(
                                text = "Mis mantenimientos",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarGreen
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text = uiState.totalMantenimientos.toString(),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarGreen
                            )
                        }
                    }

                    // PENDIENTES DE SINCRONIZACIÓN
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "🔄",
                                fontSize = 24.sp
                            )

                            Text(
                                text = "Pendientes Sync",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarGreen
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text = uiState.pendientesSync.toString(),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarGreen
                            )
                        }
                    }
                }
            }

            // =========================================
            // ESTADO OFFLINE
            // =========================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = SolarGreen.copy(alpha = 0.06f)
                )
            ) {

                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "📡 ",
                        fontSize = 18.sp
                    )

                    Text(
                        text = "Modo Offline Activo. La información se guardará en el celular y se cargará automáticamente al conectar a la NAS.",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}
