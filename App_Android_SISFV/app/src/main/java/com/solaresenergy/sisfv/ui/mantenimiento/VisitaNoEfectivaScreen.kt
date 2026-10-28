package com.solaresenergy.sisfv.ui.mantenimiento

import androidx.activity.compose.BackHandler
import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import com.solaresenergy.sisfv.ui.theme.SolarGreen
import java.io.File

private val evidenciaVisitaSaver = listSaver<List<EvidenciaFotoUbicacion>, String>(
    save = { fotos -> fotos.flatMap { listOf(it.codigo, it.ruta, it.latitud.toString(), it.longitud.toString(), it.precision.toString(), it.fechaCaptura.toString()) } },
    restore = { values -> values.chunked(6).mapNotNull { parts ->
        if (parts.size != 6) null else runCatching {
            EvidenciaFotoUbicacion(parts[0], parts[1], parts[2].toDouble(), parts[3].toDouble(), parts[4].toFloat(), parts[5].toLong())
        }.getOrNull()
    } }
)

private val fotosVisitaNoEfectiva = listOf(
    "FOTO_PANORAMICA_VIVIENDA_SOLA" to "Foto panorámica vivienda sola",
    "FOTOGRAFIA_EVIDENCIA_USUARIO_AUSENTE" to "Fotografía evidencia usuario ausente"
)

@Composable
fun VisitaNoEfectivaScreen(
    tecnico: TecnicoEntity,
    sistema: SistemaSisfvEntity,
    onVolver: () -> Unit,
    onGuardar: () -> Unit,
    viewModel: FormularioMantenimientoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val estadoUbicacion = rememberUbicacionEvidencia()
    var evidencias by rememberSaveable(sistema.id, stateSaver = evidenciaVisitaSaver) { mutableStateOf(emptyList()) }
    var observaciones by rememberSaveable(sistema.id) { mutableStateOf("") }
    var firmaTecnico by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var mostrarConfirmacionSalida by rememberSaveable { mutableStateOf(false) }

    // Ubicación manual confirmada una sola vez para todas las fotografías.
    var coordenadasManualesConfirmadas by rememberSaveable(sistema.id) { mutableStateOf(false) }
    var latitudManual by rememberSaveable(sistema.id) { mutableStateOf("") }
    var longitudManual by rememberSaveable(sistema.id) { mutableStateOf("") }
    var ubicacionManual by remember { mutableStateOf<Location?>(null) }
    val ubicacionParaFotos = ubicacionManual ?: estadoUbicacion.ubicacion
    val permisoUbicacionParaFotos = estadoUbicacion.permisoConcedido || coordenadasManualesConfirmadas

    BackHandler { if (!uiState.isSaving) mostrarConfirmacionSalida = true }
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.limpiarEstado()
            onGuardar()
        }
    }

    if (mostrarConfirmacionSalida) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionSalida = false },
            title = { Text("Confirmar salida") },
            text = { Text("¿Está seguro de salir? Eso eliminará el formulario que se está diligenciando.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacionSalida = false
                    evidencias.forEach { File(it.ruta).delete() }
                    firmaTecnico?.let { File(it).delete() }
                    onVolver()
                }) { Text("SALIR SIN GUARDAR") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionSalida = false }) { Text("CONTINUAR DILIGENCIANDO") }
            }
        )
    }

    Scaffold(topBar = {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("VISITA NO EFECTIVA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SolarGreen)
            TextButton(onClick = { mostrarConfirmacionSalida = true }, enabled = !uiState.isSaving) { Text("VOLVER") }
        }
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("${sistema.codigoSisfv} · ${sistema.nombreUsuario}", fontWeight = FontWeight.Bold)
            Text("No se encontró un adulto responsable. Registra la evidencia de la visita.")
            SeccionUbicacionFotos(
                codigoSisfv = sistema.codigoSisfv,
                ubicacionEfectiva = ubicacionParaFotos,
                ubicacionPermitida = estadoUbicacion.permisoConcedido,
                manualConfirmado = coordenadasManualesConfirmadas,
                latitud = latitudManual,
                longitud = longitudManual,
                habilitado = !uiState.isSaving,
                onLatitudChange = { latitudManual = it },
                onLongitudChange = { longitudManual = it },
                onConfirmar = { manual ->
                    ubicacionManual = manual
                    coordenadasManualesConfirmadas = true
                },
                onCorregir = {
                    coordenadasManualesConfirmadas = false
                    ubicacionManual = null
                },
                onSolicitarPermiso = { estadoUbicacion.solicitarPermiso() }
            )
            fotosVisitaNoEfectiva.forEach { (codigo, titulo) ->
                CapturaFotoTitularidad(
                    titulo = titulo,
                    nombreArchivo = "visita_no_efectiva_${sistema.id}_$codigo",
                    rutaGuardada = evidencias.firstOrNull { it.codigo == codigo }?.ruta,
                    codigoSisfv = sistema.codigoSisfv,
                    nui = sistema.nui,
                    tecnico = tecnico,
                    ubicacionActual = ubicacionParaFotos,
                    ubicacionPermitida = permisoUbicacionParaFotos,
                    onSolicitarPermisoUbicacion = estadoUbicacion.solicitarPermiso,
                    subdirectorio = "visita_no_efectiva",
                    onFotoCapturada = { ruta, ubicacion ->
                        val captura = EvidenciaFotoUbicacion(
                            codigo = codigo,
                            ruta = ruta,
                            latitud = ubicacion.latitude,
                            longitud = ubicacion.longitude,
                            precision = ubicacion.accuracy,
                            fechaCaptura = File(ruta).lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
                        )
                        evidencias.firstOrNull { it.codigo == codigo }?.ruta?.takeIf { it != ruta }?.let { File(it).delete() }
                        evidencias = evidencias.filterNot { it.codigo == codigo } + captura
                    }
                )
            }
            OutlinedTextField(
                value = observaciones,
                onValueChange = { observaciones = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Observaciones") },
                minLines = 3
            )
            FirmaDigitalCanvas(
                titulo = "Firma del técnico en campo",
                nombreArchivo = "firma_tecnico_visita_no_efectiva_${sistema.id}",
                firmaGuardadaPath = firmaTecnico,
                onFirmaGuardada = { firmaTecnico = it },
                onEliminarFirma = { firmaTecnico?.let { File(it).delete() }; firmaTecnico = null }
            )
            (error ?: uiState.errorMessage)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                enabled = !uiState.isSaving,
                onClick = {
                    error = null
                    viewModel.guardarVisitaNoEfectiva(
                        sistema = sistema,
                        tecnico = tecnico,
                        observaciones = observaciones,
                        firmaTecnicoPath = firmaTecnico.orEmpty(),
                        evidencias = evidencias.toList()
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSaving) CircularProgressIndicator() else Text("GUARDAR VISITA NO EFECTIVA")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
