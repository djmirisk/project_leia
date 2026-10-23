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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import com.solaresenergy.sisfv.ui.theme.SolarGreen
import com.solaresenergy.sisfv.ui.theme.SolarPrimary
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun HomeScreen(
    tecnico: TecnicoEntity,
    viewModel: HomeViewModel = viewModel(),
    onNuevaVisitaClick: () -> Unit,
    onCambioTitularidadClick: () -> Unit,
    onCambioComponenteClick: () -> Unit,
    onReporteHurtoClick: () -> Unit,
    onGaleriaClick: () -> Unit,
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
                color = MaterialTheme.colorScheme.surface,
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
                            color = SolarGreen
                        )

                        Text(
                            text = "Sistema SISFV ZNI",
                            fontSize = 12.sp,
                            color = SolarPrimary
                        )
                    }

                    Button(
                        onClick = onCerrarSesionClick,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SolarGreen)
                    ) {
                        Text(
                            text = "SALIR",
                            fontSize = 11.sp,
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNuevaVisitaClick,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SolarGreen)
                    ) {
                        Text("NUEVO MANTENIMIENTO", fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 13.sp)
                    }
                    Button(
                        onClick = onCambioTitularidadClick,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SolarGreen)
                    ) {
                        Text("CAMBIO DE TITULARIDAD", fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCambioComponenteClick,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SolarGreen)
                    ) {
                        Text("CAMBIO DE COMPONENTE", fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 13.sp)
                    }
                    Button(
                        onClick = onGaleriaClick,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SolarGreen)
                    ) {
                        Text("CONSULTAR FOTOGRAFÍAS", fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onReporteHurtoClick,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SolarGreen)
                    ) {
                        Text("REPORTE DE HURTO", fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 13.sp)
                    }
                    Card(
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mantenimientos realizados", fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold, color = SolarPrimary, modifier = Modifier.weight(1f))
                            Text(uiState.totalMantenimientos.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SolarGreen, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                // =========================================
                // Registros realizados y estado local de sincronización.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Sincronización", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SolarPrimary)
                        EstadoSincronizacionCard("Mantenimientos", uiState.totalMantenimientos, uiState.mantenimientosSincronizados)
                        EstadoSincronizacionCard("Cambios de titularidad", uiState.totalCambiosTitularidad, uiState.titularidadesSincronizadas)
                        EstadoSincronizacionCard("Cambios de componentes", uiState.totalCambiosComponente, uiState.componentesSincronizados)
                    }
                }
            }
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

@Composable
private fun EstadoSincronizacionCard(
    titulo: String,
    total: Int,
    sincronizados: Int
) {
    val fondo = when {
        total == 0 -> Color(0xFFF5F5F5)
        sincronizados < total -> Color(0xFFFFEBEE)
        else -> Color(0xFFE8F5E9)
    }
    val sombra = when {
        total == 0 -> Color(0x22333333)
        sincronizados < total -> Color(0x66EF5350)
        else -> Color(0x664CAF50)
    }
    val colorTexto = when {
        total == 0 -> Color.DarkGray
        sincronizados < total -> Color(0xFFC62828)
        else -> SolarGreen
    }
    val estado = when {
        total == 0 -> "Sin registros"
        sincronizados < total -> "${total - sincronizados} sin sincronizar"
        else -> "Sincronizado"
    }
    val forma = RoundedCornerShape(10.dp)

    Card(
        modifier = Modifier.fillMaxWidth().shadow(
            elevation = 5.dp,
            shape = forma,
            ambientColor = sombra,
            spotColor = sombra
        ),
        shape = forma,
        colors = CardDefaults.cardColors(containerColor = fondo),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(titulo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
            Text(estado, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorTexto)
        }
    }
}
