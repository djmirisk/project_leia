package com.solaresenergy.sisfv.ui.mantenimiento

import android.content.Context
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Patterns
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import com.solaresenergy.sisfv.ui.theme.SolarPrimary

private fun crearArchivoEvidenciaMedidor(
    context: Context,
    tipo: String
): Pair<Uri, String> {
    val directorio = File(context.cacheDir, "evidencias_sisfv_tmp")
    if (!directorio.exists() && !directorio.mkdirs()) {
        throw IllegalStateException("No se pudo crear la carpeta temporal de evidencias.")
    }

    val archivo = File(
        directorio,
        "${tipo}_${System.currentTimeMillis()}.jpg"
    )
    if (!archivo.createNewFile()) {
        throw IllegalStateException("No se pudo crear el archivo de la fotografía.")
    }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        archivo
    )
    return uri to archivo.absolutePath
}

private fun tienePermisoUbicacion(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

private suspend fun guardarFotoMedidorConMarcaAgua(
    context: Context,
    rutaTemporal: String,
    tipo: String,
    titulo: String,
    codigoSisfv: String,
    nui: String?,
    tecnicoNombre: String,
    tecnicoId: Int,
    ubicacion: Location
): Pair<String, Long> = withContext(Dispatchers.IO) {
    val fechaCaptura = File(rutaTemporal).lastModified().takeIf { it > 0L }
        ?: System.currentTimeMillis()
    val directorioFinal = File(context.filesDir, "evidencias_sisfv")
    if (!directorioFinal.exists() && !directorioFinal.mkdirs()) {
        File(rutaTemporal).delete()
        throw IllegalStateException("No se pudo crear la carpeta definitiva de evidencias.")
    }
    val archivoFinal = File(directorioFinal, "${tipo}_${fechaCaptura}_marcada.jpg")
    try {
        generarFotoConMarcaAgua(
            context = context,
            rutaCapturaTemporal = rutaTemporal,
            rutaFinal = archivoFinal.absolutePath,
            codigoSisfv = codigoSisfv,
            nui = nui,
            tecnicoNombre = tecnicoNombre,
            tecnicoId = tecnicoId,
            tituloFoto = titulo,
            fechaCaptura = fechaCaptura,
            latitud = ubicacion.latitude,
            longitud = ubicacion.longitude,
            precision = ubicacion.accuracy,
            coordenadasManuales = ubicacion.provider == "manual"
        )
        if (!archivoFinal.exists() || archivoFinal.length() == 0L) {
            throw IllegalStateException("La fotografía marcada no se pudo guardar.")
        }
        archivoFinal.absolutePath to fechaCaptura
    } catch (e: Exception) {
        archivoFinal.delete()
        throw e
    } finally {
        // El original temporal sin marca de agua no se conserva.
        File(rutaTemporal).delete()
    }
}

private fun convertirNumero(texto: String): Double? =
    texto.trim().replace(",", ".").toDoubleOrNull()

private fun firmaValida(ruta: String?): Boolean {
    if (ruta.isNullOrBlank()) return false
    val archivo = File(ruta)
    return archivo.isFile && archivo.length() > 0L
}

private val fotosMantenimientoSaver = listSaver<List<FotoMantenimientoCapturada>, Bundle>(
    save = { fotos -> fotos.map { foto ->
        Bundle().apply {
            putString("codigo", foto.codigoFoto)
            putInt("orden", foto.orden)
            putString("titulo", foto.titulo)
            putString("ruta", foto.rutaEvidencia)
            putLong("fecha", foto.fechaCaptura)
            putDouble("latitud", foto.latitudGps)
            putDouble("longitud", foto.longitudGps)
            putFloat("precision", foto.precisionGps)
            putString("hash", foto.hashSha256)
        }
    } },
    restore = { bundles -> bundles.map { foto ->
        FotoMantenimientoCapturada(
            codigoFoto = foto.getString("codigo").orEmpty(),
            orden = foto.getInt("orden"),
            titulo = foto.getString("titulo").orEmpty(),
            rutaEvidencia = foto.getString("ruta").orEmpty(),
            fechaCaptura = foto.getLong("fecha"),
            latitudGps = foto.getDouble("latitud"),
            longitudGps = foto.getDouble("longitud"),
            precisionGps = foto.getFloat("precision"),
            hashSha256 = foto.getString("hash").orEmpty()
        )
    } }
)

@Composable
fun FormularioMantenimientoScreen(
    tecnico: TecnicoEntity,
    sistema: SistemaSisfvEntity,
    onVolver: () -> Unit,
    onGuardar: () -> Unit,
    correctivoObligatorio: Boolean = false,
    viewModel: FormularioMantenimientoViewModel = viewModel()
) {

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var errorFormulario by rememberSaveable { mutableStateOf<String?>(null) }
    var mostrarConfirmacionSalida by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var ubicacionPermitida by remember { mutableStateOf(tienePermisoUbicacion(context)) }
    var ubicacionGps by remember { mutableStateOf<Location?>(null) }
    var coordenadasManualesConfirmadas by rememberSaveable(sistema.id) { mutableStateOf(false) }
    var latitudManualConfirmada by rememberSaveable(sistema.id) { mutableStateOf("") }
    var longitudManualConfirmada by rememberSaveable(sistema.id) { mutableStateOf("") }
    var errorCoordenadasManual by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(coordenadasManualesConfirmadas, latitudManualConfirmada, longitudManualConfirmada) {
        if (coordenadasManualesConfirmadas && ubicacionGps?.provider != "manual") {
            val latitud = latitudManualConfirmada.toDoubleOrNull()
            val longitud = longitudManualConfirmada.toDoubleOrNull()
            if (latitud != null && longitud != null) {
                ubicacionGps = Location("manual").apply {
                    latitude = latitud
                    this.longitude = longitud
                    accuracy = 0f
                    time = System.currentTimeMillis()
                }
                ubicacionPermitida = true
            }
        }
    }

    val solicitarPermisoUbicacionGps = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        ubicacionPermitida = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                tienePermisoUbicacion(context)
        errorFormulario = if (ubicacionPermitida) {
            "Permiso GPS concedido. Espera a que la tablet obtenga una posición y vuelve a pulsar la fotografía."
        } else {
            "Debes permitir la ubicación para incluir coordenadas GPS en las fotografías."
        }
    }

    DisposableEffect(context, ubicacionPermitida) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var listener: LocationListener? = null
        if (ubicacionPermitida) {
            val receptorUbicacion = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (coordenadasManualesConfirmadas) return
                    val anterior = ubicacionGps
                    if (anterior == null || location.time >= anterior.time || location.accuracy < anterior.accuracy) {
                        ubicacionGps = location
                    }
                }
                @Deprecated("Deprecated by Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }
            listener = receptorUbicacion
            try {
                val proveedores = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                val ultimaConocida = proveedores.mapNotNull { proveedor ->
                    try { manager.getLastKnownLocation(proveedor) } catch (_: Exception) { null }
                }.maxByOrNull { it.time }
                if (!coordenadasManualesConfirmadas && ultimaConocida != null) ubicacionGps = ultimaConocida
                proveedores.forEach { proveedor ->
                    try {
                        if (manager.isProviderEnabled(proveedor)) {
                            manager.requestLocationUpdates(
                                proveedor, 2_000L, 0f, receptorUbicacion, Looper.getMainLooper()
                            )
                        }
                    } catch (_: Exception) {
                        // El proveedor puede estar ausente o no disponible en campo.
                    }
                }
            } catch (_: Exception) {
                errorFormulario = "No fue posible iniciar el GPS. Verifica que la ubicación esté activada."
            }
        }
        onDispose {
            try { listener?.let { manager.removeUpdates(it) } } catch (_: Exception) { }
        }
    }

    var ubicacionFotoLecturaPendiente by remember { mutableStateOf<Location?>(null) }
    var ubicacionFotoCodigoPendiente by remember { mutableStateOf<Location?>(null) }
    var procesandoFotoMedidor by remember { mutableStateOf(false) }

    val solicitarPermisoCamara =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { concedido ->

            errorFormulario = if (concedido) {
                "Permiso concedido. Pulsa nuevamente el botón para tomar la fotografía."
            } else {
                "Debes permitir el acceso a la cámara para capturar la evidencia."
            }
        }
    // Datos y evidencias del medidor Excelec PPKW
    var energiaConsumidaKwh by rememberSaveable(sistema.id) { mutableStateOf("") }
    var evidenciaLecturaPath by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var evidenciaCodigoPath by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var fechaHoraLectura by rememberSaveable(sistema.id) { mutableStateOf<Long?>(null) }
    var rutaFotoLecturaPendiente by remember { mutableStateOf<String?>(null) }
    var rutaFotoCodigoPendiente by remember { mutableStateOf<String?>(null) }

    val capturarFotoLectura = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        val rutaTemporal = rutaFotoLecturaPendiente
        val ubicacion = ubicacionFotoLecturaPendiente
        rutaFotoLecturaPendiente = null
        ubicacionFotoLecturaPendiente = null
        if (exito && rutaTemporal != null && ubicacion != null) {
            procesandoFotoMedidor = true
            scope.launch {
                try {
                    val (rutaFinal, fecha) = guardarFotoMedidorConMarcaAgua(
                        context = context,
                        rutaTemporal = rutaTemporal,
                        tipo = "lectura_ppkw",
                        titulo = "Lectura del medidor Excelec PPKW",
                        codigoSisfv = sistema.codigoSisfv,
                        nui = sistema.nui,
                        tecnicoNombre = tecnico.nombre,
                        tecnicoId = tecnico.id,
                        ubicacion = ubicacion
                    )
                    evidenciaLecturaPath?.let { anterior ->
                        if (anterior != rutaFinal) File(anterior).delete()
                    }
                    evidenciaLecturaPath = rutaFinal
                    fechaHoraLectura = fecha
                    errorFormulario = null
                } catch (e: Exception) {
                    errorFormulario = "No se pudo marcar la fotografía de lectura: ${e.localizedMessage ?: "error de imagen"}"
                } finally {
                    procesandoFotoMedidor = false
                }
            }
        } else {
            rutaTemporal?.let { File(it).delete() }
            errorFormulario = "No se capturó la fotografía de lectura o no hay ubicación GPS asociada."
        }
    }

    val capturarFotoCodigo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        val rutaTemporal = rutaFotoCodigoPendiente
        val ubicacion = ubicacionFotoCodigoPendiente
        rutaFotoCodigoPendiente = null
        ubicacionFotoCodigoPendiente = null
        if (exito && rutaTemporal != null && ubicacion != null) {
            procesandoFotoMedidor = true
            scope.launch {
                try {
                    val (rutaFinal, _) = guardarFotoMedidorConMarcaAgua(
                        context = context,
                        rutaTemporal = rutaTemporal,
                        tipo = "codigo_ppkw",
                        titulo = "Código activo del medidor Excelec PPKW",
                        codigoSisfv = sistema.codigoSisfv,
                        nui = sistema.nui,
                        tecnicoNombre = tecnico.nombre,
                        tecnicoId = tecnico.id,
                        ubicacion = ubicacion
                    )
                    evidenciaCodigoPath?.let { anterior ->
                        if (anterior != rutaFinal) File(anterior).delete()
                    }
                    evidenciaCodigoPath = rutaFinal
                    errorFormulario = null
                } catch (e: Exception) {
                    errorFormulario = "No se pudo marcar la fotografía del código: ${e.localizedMessage ?: "error de imagen"}"
                } finally {
                    procesandoFotoMedidor = false
                }
            }
        } else {
            rutaTemporal?.let { File(it).delete() }
            errorFormulario = "No se capturó la fotografía del código o no hay ubicación GPS asociada."
        }
    }

    fun iniciarCapturaFotoMedidor(esLectura: Boolean) {
        if (procesandoFotoMedidor) return
        if (!ubicacionPermitida) {
            solicitarPermisoUbicacionGps.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            errorFormulario = "Autoriza la ubicación GPS; luego espera una posición y vuelve a pulsar la fotografía."
            return
        }
        val ubicacion = ubicacionGps
        if (!ubicacionGpsReciente(ubicacion)) {
            errorFormulario = "No hay una ubicación GPS reciente. Activa la ubicación y espera una posición antes de tomar la fotografía."
            return
        }
        val tienePermisoCamara = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!tienePermisoCamara) {
            solicitarPermisoCamara.launch(Manifest.permission.CAMERA)
            return
        }
        try {
            val (uri, ruta) = crearArchivoEvidenciaMedidor(
                context,
                if (esLectura) "lectura_ppkw" else "codigo_ppkw"
            )
            if (esLectura) {
                rutaFotoLecturaPendiente = ruta
                ubicacionFotoLecturaPendiente = Location(ubicacion!!)
                capturarFotoLectura.launch(uri)
            } else {
                rutaFotoCodigoPendiente = ruta
                ubicacionFotoCodigoPendiente = Location(ubicacion!!)
                capturarFotoCodigo.launch(uri)
            }
        } catch (e: Exception) {
            errorFormulario = "Error al iniciar la fotografía: ${e.localizedMessage ?: "error desconocido"}"
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.limpiarEstado()
            onGuardar()
        }
    }

    BackHandler {
        if (!uiState.isSaving) mostrarConfirmacionSalida = true
    }

    // =========================================================
    // TIPO DE MANTENIMIENTO
    // =========================================================

    var tipoMantenimiento by rememberSaveable(sistema.id, correctivoObligatorio) {
        mutableStateOf(if (correctivoObligatorio) "CORRECTIVO" else "PREVENTIVO")
    }

    // =========================================================
    // MEDICIONES ELÉCTRICAS
    // =========================================================

    var voltajePanelDc by rememberSaveable(sistema.id) { mutableStateOf("") }
    var corrientePanelDc by rememberSaveable(sistema.id) { mutableStateOf("") }

    var voltajeBateriaDc by rememberSaveable(sistema.id) { mutableStateOf("") }
    var corrienteBateriaDc by rememberSaveable(sistema.id) { mutableStateOf("") }

    var voltajeInversorAc by rememberSaveable(sistema.id) { mutableStateOf("") }

    // =========================================================
    // PROTECCIONES
    // =========================================================

    var breakerVerificado by rememberSaveable(sistema.id) { mutableStateOf(false) }
    var breakerNormal by rememberSaveable(sistema.id) { mutableStateOf(true) }
    var breakerNovedad by rememberSaveable(sistema.id) { mutableStateOf("") }

    var fusibleVerificado by rememberSaveable(sistema.id) { mutableStateOf(false) }
    var fusibleNormal by rememberSaveable(sistema.id) { mutableStateOf(true) }
    var fusibleNovedad by rememberSaveable(sistema.id) { mutableStateOf("") }

    var dpsVerificado by rememberSaveable(sistema.id) { mutableStateOf(false) }
    var dpsNormal by rememberSaveable(sistema.id) { mutableStateOf(true) }
    var dpsNovedad by rememberSaveable(sistema.id) { mutableStateOf("") }
    // =========================================================
    // TORQUE
    // =========================================================

    var torqueBateria by rememberSaveable(sistema.id) { mutableStateOf(false) }
    var torqueBateriaConforme by rememberSaveable(sistema.id) { mutableStateOf(true) }
    var torqueBateriaNovedad by rememberSaveable(sistema.id) { mutableStateOf("") }

    var torqueBarrajePositivo by rememberSaveable(sistema.id) { mutableStateOf(false) }
    var torqueBarrajePositivoConforme by rememberSaveable(sistema.id) { mutableStateOf(true) }
    var torqueBarrajePositivoNovedad by rememberSaveable(sistema.id) { mutableStateOf("") }

    var torqueBarrajeNegativo by rememberSaveable(sistema.id) { mutableStateOf(false) }
    var torqueBarrajeNegativoConforme by rememberSaveable(sistema.id) { mutableStateOf(true) }
    var torqueBarrajeNegativoNovedad by rememberSaveable(sistema.id) { mutableStateOf("") }

    var torqueBarrajeNeutro by rememberSaveable(sistema.id) { mutableStateOf(false) }
    var torqueBarrajeNeutroConforme by rememberSaveable(sistema.id) { mutableStateOf(true) }
    var torqueBarrajeNeutroNovedad by rememberSaveable(sistema.id) { mutableStateOf("") }
    // =========================================================
    // =========================================================
