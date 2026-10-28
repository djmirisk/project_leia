package com.solaresenergy.sisfv.ui.mantenimiento

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import com.solaresenergy.sisfv.ui.theme.SolarGreen
import com.solaresenergy.sisfv.ui.theme.SolarPrimary
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

// Orden de las evidencias del formulario; la galería lo reutiliza para mostrarlas igual.
internal val codigosFotosHurto: List<String> = fotosHurto.map { it.first }

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var latitudManual by rememberSaveable(sistema.id) { mutableStateOf("") }
    var longitudManual by rememberSaveable(sistema.id) { mutableStateOf("") }
    var coordenadasManualesConfirmadas by rememberSaveable(sistema.id) { mutableStateOf(false) }
    var ubicacionFormulario by remember(sistema.id) { mutableStateOf<Location?>(null) }
    var camaraPermitida by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var rutaPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    var codigoPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    var tituloPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    var latitudPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    var longitudPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    var precisionPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    var coordenadasPendientesManuales by rememberSaveable { mutableStateOf(false) }
    var procesandoFoto by rememberSaveable { mutableStateOf(false) }

    val solicitarPermisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permitido ->
        camaraPermitida = permitido
        error = if (permitido) "Permiso de cámara concedido. Pulsa nuevamente la fotografía." else "Debes permitir el acceso a la cámara."
    }

    val tomarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val ruta = rutaPendiente
        val codigo = codigoPendiente
        val titulo = tituloPendiente
        val latitud = latitudPendiente?.toDoubleOrNull()
        val longitud = longitudPendiente?.toDoubleOrNull()
        val precision = precisionPendiente?.toFloatOrNull()
        rutaPendiente = null
        codigoPendiente = null
        tituloPendiente = null
        latitudPendiente = null
        longitudPendiente = null
        precisionPendiente = null

        val origen = ruta?.let(::File)
        if (!exito || origen == null || codigo == null || titulo == null || latitud == null || longitud == null || precision == null || !origen.isFile || origen.length() == 0L) {
            origen?.delete()
            error = "La cámara no entregó una fotografía. Intenta tomarla nuevamente."
        } else {
            procesandoFoto = true
            scope.launch {
                try {
                    val captura = withContext(Dispatchers.IO) {
                        val fecha = origen.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
                        val directorioTemporal = File(context.cacheDir, "evidencias_sisfv_tmp")
                        val directorioFinal = File(context.filesDir, "evidencias_sisfv/reporte_hurto")
                        if (!directorioTemporal.exists() && !directorioTemporal.mkdirs()) {
                            throw IllegalStateException("No se pudo crear la carpeta temporal de fotografías.")
                        }
                        if (!directorioFinal.exists() && !directorioFinal.mkdirs()) {
                            throw IllegalStateException("No se pudo crear la carpeta definitiva de fotografías.")
                        }
                        val destino = File(directorioFinal, "HURTO_${codigo}_${fecha}_marcada.jpg")
                        generarFotoConMarcaAgua(
                            context = context,
                            rutaCapturaTemporal = origen.absolutePath,
                            rutaFinal = destino.absolutePath,
                            codigoSisfv = sistema.codigoSisfv,
                            nui = sistema.nui,
                            tecnicoNombre = tecnico.nombre,
                            tecnicoId = tecnico.id,
                            tituloFoto = titulo,
                            fechaCaptura = fecha,
                            latitud = latitud,
                            longitud = longitud,
                            precision = precision,
                            coordenadasManuales = coordenadasPendientesManuales
                        )
                        if (!destino.isFile || destino.length() == 0L) {
                            throw IllegalStateException("La fotografía marcada no se pudo guardar.")
                        }
                        EvidenciaFotoUbicacion(codigo, destino.absolutePath, latitud, longitud, precision, fecha)
                    }
                    fotos.firstOrNull { it.codigo == captura.codigo }?.ruta?.takeIf { it != captura.ruta }?.let { File(it).delete() }
                    fotos = fotos.filterNot { it.codigo == captura.codigo } + captura
                    error = null
                } catch (e: Exception) {
                    error = "No se pudo guardar la foto con marca de agua: ${e.localizedMessage ?: "error de imagen"}"
                } finally {
                    origen.delete()
                    procesandoFoto = false
                }
            }
        }
    }

    fun tomar(codigo: String, titulo: String) {
        error = null
        val ubicacion = ubicacionFormulario ?: estadoUbicacion.ubicacion
        if (ubicacionFormulario == null && !estadoUbicacion.permisoConcedido) {
            estadoUbicacion.solicitarPermiso()
            error = "Permite la ubicación o confirma las coordenadas manuales antes de tomar la foto."
            return
        }
        if (!ubicacionGpsReciente(ubicacion)) {
            error = "No hay una ubicación reciente. Confirma la ubicación en la sección superior antes de tomar la foto."
            return
        }
        if (ubicacion == null) {
            error = "Confirma la ubicación antes de tomar la primera foto."
            return
        }
        if (!camaraPermitida) {
            solicitarPermisoCamara.launch(Manifest.permission.CAMERA)
            return
        }
        runCatching {
            val directorio = File(context.cacheDir, "evidencias_sisfv_tmp")
            if (!directorio.exists() && !directorio.mkdirs()) throw IllegalStateException("No se pudo crear la carpeta temporal de fotografías.")
            val archivo = File(directorio, "hurto_${codigo}_${System.currentTimeMillis()}_captura.jpg")
            if (!archivo.createNewFile()) throw IllegalStateException("No se pudo preparar el archivo para la cámara.")
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
            rutaPendiente = archivo.absolutePath
            codigoPendiente = codigo
            tituloPendiente = titulo
            latitudPendiente = ubicacion.latitude.toString()
            longitudPendiente = ubicacion.longitude.toString()
            precisionPendiente = ubicacion.accuracy.toString()
            coordenadasPendientesManuales = ubicacion.provider == "manual"
            tomarFoto.launch(uri)
        }.onFailure { error = it.localizedMessage ?: "No se pudo abrir la cámara." }
    }

    BackHandler { if (!state.isSaving && !procesandoFoto) mostrarConfirmacionSalida = true }
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
            TextButton(onClick = { mostrarConfirmacionSalida = true }, enabled = !state.isSaving && !procesandoFoto) { Text("VOLVER", maxLines = 1) }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${sistema.codigoSisfv} · ${sistema.nombreUsuario}", fontWeight = FontWeight.Bold)
            Text("Evidencias fotográficas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SeccionUbicacionFotos(
                codigoSisfv = sistema.codigoSisfv,
                ubicacionEfectiva = ubicacionFormulario ?: estadoUbicacion.ubicacion,
                ubicacionPermitida = estadoUbicacion.permisoConcedido,
                manualConfirmado = coordenadasManualesConfirmadas,
                latitud = latitudManual,
                longitud = longitudManual,
                habilitado = !procesandoFoto && !state.isSaving,
                onLatitudChange = { latitudManual = it },
                onLongitudChange = { longitudManual = it },
                onConfirmar = { manual ->
                    ubicacionFormulario = manual
                    coordenadasManualesConfirmadas = true
                },
                onCorregir = {
                    coordenadasManualesConfirmadas = false
                    ubicacionFormulario = null
                },
                onSolicitarPermiso = { estadoUbicacion.solicitarPermiso() }
            )
            fotosHurto.forEach { (codigo, titulo) ->
                Button(
                    onClick = { tomar(codigo, titulo) },
                    enabled = !procesandoFoto && !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            procesandoFoto && codigoPendiente == codigo -> "APLICANDO MARCA DE AGUA…"
                            fotos.any { it.codigo == codigo } -> "✓ $titulo · REPETIR"
                            else -> "TOMAR · $titulo"
                        }
                    )
                }
            }
            OutlinedTextField(
                value = testimonio,
                onValueChange = { testimonio = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Novedades") },
                placeholder = { Text("Describa el testimonio de quien reporta el hurto", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)) },
                minLines = 4
            )
            OutlinedTextField(nombre, { nombre = capitalizarNombre(it) }, modifier = Modifier.fillMaxWidth(), label = { Text("Nombre completo de quien reporta") }, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next))
            OutlinedTextField(documento, { documento = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Documento de identidad") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next))
            Text("¿Quien reporta el hurto es el titular?")
            Row(verticalAlignment = Alignment.CenterVertically) {
                OpcionRadio("Sí", seleccionado = esTitular == true, onClick = { esTitular = true })
                Spacer(Modifier.height(1.dp).weight(1f))
                OpcionRadio("No", seleccionado = esTitular == false, onClick = { esTitular = false })
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
                enabled = !state.isSaving && !procesandoFoto,
                modifier = Modifier.fillMaxWidth()
            ) { if (state.isSaving) CircularProgressIndicator() else Text("GUARDAR REPORTE DE HURTO") }
        }
    }
}
