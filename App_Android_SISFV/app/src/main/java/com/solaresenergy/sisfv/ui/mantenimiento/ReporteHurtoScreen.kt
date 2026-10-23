package com.solaresenergy.sisfv.ui.mantenimiento

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import com.solaresenergy.sisfv.ui.theme.SolarGreen
import java.io.File

private val fotosHurto = listOf(
    "PANORAMICA_HURTO" to "Foto #1 · Panorámica del lugar donde estaba el SISFV",
    "POSTE_PANELES" to "Foto #2 · Poste y paneles",
    "SPT_PUESTA_TIERRA" to "Foto #4 · Sistema de puesta a tierra",
    "GABINETE_CERRADO" to "Foto #5 · Gabinete cerrado",
    "TABLERO_DISTRIBUCION_AC" to "Foto #12 · Tablero de distribución AC",
    "INTERRUPTOR_LUCES" to "Foto #13 · Interruptor de luces",
    "TOMACORRIENTES" to "Foto #14 · Tomacorrientes",
    "BOMBILLO_ENCENDIDO" to "Foto #15 · Bombillo"
)

private val fotosHurtoSaver = listSaver<List<EvidenciaFotoUbicacion>, String>(
    save = { fotos -> fotos.flatMap { listOf(it.codigo, it.ruta, it.latitud.toString(), it.longitud.toString(), it.precision.toString(), it.fechaCaptura.toString()) } },
    restore = { values -> values.chunked(6).mapNotNull { parts ->
        if (parts.size != 6) null else runCatching {
            EvidenciaFotoUbicacion(parts[0], parts[1], parts[2].toDouble(), parts[3].toDouble(), parts[4].toFloat(), parts[5].toLong())
        }.getOrNull()
    } }
)

@Composable
fun ReporteHurtoScreen(
    tecnico: TecnicoEntity,
    sistema: SistemaSisfvEntity,
    onVolver: () -> Unit,
    onGuardar: () -> Unit,
    viewModel: FormularioMantenimientoViewModel = viewModel()
) {
    val estadoUbicacion = rememberUbicacionEvidencia()
    val state by viewModel.uiState.collectAsState()
    var fotos by rememberSaveable(sistema.id, stateSaver = fotosHurtoSaver) { mutableStateOf(emptyList()) }
    var testimonio by rememberSaveable(sistema.id) { mutableStateOf("") }
    var nombre by rememberSaveable(sistema.id) { mutableStateOf("") }
    var documento by rememberSaveable(sistema.id) { mutableStateOf("") }
    var esTitular by rememberSaveable(sistema.id) { mutableStateOf<Boolean?>(null) }
    var firmaReportante by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var firmaTecnico by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var mostrarConfirmacionSalida by rememberSaveable { mutableStateOf(false) }

    BackHandler { if (!state.isSaving) mostrarConfirmacionSalida = true }
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
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
                    fotos.forEach { File(it.ruta).delete() }
                    listOfNotNull(firmaReportante, firmaTecnico).forEach { File(it).delete() }
                    onVolver()
                }) { Text("SALIR SIN GUARDAR") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionSalida = false }) { Text("CONTINUAR DILIGENCIANDO") }
            }
        )
    }

    Scaffold(topBar = {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("REPORTE DE HURTO", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SolarGreen)
            TextButton(onClick = { mostrarConfirmacionSalida = true }, enabled = !state.isSaving) { Text("VOLVER", maxLines = 1) }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${sistema.codigoSisfv} · ${sistema.nombreUsuario}", fontWeight = FontWeight.Bold)
            Text("Evidencias fotográficas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            fotosHurto.forEach { (codigo, titulo) ->
                CapturaFotoTitularidad(
                    titulo = titulo,
                    nombreArchivo = "hurto_${sistema.id}_$codigo",
                    rutaGuardada = fotos.firstOrNull { it.codigo == codigo }?.ruta,
                    codigoSisfv = sistema.codigoSisfv,
                    nui = sistema.nui,
                    tecnico = tecnico,
                    ubicacionActual = estadoUbicacion.ubicacion,
                    ubicacionPermitida = estadoUbicacion.permisoConcedido,
                    onSolicitarPermisoUbicacion = estadoUbicacion.solicitarPermiso,
                    subdirectorio = "reporte_hurto",
                    onFotoCapturada = { ruta, ubicacion ->
                        val captura = EvidenciaFotoUbicacion(
                            codigo = codigo,
                            ruta = ruta,
                            latitud = ubicacion.latitude,
                            longitud = ubicacion.longitude,
                            precision = ubicacion.accuracy,
                            fechaCaptura = File(ruta).lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
                        )
                        fotos.firstOrNull { it.codigo == codigo }?.ruta?.takeIf { it != ruta }?.let { File(it).delete() }
                        fotos = fotos.filterNot { it.codigo == codigo } + captura
                    }
                )
            }
            OutlinedTextField(
                value = testimonio,
                onValueChange = { testimonio = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Novedades") },
                placeholder = { Text("Describa el testimonio de quien reporta el hurto", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)) },
                minLines = 4
            )
            OutlinedTextField(nombre, { nombre = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Nombre completo de quien reporta") })
            OutlinedTextField(documento, { documento = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Documento de identidad") })
            Text("¿Quien reporta el hurto es el titular?")
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = esTitular == true, onClick = { esTitular = true }); Text("Sí")
                Spacer(Modifier.height(1.dp).weight(1f))
                RadioButton(selected = esTitular == false, onClick = { esTitular = false }); Text("No")
            }
            FirmaDigitalCanvas(
                titulo = "Firma de quien reporta el hurto",
                nombreArchivo = "firma_reportante_hurto_${sistema.id}",
                firmaGuardadaPath = firmaReportante,
                onFirmaGuardada = { firmaReportante = it },
                onEliminarFirma = { firmaReportante?.let { File(it).delete() }; firmaReportante = null }
            )
            FirmaDigitalCanvas(
                titulo = "Firma del técnico en campo",
                nombreArchivo = "firma_tecnico_hurto_${sistema.id}",
                firmaGuardadaPath = firmaTecnico,
                onFirmaGuardada = { firmaTecnico = it },
                onEliminarFirma = { firmaTecnico?.let { File(it).delete() }; firmaTecnico = null }
            )
            (error ?: state.errorMessage)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    error = null
                    if (esTitular == null) error = "Indique si quien reporta es el titular."
                    else viewModel.guardarReporteHurto(
                        sistema, tecnico, testimonio, nombre, documento, esTitular!!,
                        firmaReportante.orEmpty(), firmaTecnico.orEmpty(), fotos.toList()
                    )
                },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) { if (state.isSaving) CircularProgressIndicator() else Text("GUARDAR REPORTE DE HURTO") }
        }
    }
}