// MEDIDOR
// =========================================================

// Por regulación, se asume instalado inicialmente.
// Si no existe físicamente, el técnico debe declararlo.
    var medidorAplica by rememberSaveable(sistema.id) {
        mutableStateOf(true)
    }

// Estado de lectura del medidor instalado.
    var medidorNormal by rememberSaveable(sistema.id) {
        mutableStateOf(true)
    }

    var codigoActivo by rememberSaveable(sistema.id) {
        mutableStateOf("")
    }

// Justificación obligatoria cuando el medidor no está instalado.
    var motivoMedidorNoInstalado by rememberSaveable(sistema.id) {
        mutableStateOf("")
    }
// =========================================================
// ACTUALIZACIÓN DE DATOS DE CONTACTO
// =========================================================

    var telefonoContactoPropuesto by rememberSaveable(sistema.id) {
        mutableStateOf("")
    }

    var correoContactoPropuesto by rememberSaveable(sistema.id) {
        mutableStateOf("")
    }

    // El técnico decide si actualiza los datos de contacto; si acepta, se abren
    // los campos en un diálogo y ambos datos quedan obligatorios.
    var deseaActualizarContacto by rememberSaveable(sistema.id) {
        mutableStateOf<Boolean?>(null)
    }

    var mostrarDialogoContacto by rememberSaveable(sistema.id) {
        mutableStateOf(false)
    }
    // Catálogo obligatorio de 18 evidencias fotográficas
    var fotosCapturadas by rememberSaveable(sistema.id, stateSaver = fotosMantenimientoSaver) {
        mutableStateOf<List<FotoMantenimientoCapturada>>(emptyList())
    }
    var bombilloEncendido by rememberSaveable(sistema.id) {
        mutableStateOf<Boolean?>(null)
    }

    // =========================================================
    // OBSERVACIONES
    // =========================================================

    var observaciones by rememberSaveable(sistema.id) {
        mutableStateOf("")
    }

    // Datos y firmas de la persona que recibe la visita y del técnico
    var nombreReceptorFirma by rememberSaveable(sistema.id) { mutableStateOf("") }
    var documentoReceptorFirma by rememberSaveable(sistema.id) { mutableStateOf("") }
    var receptorEsTitular by rememberSaveable(sistema.id) { mutableStateOf<Boolean?>(null) }
    var firmaUsuarioPath by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var firmaTecnicoPath by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }

    if (mostrarConfirmacionSalida) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionSalida = false },
            title = { Text("Confirmar salida") },
            text = { Text("¿Está seguro de salir? Eso eliminará el formulario que se está diligenciando.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacionSalida = false
                    fotosCapturadas.forEach { File(it.rutaEvidencia).delete() }
                    listOfNotNull(
                        evidenciaLecturaPath,
                        evidenciaCodigoPath,
                        firmaUsuarioPath,
                        firmaTecnicoPath
                    ).distinct().forEach { File(it).delete() }
                    onVolver()
                }) { Text("SALIR SIN GUARDAR") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionSalida = false }) { Text("CONTINUAR DILIGENCIANDO") }
            }
        )
    }

    if (mostrarDialogoContacto) {
        var telefonoTemporal by remember { mutableStateOf(telefonoContactoPropuesto) }
        var correoTemporal by remember { mutableStateOf(correoContactoPropuesto) }
        var errorContacto by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                mostrarDialogoContacto = false
                if (telefonoContactoPropuesto.isBlank()) {
                    deseaActualizarContacto = false
                }
            },
            title = { Text("Datos de contacto del usuario") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Ingrese el nuevo teléfono del usuario. El correo electrónico es opcional.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = telefonoTemporal,
                        onValueChange = { entrada ->
                            telefonoTemporal = entrada.filter(Char::isDigit).take(10)
                            errorContacto = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nuevo teléfono") },
                        placeholder = { Text("3001234567") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        )
                    )
                    OutlinedTextField(
                        value = correoTemporal,
                        onValueChange = {
                            correoTemporal = it
                            errorContacto = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Correo electrónico (opcional)") },
                        placeholder = { Text("usuario@correo.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    errorContacto?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val telefono = telefonoTemporal.trim()
                    val correo = correoTemporal.trim()
                    val error = when {
                        telefono.isBlank() -> "Ingrese el nuevo teléfono del usuario."
                        telefono.length != 10 ->
                            "El teléfono debe tener 10 dígitos."
                        correo.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(correo).matches() ->
                            "Ingrese un correo electrónico válido o déjelo vacío."
                        else -> null
                    }
                    errorContacto = error
                    if (error == null) {
                        telefonoContactoPropuesto = telefono
                        correoContactoPropuesto = correo
                        deseaActualizarContacto = true
                        mostrarDialogoContacto = false
                        errorFormulario = null
                    }
                }) { Text("GUARDAR") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoContacto = false
                    if (telefonoContactoPropuesto.isBlank()) {
                        deseaActualizarContacto = false
                    }
                }) { Text("CANCELAR") }
            }
        )
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
                    text = "Mantenimiento ML-GT-001",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SolarPrimary
                )

                TextButton(
                    onClick = { mostrarConfirmacionSalida = true },
                    enabled = !uiState.isSaving
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // =================================================
            // 1. SISFV SELECCIONADO
            // =================================================

            Text(
                text = "SISFV seleccionado",
                fontWeight = FontWeight.Bold,
                color = SolarPrimary
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = sistema.codigoSisfv,
                        fontWeight = FontWeight.Bold,
                        color = SolarPrimary
                    )

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
                        text = "Proyecto ID: ${sistema.proyectoId}"
                    )

                    Text(
                        text = "Técnico: ${tecnico.nombre}"
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            HorizontalDivider()

            Spacer(
                modifier = Modifier.height(20.dp)
            )
// =========================================================
// ACTUALIZACIÓN DE DATOS DE CONTACTO
// =========================================================

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Actualización de datos de contacto",
                        fontWeight = FontWeight.Bold,
                        color = SolarPrimary
                    )

                    Text(
                        text = "¿Desea actualizar los datos de contacto del usuario? " +
                                "El teléfono es obligatorio y el correo electrónico es opcional.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OpcionRadio(
                            texto = "Sí, actualizar",
                            seleccionado = deseaActualizarContacto == true,
                            onClick = {
                                deseaActualizarContacto = true
                                mostrarDialogoContacto = true
                                errorFormulario = null
                            }
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                        OpcionRadio(
                            texto = "No",
                            seleccionado = deseaActualizarContacto == false,
                            onClick = {
                                deseaActualizarContacto = false
                                mostrarDialogoContacto = false
                                telefonoContactoPropuesto = ""
                                correoContactoPropuesto = ""
                                errorFormulario = null
                            }
                        )
                    }

                    if (deseaActualizarContacto == true &&
                        telefonoContactoPropuesto.isNotBlank()
                    ) {
                        Text(
                            text = "Nuevo teléfono: $telefonoContactoPropuesto",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = if (correoContactoPropuesto.isBlank()) {
                                "Correo electrónico: no registrado"
                            } else {
                                "Correo electrónico: $correoContactoPropuesto"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = { mostrarDialogoContacto = true }) {
                            Text("EDITAR DATOS DE CONTACTO")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            // =================================================
            // 2. TIPO DE MANTENIMIENTO
            // =================================================

            Text(
                text = "1. Tipo de mantenimiento",
                fontWeight = FontWeight.Bold,
                color = SolarPrimary
            )

            if (correctivoObligatorio) {
                Text("Este mantenimiento correctivo completa el cambio de componente.", color = SolarPrimary)
            } else Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                OpcionRadio(
                    texto = "Preventivo",
                    seleccionado = tipoMantenimiento == "PREVENTIVO",
                    onClick = {
                        tipoMantenimiento = "PREVENTIVO"
                    }
                )

                OpcionRadio(
                    texto = "Correctivo",
                    seleccionado = tipoMantenimiento == "CORRECTIVO",
                    onClick = {
                        tipoMantenimiento = "CORRECTIVO"
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // =================================================
            // 3. MEDICIONES ELÉCTRICAS
            // =================================================

            Text(
                text = "2. Mediciones eléctricas",
                fontWeight = FontWeight.Bold,
                color = SolarPrimary
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            OutlinedTextField(
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                value = voltajePanelDc,
                onValueChange = {
                    voltajePanelDc = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Voltaje panel DC (V)")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedTextField(
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                value = corrientePanelDc,
                onValueChange = {
                    corrientePanelDc = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Corriente panel DC (A)")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedTextField(
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                value = voltajeBateriaDc,
                onValueChange = {
                    voltajeBateriaDc = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Voltaje batería DC (V)")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedTextField(
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                value = corrienteBateriaDc,
                onValueChange = {
                    corrienteBateriaDc = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Corriente batería DC (A)")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedTextField(
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                value = voltajeInversorAc,
                onValueChange = {
                    voltajeInversorAc = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Voltaje salida inversor AC (V)")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // =================================================
            // 4. PROTECCIONES
            // =================================================

            Text(
                text = "3. Estado de las protecciones",
                fontWeight = FontWeight.Bold,
                color = SolarPrimary
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            // BREAKER

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                FilaCasilla(
                    texto = "Breaker verificado",
                    marcado = breakerVerificado,
                    onCambio = {
                        breakerVerificado = it
                    }
                )
            }

            if (breakerVerificado) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    OpcionRadio(
                        texto = "Normal",
                        seleccionado = breakerNormal,
                        onClick = {
                            breakerNormal = true
                        }
                    )

                    OpcionRadio(
                        texto = "Novedad",
                        seleccionado = !breakerNormal,
                        onClick = {
                            breakerNormal = false
                        }
                    )
                }

                if (!breakerNormal) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    OutlinedTextField(
                        value = breakerNovedad,
                        onValueChange = {
                            breakerNovedad = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Describa la novedad del breaker")
                        },
                        placeholder = {
                            Text("Ej. Breaker deteriorado, no acciona...")
                        }
                    )
                }
            }

            // FUSIBLE

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                FilaCasilla(
                    texto = "Fusibles verificados",
                    marcado = fusibleVerificado,
                    onCambio = {
                        fusibleVerificado = it
                    }
                )
            }

            if (fusibleVerificado) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    OpcionRadio(
                        texto = "Normal",
                        seleccionado = fusibleNormal,
                        onClick = {
                            fusibleNormal = true
                        }
                    )

                    OpcionRadio(
                        texto = "Novedad",
                        seleccionado = !fusibleNormal,
                        onClick = {
                            fusibleNormal = false
                        }
                    )
                }

                if (!fusibleNormal) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    OutlinedTextField(
                        value = fusibleNovedad,
                        onValueChange = {
                            fusibleNovedad = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Describa la novedad de los fusibles")
                        },
                        placeholder = {
                            Text("Ej. Fusible abierto, deteriorado...")
                        }
                    )
                }
            }

            // DPS

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                FilaCasilla(
                    texto = "DPS verificado",
                    marcado = dpsVerificado,
                    onCambio = {
                        dpsVerificado = it
                    }
                )
            }

            if (dpsVerificado) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    OpcionRadio(
                        texto = "Normal",
                        seleccionado = dpsNormal,
                        onClick = {
                            dpsNormal = true
                        }
                    )

                    OpcionRadio(
                        texto = "Novedad",
                        seleccionado = !dpsNormal,
                        onClick = {
                            dpsNormal = false
                        }
                    )
                }

                if (!dpsNormal) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    OutlinedTextField(
                        value = dpsNovedad,
                        onValueChange = {
                            dpsNovedad = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Describa la novedad del DPS")
                        },
                        placeholder = {
                            Text("Ej. Indicador de falla, DPS deteriorado...")
                        }
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // =================================================
            // 5. TORQUE
            // =================================================

            Text(
                text = "4. Verificación de torque",
                fontWeight = FontWeight.Bold,
                color = SolarPrimary
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Column {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    FilaCasilla(
                        texto = "Torque de tornillos de batería",
                        marcado = torqueBateria,
                        onCambio = {
                            torqueBateria = it
                        }
                    )
                }

                if (torqueBateria) {

                    Row(
                        modifier = Modifier.padding(start = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        OpcionRadio(
                            texto = "Conforme",
                            seleccionado = torqueBateriaConforme,
                            onClick = {
                                torqueBateriaConforme = true
                            }
                        )

                        OpcionRadio(
                            texto = "Novedad",
                            seleccionado = !torqueBateriaConforme,
                            onClick = {
                                torqueBateriaConforme = false
                            }
                        )
                    }

                    if (!torqueBateriaConforme) {

                        OutlinedTextField(
                            value = torqueBateriaNovedad,
                            onValueChange = {
                                torqueBateriaNovedad = it
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 48.dp),
                            label = {
                                Text("Describa la novedad")
                            },
                            placeholder = {
                                Text("Ej. Torque fuera de especificación...")
                            }
                        )
                    }
                }
            }

            Column {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    FilaCasilla(
                        texto = "Torque barraje positivo",
                        marcado = torqueBarrajePositivo,
                        onCambio = {
                            torqueBarrajePositivo = it
                        }
                    )
                }

                if (torqueBarrajePositivo) {

                    Row(
                        modifier = Modifier.padding(start = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        OpcionRadio(
                            texto = "Conforme",
                            seleccionado = torqueBarrajePositivoConforme,
                            onClick = {
                                torqueBarrajePositivoConforme = true
                            }
                        )

                        OpcionRadio(
                            texto = "Novedad",
                            seleccionado = !torqueBarrajePositivoConforme,
                            onClick = {
                                torqueBarrajePositivoConforme = false
                            }
                        )
                    }

                    if (!torqueBarrajePositivoConforme) {

                        OutlinedTextField(
                            value = torqueBarrajePositivoNovedad,
                            onValueChange = {
                                torqueBarrajePositivoNovedad = it
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 48.dp),
                            label = {
                                Text("Describa la novedad")
                            }
                        )
                    }
                }
            }

            Column {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    FilaCasilla(
                        texto = "Torque barraje negativo",
                        marcado = torqueBarrajeNegativo,
                        onCambio = {
                            torqueBarrajeNegativo = it
                        }
                    )
                }

                if (torqueBarrajeNegativo) {

                    Row(
                        modifier = Modifier.padding(start = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        OpcionRadio(
                            texto = "Conforme",
                            seleccionado = torqueBarrajeNegativoConforme,
                            onClick = {
                                torqueBarrajeNegativoConforme = true
                            }
                        )

                        OpcionRadio(
                            texto = "Novedad",
                            seleccionado = !torqueBarrajeNegativoConforme,
                            onClick = {
                                torqueBarrajeNegativoConforme = false
                            }
                        )
                    }

                    if (!torqueBarrajeNegativoConforme) {

                        OutlinedTextField(
                            value = torqueBarrajeNegativoNovedad,
                            onValueChange = {
                                torqueBarrajeNegativoNovedad = it
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 48.dp),
                            label = {
                                Text("Describa la novedad")
                            }
                        )
                    }
                }
            }
            Column {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    FilaCasilla(
                        texto = "Torque barraje neutro",
                        marcado = torqueBarrajeNeutro,
                        onCambio = {
                            torqueBarrajeNeutro = it
                        }
                    )
                }

                if (torqueBarrajeNeutro) {

                    Row(
                        modifier = Modifier.padding(start = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        OpcionRadio(
                            texto = "Conforme",
                            seleccionado = torqueBarrajeNeutroConforme,
                            onClick = {
                                torqueBarrajeNeutroConforme = true
                            }
                        )

                        OpcionRadio(
                            texto = "Novedad",
                            seleccionado = !torqueBarrajeNeutroConforme,
                            onClick = {
                                torqueBarrajeNeutroConforme = false
                            }
                        )
                    }

                    if (!torqueBarrajeNeutroConforme) {

                        OutlinedTextField(
                            value = torqueBarrajeNeutroNovedad,
                            onValueChange = {
                                torqueBarrajeNeutroNovedad = it
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 48.dp),
                            label = {
                                Text("Describa la novedad")
                            }
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ubicación para todas las fotografías", fontWeight = FontWeight.Bold, color = SolarPrimary)
                    Text(
                        when {
                            ubicacionGps?.provider == "manual" ->
                                "Coordenadas manuales confirmadas: ${latitudManualConfirmada}, ${longitudManualConfirmada}. Se marcan todas las fotos (sin precisión GPS)."
                            !ubicacionPermitida ->
                                "GPS sin permiso. Puedes ingresar las coordenadas manualmente."
                            ubicacionGpsReciente(ubicacionGps) ->
                                "GPS listo: ${ubicacionGps?.latitude}, ${ubicacionGps?.longitude}  ·  Precisión ±${String.format(java.util.Locale.getDefault(), "%.0f", ubicacionGps?.accuracy ?: 0f)} m"
                            else ->
                                "GPS buscando ubicación reciente. También puedes ingresar coordenadas manuales."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latitudManualConfirmada,
                            onValueChange = {
                                if (!coordenadasManualesConfirmadas) {
                                    latitudManualConfirmada = filtrarEntradaCoordenada(it)
                                    errorCoordenadasManual = null
                                }
                            },
                            readOnly = false,
                            enabled = !coordenadasManualesConfirmadas,
                            modifier = Modifier.weight(1f),
                            label = { Text("Latitud") },
                            placeholder = { Text("7.3345") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                        )
                        OutlinedTextField(
                            value = longitudManualConfirmada,
                            onValueChange = {
                                if (!coordenadasManualesConfirmadas) {
                                    longitudManualConfirmada = filtrarEntradaCoordenada(it)
                                    errorCoordenadasManual = null
                                }
                            },
                            readOnly = false,
                            enabled = !coordenadasManualesConfirmadas,
                            modifier = Modifier.weight(1f),
                            label = { Text("Longitud") },
                            placeholder = { Text("-76.1212") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                    if (coordenadasManualesConfirmadas) {
                        Text(
                            "Coordenada registrada y bloqueada. Pulsa CORREGIR COORDENADAS para modificarla y confirmarla de nuevo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SolarPrimary
                        )
                    }
                    Button(
                        onClick = {
                            if (coordenadasManualesConfirmadas) {
                                // Se desbloquea solo con esta acción: la coordenada guardada no se
                                // modifica escribiendo sobre ella, igual que al volver a firmar.
                                coordenadasManualesConfirmadas = false
                                if (ubicacionGps?.provider == "manual") {
                                    ubicacionGps = null
                                }
                                ubicacionPermitida = tienePermisoUbicacion(context)
                                errorCoordenadasManual = null
                            } else {
                                val formato = Regex("^-?\\d+\\.\\d{4}$")
                                val latitud = latitudManualConfirmada.toDoubleOrNull()
                                val longitud = longitudManualConfirmada.toDoubleOrNull()
                                if (!formato.matches(latitudManualConfirmada) || !formato.matches(longitudManualConfirmada) ||
                                    latitud == null || longitud == null || latitud !in -90.0..90.0 || longitud !in -180.0..180.0
                                ) {
                                    errorCoordenadasManual = "Ingrese coordenadas válidas con punto y 4 decimales."
                                } else {
                                    val manual = Location("manual").apply {
                                        latitude = latitud
                                        this.longitude = longitud
                                        accuracy = 0f
                                        time = System.currentTimeMillis()
                                    }
                                    coordenadasManualesConfirmadas = true
                                    ubicacionGps = manual
                                    ubicacionPermitida = true
                                    errorCoordenadasManual = null
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving && !procesandoFotoMedidor
                    ) {
                        Text(if (coordenadasManualesConfirmadas) "CORREGIR COORDENADAS" else "CONFIRMAR COORDENADAS")
                    }
                    errorCoordenadasManual?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }

            // =================================================
            // 6. MEDIDOR EXCELEC PPKW
            // =================================================

            Text(
                text = "5. Medidor de energía — Excelec PPKW",
                fontWeight = FontWeight.Bold,
                color = SolarPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Registre la lectura observada y su evidencia. La disponibilidad se calculará posteriormente en la plataforma web.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilaCasilla(
                    texto = "Medidor instalado / aplica",
                    marcado = medidorAplica,
                    onCambio = { aplica ->
                        medidorAplica = aplica
                        errorFormulario = null
                        if (!aplica) {
                            evidenciaLecturaPath?.let { File(it).delete() }
                            evidenciaCodigoPath?.let { File(it).delete() }
                            evidenciaLecturaPath = null
                            evidenciaCodigoPath = null
                            fechaHoraLectura = null
                            energiaConsumidaKwh = ""
                            codigoActivo = ""
                            medidorNormal = true
                        }
                    }
                )
            }

            if (medidorAplica) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Estado del medidor",
                    fontWeight = FontWeight.Medium
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OpcionRadio(
                        texto = "Normal",
                        seleccionado = medidorNormal,
                        onClick = {
                            medidorNormal = true
                            codigoActivo = ""
                            evidenciaCodigoPath?.let { File(it).delete() }
                            evidenciaCodigoPath = null
                            errorFormulario = null
                        }
                    )

                    OpcionRadio(
                        texto = "Código activo",
                        seleccionado = !medidorNormal,
                        onClick = {
                            medidorNormal = false
                            energiaConsumidaKwh = ""
                            evidenciaLecturaPath?.let { File(it).delete() }
                            evidenciaLecturaPath = null
                            fechaHoraLectura = null
                            errorFormulario = null
                        }
                    )
                }

                if (medidorNormal) {
                    OutlinedTextField(
                        value = energiaConsumidaKwh,
                        onValueChange = { entrada ->
                            energiaConsumidaKwh = entrada.filter {
                                it.isDigit() || it == '.' || it == ','
                            }
                            errorFormulario = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Energía consumida (kWh)") },
                        placeholder = { Text("Ej. 123.456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { iniciarCapturaFotoMedidor(esLectura = true) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving && !procesandoFotoMedidor
                    ) {
                        Text(
                            if (evidenciaLecturaPath == null) {
                                "TOMAR FOTO DE LA LECTURA"
                            } else {
                                "VOLVER A TOMAR FOTO DE LA LECTURA"
                            }
                        )
                    }

                    if (procesandoFotoMedidor) {
                        Text("Procesando fotografía y aplicando marca de agua…", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = if (evidenciaLecturaPath != null) {
                            "✓ Fotografía de lectura guardada en el dispositivo"
                        } else {
                            "La fotografía legible de la lectura es obligatoria."
                        },
                        color = if (evidenciaLecturaPath != null) {
                            SolarPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    OutlinedTextField(
                        value = codigoActivo,
                        onValueChange = {
                            codigoActivo = it
                            errorFormulario = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Código activo") },
                        placeholder = {
                            Text("Ingrese el código indicado en el medidor")
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { iniciarCapturaFotoMedidor(esLectura = false) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving && !procesandoFotoMedidor
                    ) {
                        Text(
                            if (evidenciaCodigoPath == null) {
                                "TOMAR FOTO DEL CÓDIGO ACTIVO"
                            } else {
                                "VOLVER A TOMAR FOTO DEL CÓDIGO"
                            }
                        )
                    }

                    if (procesandoFotoMedidor) {
                        Text("Procesando fotografía y aplicando marca de agua…", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = if (evidenciaCodigoPath != null) {
                            "✓ Fotografía del código guardada en el dispositivo"
                        } else {
                            "La fotografía del código activo es obligatoria."
                        },
                        color = if (evidenciaCodigoPath != null) {
                            SolarPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // =================================================
            // 6. EVIDENCIAS FOTOGRÁFICAS OBLIGATORIAS
            // =================================================
// =========================================================
// NOVEDAD: MEDIDOR NO INSTALADO
// =========================================================

            if (!medidorAplica) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "NOVEDAD: MEDIDOR NO INSTALADO",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "El medidor debe estar instalado. Registre el motivo de su ausencia.",
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = motivoMedidorNoInstalado,
                    onValueChange = {
                        motivoMedidorNoInstalado = it
                        errorFormulario = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Motivo por el que no está instalado")
                    },
                    placeholder = {
                        Text("Ej. El proyecto fue entregado sin medidor.")
                    },
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
            EvidenciasFotograficasSection(
                codigoSisfv = sistema.codigoSisfv,
                nui = sistema.nui,
                tecnicoNombre = tecnico.nombre,
                tecnicoId = tecnico.id,
                fotos = fotosCapturadas,
                onFotosChange = { nuevasFotos ->
                    fotosCapturadas = nuevasFotos
                },
                bombilloEncendido = bombilloEncendido,
                onBombilloEncendidoChange = { encendido ->
                    bombilloEncendido = encendido
                },
                ubicacionActual = ubicacionGps,
                ubicacionPermitida = ubicacionPermitida,
                onSolicitarPermisoUbicacion = {
                    solicitarPermisoUbicacionGps.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                guardando = uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
                medidorInstalado = medidorAplica,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // =================================================
            // 7. OBSERVACIONES
            // =================================================

            Text(
                text = "7. Observaciones y novedades",
                fontWeight = FontWeight.Bold,
                color = SolarPrimary
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            OutlinedTextField(
                value = observaciones,
                onValueChange = {
                    observaciones = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                label = {
                    Text("Observaciones")
                }
            )

            // =================================================
            // 8. DATOS DE RECIBIDO Y FIRMAS
            // =================================================
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "8. Recibido y firmas",
                fontWeight = FontWeight.Bold,
                color = SolarPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "La persona presente puede ser distinta del titular registrado. Identifique a quien recibe la visita y solicite ambas firmas.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = nombreReceptorFirma,
                onValueChange = { nombreReceptorFirma = capitalizarNombre(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre completo de quien recibe") },
                singleLine = true,
                enabled = receptorEsTitular != true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = documentoReceptorFirma,
                onValueChange = { documentoReceptorFirma = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Documento de identidad de quien recibe") },
                singleLine = true,
                enabled = receptorEsTitular != true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "¿La persona que recibe es el titular del SISFV?",
                fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OpcionRadio(
                    texto = "Sí",
                    seleccionado = receptorEsTitular == true,
                    onClick = {
                        receptorEsTitular = true
                        nombreReceptorFirma = ""
                        documentoReceptorFirma = ""
                        errorFormulario = null
                    }
                )
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                OpcionRadio(
                    texto = "No",
                    seleccionado = receptorEsTitular == false,
                    onClick = { receptorEsTitular = false }
                )
            }
            if (receptorEsTitular == true) {
                Text(
                    text = "Se registrarán los datos del titular del SISFV: ${sistema.nombreUsuario} · ${sistema.documentoUsuario ?: "documento no registrado"}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SolarPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            FirmaDigitalCanvas(
                titulo = "Firma de la persona que recibe",
                nombreArchivo = "firma_receptor",
                firmaGuardadaPath = firmaUsuarioPath,
                onFirmaGuardada = { nuevaRuta ->
                    firmaUsuarioPath?.let { anterior ->
                        if (anterior != nuevaRuta) File(anterior).delete()
                    }
                    firmaUsuarioPath = nuevaRuta
                    errorFormulario = null
                },
                onEliminarFirma = {
                    firmaUsuarioPath?.let { File(it).delete() }
                    firmaUsuarioPath = null
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            FirmaDigitalCanvas(
                titulo = "Firma del técnico: ${tecnico.nombre}",
                nombreArchivo = "firma_tecnico",
                firmaGuardadaPath = firmaTecnicoPath,
                onFirmaGuardada = { nuevaRuta ->
                    firmaTecnicoPath?.let { anterior ->
                        if (anterior != nuevaRuta) File(anterior).delete()
                    }
                    firmaTecnicoPath = nuevaRuta
                    errorFormulario = null
                },
                onEliminarFirma = {
                    firmaTecnicoPath?.let { File(it).delete() }
                    firmaTecnicoPath = null
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            // =================================================
            // GUARDAR
            // =================================================

            val mensajeError = errorFormulario ?: uiState.errorMessage
            if (mensajeError != null) {
                Text(
                    text = mensajeError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    errorFormulario = null

                    val voltajePanel = convertirNumero(voltajePanelDc)
                    val corrientePanel = convertirNumero(corrientePanelDc)
                    val voltajeBateria = convertirNumero(voltajeBateriaDc)
                    val corrienteBateria = convertirNumero(corrienteBateriaDc)
                    val voltajeInversor = convertirNumero(voltajeInversorAc)
                    val energiaKwh = convertirNumero(energiaConsumidaKwh)
                    val telefonoPropuesto = telefonoContactoPropuesto.trim()
                    val correoPropuesto = correoContactoPropuesto.trim()

                    when {
                        deseaActualizarContacto == null -> {
                            errorFormulario = "Indique si desea actualizar los datos de contacto del usuario."
                        }

                        deseaActualizarContacto == true && telefonoPropuesto.isEmpty() -> {
                            errorFormulario = "Ingrese el nuevo teléfono del usuario o indique que no desea actualizar los datos."
                        }

                        correoPropuesto.isNotEmpty() && telefonoPropuesto.isEmpty() -> {
                            errorFormulario = "Para solicitar un cambio de correo también debe ingresar el nuevo teléfono."
                        }

                        telefonoPropuesto.isNotEmpty() &&
                                (!telefonoPropuesto.matches(Regex("^[+0-9() .-]+$")) ||
                                        telefonoPropuesto.count(Char::isDigit) != 10) -> {
                            errorFormulario = "El teléfono debe tener 10 dígitos."
                        }

                        correoPropuesto.isNotEmpty() &&
                                !Patterns.EMAIL_ADDRESS.matcher(correoPropuesto).matches() -> {
                            errorFormulario = "Ingrese un correo electrónico válido."
                        }

                        voltajePanel == null || corrientePanel == null ||
                                voltajeBateria == null || corrienteBateria == null ||
                                voltajeInversor == null -> {
                            errorFormulario = "Complete todas las mediciones eléctricas con valores numéricos válidos. Use coma o punto decimal."
                        }

                        breakerVerificado && !breakerNormal && breakerNovedad.isBlank() -> {
                            errorFormulario = "Describa la novedad del breaker."
                        }

                        fusibleVerificado && !fusibleNormal && fusibleNovedad.isBlank() -> {
                            errorFormulario = "Describa la novedad de los fusibles."
                        }

                        dpsVerificado && !dpsNormal && dpsNovedad.isBlank() -> {
                            errorFormulario = "Describa la novedad del DPS."
                        }

                        torqueBateria && !torqueBateriaConforme && torqueBateriaNovedad.isBlank() -> {
                            errorFormulario = "Describa la novedad del torque de batería."
                        }

                        torqueBarrajePositivo && !torqueBarrajePositivoConforme && torqueBarrajePositivoNovedad.isBlank() -> {
                            errorFormulario = "Describa la novedad del torque del barraje positivo."
                        }

                        torqueBarrajeNegativo && !torqueBarrajeNegativoConforme && torqueBarrajeNegativoNovedad.isBlank() -> {
                            errorFormulario = "Describa la novedad del torque del barraje negativo."
                        }
                        torqueBarrajeNeutro && !torqueBarrajeNeutroConforme && torqueBarrajeNeutroNovedad.isBlank() -> {
                            errorFormulario = "Describa la novedad del torque del barraje neutro."
                        }
                        !medidorAplica && motivoMedidorNoInstalado.isBlank() -> {
                            errorFormulario =
                                "Indique el motivo por el que el medidor no está instalado."
                        }

                        medidorAplica && medidorNormal && (energiaKwh == null || energiaKwh < 0.0) -> {
                            errorFormulario = "Ingrese una lectura de energía consumida válida y mayor o igual a cero."
                        }

                        medidorAplica && medidorNormal && evidenciaLecturaPath == null -> {
                            errorFormulario = "Debe tomar la fotografía de respaldo de la lectura del medidor."
                        }

                        medidorAplica && !medidorNormal && codigoActivo.isBlank() -> {
                            errorFormulario = "Ingrese el código activo del medidor."
                        }

                        medidorAplica && !medidorNormal && evidenciaCodigoPath == null -> {
                            errorFormulario = "Debe tomar la fotografía de respaldo del código activo."
                        }

                        receptorEsTitular == null -> {
                            errorFormulario = "Indique si la persona que recibe es el titular del SISFV."
                        }

                        receptorEsTitular == false && nombreReceptorFirma.isBlank() -> {
                            errorFormulario = "Ingrese el nombre completo de la persona que recibe la visita."
                        }

                        receptorEsTitular == false && documentoReceptorFirma.isBlank() -> {
                            errorFormulario = "Ingrese el documento de identidad de la persona que recibe la visita."
                        }

                        receptorEsTitular == false &&
                                nombreReceptorFirma.trim().equals(sistema.nombreUsuario.trim(), ignoreCase = true) -> {
                            errorFormulario =
                                "El nombre de quien recibe no puede ser el del titular registrado. Si lo es, marque «Sí» como titular."
                        }

                        receptorEsTitular == false &&
                                !sistema.documentoUsuario.isNullOrBlank() &&
                                documentoReceptorFirma.trim() == sistema.documentoUsuario?.trim() -> {
                            errorFormulario =
                                "El documento de quien recibe no puede ser el del titular registrado. Si lo es, marque «Sí» como titular."
                        }

                        !firmaValida(firmaUsuarioPath) -> {
                            errorFormulario = "Debe guardar la firma de la persona que recibe la visita."
                        }

                        !firmaValida(firmaTecnicoPath) -> {
                            errorFormulario = "Debe guardar su firma como técnico responsable."
                        }

                        !ubicacionGpsReciente(ubicacionGps) -> {
                            errorFormulario = "La ubicación GPS ya no es reciente. Espera a que la tablet actualice la posición y vuelve a guardar."
                        }
                        fotosCapturadas.none {
                            it.codigoFoto == "MEDIDOR_GABINETE"
                        } -> {
                            errorFormulario =
                                "Debe tomar la fotografía 11 del medidor o de su ubicación prevista."
                        }
                            fotosCapturadas.size != 18 -> {
                            val faltantes = 18 - fotosCapturadas.size
                            errorFormulario = "Debe completar las 18 fotografías obligatorias. Faltan $faltantes."
                        }

                        bombilloEncendido == null -> {
                            errorFormulario = "Indique si el bombillo enciende o no durante la prueba."
                        }

                        else -> {
                            viewModel.guardarMantenimiento(
                                FormularioMantenimientoData(
                                    sistemaId = sistema.id,
                                    codigoSisfv = sistema.codigoSisfv,
                                    nombreUsuario = sistema.nombreUsuario,
                                    documentoUsuario = sistema.documentoUsuario,
                                    tecnicoId = tecnico.id,
                                    telefonoContactoPropuesto = telefonoPropuesto,
                                    correoContactoPropuesto = correoPropuesto,
                                    tipoMantenimiento = tipoMantenimiento,
                                    latitudGps = ubicacionGps!!.latitude,
                                    longitudGps = ubicacionGps!!.longitude,
                                    precisionGps = ubicacionGps!!.accuracy,
                                    voltajePanelDc = voltajePanel!!,
                                    corrientePanelDc = corrientePanel!!,
                                    voltajeBateriaDc = voltajeBateria!!,
                                    corrienteBateriaDc = corrienteBateria!!,
                                    voltajeInversorAc = voltajeInversor!!,

                                    breakerVerificado = breakerVerificado,
                                    breakerConforme = breakerNormal,
                                    breakerNovedad = breakerNovedad,

                                    fusibleVerificado = fusibleVerificado,
                                    fusibleConforme = fusibleNormal,
                                    fusibleNovedad = fusibleNovedad,

                                    dpsVerificado = dpsVerificado,
                                    dpsConforme = dpsNormal,
                                    dpsNovedad = dpsNovedad,

                                    torqueBateria = torqueBateria,
                                    torqueBateriaConforme = torqueBateriaConforme,
                                    torqueBateriaNovedad = torqueBateriaNovedad,

                                    torqueBarrajePositivo = torqueBarrajePositivo,
                                    torqueBarrajePositivoConforme = torqueBarrajePositivoConforme,
                                    torqueBarrajePositivoNovedad = torqueBarrajePositivoNovedad,

                                    torqueBarrajeNegativo = torqueBarrajeNegativo,
                                    torqueBarrajeNegativoConforme = torqueBarrajeNegativoConforme,
                                    torqueBarrajeNegativoNovedad = torqueBarrajeNegativoNovedad,

                                    torqueBarrajeNeutro = torqueBarrajeNeutro,
                                    torqueBarrajeNeutroConforme = torqueBarrajeNeutroConforme,
                                    torqueBarrajeNeutroNovedad = torqueBarrajeNeutroNovedad,

                                    medidorAplica = medidorAplica,
                                    medidorNormal = medidorNormal,
                                    codigoActivo = codigoActivo,

                                    observaciones = buildString {
                                        append(observaciones.trim())

                                        if (!medidorAplica) {
                                            if (isNotBlank()) {
                                                append("\n\n")
                                            }

                                            append("NOVEDAD MEDIDOR: NO INSTALADO. Motivo: ")
                                            append(motivoMedidorNoInstalado.trim())
                                        }
                                    },
                                    energiaConsumidaKwh = if (medidorAplica && medidorNormal) energiaKwh else null,
                                    evidenciaLecturaPath = if (medidorAplica && medidorNormal) evidenciaLecturaPath else null,
                                    evidenciaCodigoPath = if (medidorAplica && !medidorNormal) evidenciaCodigoPath else null,
                                    fechaHoraLectura = if (medidorAplica && medidorNormal) fechaHoraLectura else null,
                                    bombilloEncendido = bombilloEncendido,
                                    fotos = fotosCapturadas,
                                    nombreReceptorFirma = nombreReceptorFirma.trim(),
                                    documentoReceptorFirma = documentoReceptorFirma.trim(),
                                    receptorEsTitular = receptorEsTitular,
                                    firmaUsuarioPath = firmaUsuarioPath,
                                    firmaTecnicoPath = firmaTecnicoPath
                                )
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = "GUARDAR MANTENIMIENTO",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(30.dp)
            )
        }
    }
}

