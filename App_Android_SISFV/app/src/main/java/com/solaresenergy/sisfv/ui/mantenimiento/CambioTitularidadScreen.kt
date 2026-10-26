package com.solaresenergy.sisfv.ui.mantenimiento

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import com.solaresenergy.sisfv.ui.theme.SolarGreen
import com.solaresenergy.sisfv.ui.theme.SolarPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
fun CambioTitularidadScreen(
    tecnico: TecnicoEntity,
    sistema: SistemaSisfvEntity,
    onVolver: () -> Unit,
    onGuardar: () -> Unit,
    viewModel: CambioTitularidadViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var ubicacionPermitida by remember { mutableStateOf(tienePermisoUbicacionTitularidad(context)) }
    var ubicacionGps by remember { mutableStateOf<Location?>(null) }
    var coordenadasManualesConfirmadas by rememberSaveable(sistema.id) { mutableStateOf(false) }
    var latitudManual by rememberSaveable(sistema.id) { mutableStateOf("") }
    var longitudManual by rememberSaveable(sistema.id) { mutableStateOf("") }
    var errorUbicacion by rememberSaveable { mutableStateOf<String?>(null) }

    val solicitarPermisoUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        ubicacionPermitida = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            tienePermisoUbicacionTitularidad(context)
        errorUbicacion = if (ubicacionPermitida) {
            "Permiso GPS concedido. Espera una ubicación reciente antes de tomar la foto."
        } else {
            "Debes permitir la ubicación para incluir coordenadas en las fotografías."
        }
    }

    DisposableEffect(context, ubicacionPermitida) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var listener: LocationListener? = null
        if (ubicacionPermitida) {
            val receptor = object : LocationListener {
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
            listener = receptor
            try {
                val proveedores = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                proveedores.mapNotNull { proveedor ->
                    try { manager.getLastKnownLocation(proveedor) } catch (_: Exception) { null }
                }.maxByOrNull { it.time }?.let { if (!coordenadasManualesConfirmadas) ubicacionGps = it }
                proveedores.forEach { proveedor ->
                    try {
                        if (manager.isProviderEnabled(proveedor)) {
                            manager.requestLocationUpdates(proveedor, 2_000L, 0f, receptor, Looper.getMainLooper())
                        }
                    } catch (_: Exception) { }
                }
            } catch (_: Exception) {
                errorUbicacion = "No fue posible iniciar el GPS. Verifica que la ubicación esté activada."
            }
        }
        onDispose {
            try { listener?.let { manager.removeUpdates(it) } } catch (_: Exception) { }
        }
    }

    var claveCaptura by rememberSaveable(sistema.id) { mutableStateOf(UUID.randomUUID().toString()) }

    var nombreNuevo by rememberSaveable(sistema.id) { mutableStateOf("") }
    var documentoNuevo by rememberSaveable(sistema.id) { mutableStateOf("") }
    var telefonoNuevo by rememberSaveable(sistema.id) { mutableStateOf("") }
    var correoNuevo by rememberSaveable(sistema.id) { mutableStateOf("") }
    var motivo by rememberSaveable(sistema.id) { mutableStateOf("") }
    var fotoFrente by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var fotoReverso by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var fotoHuella by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var firmaTecnico by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var firmaNuevoUsuario by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var mostrarConfirmacionSalida by rememberSaveable(sistema.id) { mutableStateOf(false) }

    LaunchedEffect(coordenadasManualesConfirmadas, latitudManual, longitudManual) {
        if (coordenadasManualesConfirmadas && ubicacionGps?.provider != "manual") {
            val latitud = latitudManual.toDoubleOrNull()
            val longitud = longitudManual.toDoubleOrNull()
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

    LaunchedEffect(sistema.id) {
        viewModel.limpiarEstado()
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Toast.makeText(context, "Solicitud guardada localmente y pendiente de revisión.", Toast.LENGTH_LONG).show()
            onGuardar()
        }
    }

    fun eliminarBorrador() {
        listOf(fotoFrente, fotoReverso, fotoHuella, firmaTecnico, firmaNuevoUsuario)
            .filterNotNull()
            .forEach { File(it).delete() }
        onVolver()
    }

    BackHandler { if (!uiState.isSaving) mostrarConfirmacionSalida = true }

    if (mostrarConfirmacionSalida) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { mostrarConfirmacionSalida = false },
            title = { Text("Confirmar salida") },
            text = { Text("¿Está seguro de salir? Eso eliminará el formulario que se está diligenciando.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacionSalida = false
                    eliminarBorrador()
                }) { Text("SALIR SIN GUARDAR") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionSalida = false }) { Text("CONTINUAR DILIGENCIANDO") }
            }
        )
    }

    Scaffold(
        topBar = {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cambio de titularidad",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SolarGreen,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { mostrarConfirmacionSalida = true }, enabled = !uiState.isSaving) { Text("VOLVER") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "La solicitud se guardará en este dispositivo para revisión. No modifica el titular maestro del SISFV.",
                style = MaterialTheme.typography.bodySmall
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("Usuario actual en la base local", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SolarPrimary)
                    Text("Código SISFV: ${sistema.codigoSisfv}")
                    Text("Nombre: ${sistema.nombreUsuario}")
                    Text("Documento: ${sistema.documentoUsuario ?: "No registrado"}")
                    sistema.nui?.let { Text("NUI: $it") }
                    Text("Contrato: ${sistema.contratoId} · Proyecto: ${sistema.proyectoId}")
                    Text(
                        "El registro maestro local no contiene teléfono ni correo para comparar.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. Datos del nuevo usuario", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SolarPrimary)
                    HorizontalDivider()
                    OutlinedTextField(value = nombreNuevo, onValueChange = { nombreNuevo = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Nombre completo") }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                    OutlinedTextField(value = documentoNuevo, onValueChange = { documentoNuevo = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Número de documento") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next))
                    OutlinedTextField(value = telefonoNuevo, onValueChange = { telefonoNuevo = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Número de teléfono") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next))
                    OutlinedTextField(value = correoNuevo, onValueChange = { correoNuevo = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Correo electrónico (opcional)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
                    OutlinedTextField(value = motivo, onValueChange = { motivo = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Motivo del cambio de titularidad") }, minLines = 3)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("2. Evidencias fotográficas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SolarPrimary)
                    HorizontalDivider()
                    Text("Las fotos incluirán marca de agua con código SISFV, técnico, fecha y ubicación GPS. La huella debe estar sobre papel blanco.", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = when {
                            ubicacionGps?.provider == "manual" -> "Coordenadas manuales confirmadas."
                            !ubicacionPermitida -> "GPS: permiso pendiente."
                            ubicacionGpsReciente(ubicacionGps) -> "GPS listo · ±${ubicacionGps?.accuracy?.let { String.format(java.util.Locale.getDefault(), "%.0f", it) } ?: "?"} m"
                            else -> "GPS buscando ubicación reciente…"
                        },
                        color = if (ubicacionGpsReciente(ubicacionGps)) SolarPrimary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latitudManual,
                            onValueChange = {
                                latitudManual = filtrarEntradaCoordenada(it)
                                if (coordenadasManualesConfirmadas) {
                                    coordenadasManualesConfirmadas = false
                                    ubicacionGps = null
                                    ubicacionPermitida = tienePermisoUbicacionTitularidad(context)
                                }
                                errorUbicacion = null
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Latitud") },
                            placeholder = { Text("7.3345") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                        )
                        OutlinedTextField(
                            value = longitudManual,
                            onValueChange = {
                                longitudManual = filtrarEntradaCoordenada(it)
                                if (coordenadasManualesConfirmadas) {
                                    coordenadasManualesConfirmadas = false
                                    ubicacionGps = null
                                    ubicacionPermitida = tienePermisoUbicacionTitularidad(context)
                                }
                                errorUbicacion = null
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Longitud") },
                            placeholder = { Text("-76.1212") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                    Button(
                        onClick = {
                            val formato = Regex("^-?\\d+\\.\\d{4}$")
                            val latitud = latitudManual.toDoubleOrNull()
                            val longitud = longitudManual.toDoubleOrNull()
                            if (!formato.matches(latitudManual) || !formato.matches(longitudManual) ||
                                latitud == null || longitud == null || latitud !in -90.0..90.0 || longitud !in -180.0..180.0
                            ) {
                                errorUbicacion = "Ingrese latitud y longitud válidas con punto y 4 decimales."
                            } else {
                                ubicacionGps = Location("manual").apply {
                                    latitude = latitud
                                    this.longitude = longitud
                                    accuracy = 0f
                                    time = System.currentTimeMillis()
                                }
                                coordenadasManualesConfirmadas = true
                                ubicacionPermitida = true
                                errorUbicacion = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (coordenadasManualesConfirmadas) "COORDENADAS CONFIRMADAS" else "CONFIRMAR COORDENADAS") }
                    if (!ubicacionPermitida || !ubicacionGpsReciente(ubicacionGps)) {
                        OutlinedButton(
                            onClick = {
                                if (!ubicacionPermitida) {
                                    solicitarPermisoUbicacion.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                } else {
                                    errorUbicacion = "Activa la ubicación y espera a que el dispositivo obtenga una posición GPS reciente."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (ubicacionPermitida) "ACTUALIZAR UBICACIÓN" else "PERMITIR UBICACIÓN") }
                    }
                    errorUbicacion?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }

            CapturaFotoTitularidad("Documento del nuevo usuario · frente", "doc_frente_$claveCaptura", fotoFrente, sistema.codigoSisfv, sistema.nui, tecnico, ubicacionGps, ubicacionPermitida, {
                solicitarPermisoUbicacion.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }, mostrarCoordenadasManuales = false) { ruta, _ -> fotoFrente?.takeIf { it != ruta }?.let { File(it).delete() }; fotoFrente = ruta }
            CapturaFotoTitularidad("Documento del nuevo usuario · reverso", "doc_reverso_$claveCaptura", fotoReverso, sistema.codigoSisfv, sistema.nui, tecnico, ubicacionGps, ubicacionPermitida, {
                solicitarPermisoUbicacion.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }, mostrarCoordenadasManuales = false) { ruta, _ -> fotoReverso?.takeIf { it != ruta }?.let { File(it).delete() }; fotoReverso = ruta }
            CapturaFotoTitularidad("Huella dactilar sobre papel blanco", "huella_$claveCaptura", fotoHuella, sistema.codigoSisfv, sistema.nui, tecnico, ubicacionGps, ubicacionPermitida, {
                solicitarPermisoUbicacion.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }, mostrarCoordenadasManuales = false) { ruta, _ -> fotoHuella?.takeIf { it != ruta }?.let { File(it).delete() }; fotoHuella = ruta }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("3. Firmas obligatorias", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SolarPrimary)
                    HorizontalDivider()
                    FirmaDigitalCanvas(
                        titulo = "Firma del nuevo usuario",
                        nombreArchivo = "titularidad_${claveCaptura}_usuario",
                        firmaGuardadaPath = firmaNuevoUsuario,
                        onFirmaGuardada = { firmaNuevoUsuario = it },
                        onEliminarFirma = { firmaNuevoUsuario?.let { File(it).delete() }; firmaNuevoUsuario = null }
                    )
                    FirmaDigitalCanvas(
                        titulo = "Firma del técnico responsable · ${tecnico.nombre}",
                        nombreArchivo = "titularidad_${claveCaptura}_tecnico",
                        firmaGuardadaPath = firmaTecnico,
                        onFirmaGuardada = { firmaTecnico = it },
                        onEliminarFirma = { firmaTecnico?.let { File(it).delete() }; firmaTecnico = null }
                    )
                }
            }

            uiState.errorMessage?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    viewModel.guardarSolicitud(
                        sistemaSeleccionado = sistema,
                        tecnico = tecnico,
                        datos = CambioTitularidadData(
                            nombreNuevo = nombreNuevo,
                            documentoNuevo = documentoNuevo,
                            telefonoNuevo = telefonoNuevo,
                            correoNuevo = correoNuevo,
                            motivo = motivo,
                            fotoDocumentoFrentePath = fotoFrente,
                            fotoDocumentoReversoPath = fotoReverso,
                            fotoHuellaPath = fotoHuella,
                            firmaTecnicoPath = firmaTecnico,
                            firmaNuevoUsuarioPath = firmaNuevoUsuario
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = SolarGreen),
                enabled = !uiState.isSaving
            ) {
                Text(if (uiState.isSaving) "GUARDANDO..." else "GUARDAR SOLICITUD")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun CapturaFotoTitularidad(
    titulo: String,
    nombreArchivo: String,
    rutaGuardada: String?,
    codigoSisfv: String,
    nui: String?,
    tecnico: TecnicoEntity,
    ubicacionActual: Location?,
    ubicacionPermitida: Boolean,
    onSolicitarPermisoUbicacion: () -> Unit,
    subdirectorio: String = "cambio_titularidad",
    mostrarCoordenadasManuales: Boolean = true,
    onFotoCapturada: (String, Location) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var camaraPermitida by remember { mutableStateOf(tienePermisoCamaraTitularidad(context)) }
    var rutaPendiente by remember { mutableStateOf<String?>(null) }
    var ubicacionPendiente by remember { mutableStateOf<Location?>(null) }
    var procesando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var latitudManual by rememberSaveable(codigoSisfv, nombreArchivo) { mutableStateOf("") }
    var longitudManual by rememberSaveable(codigoSisfv, nombreArchivo) { mutableStateOf("") }
    var coordenadasManualesConfirmadas by rememberSaveable(codigoSisfv, nombreArchivo) { mutableStateOf(false) }
    val ubicacionManual = remember(coordenadasManualesConfirmadas, latitudManual, longitudManual) {
        if (!coordenadasManualesConfirmadas) null else {
            val latitud = latitudManual.toDoubleOrNull()
            val longitud = longitudManual.toDoubleOrNull()
            if (latitud == null || longitud == null) null else Location("manual").apply {
                latitude = latitud
                this.longitude = longitud
                accuracy = 0f
                time = System.currentTimeMillis()
            }
        }
    }

    val solicitarPermisoCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        camaraPermitida = concedido
        error = if (concedido) "Permiso de cámara concedido. Pulsa nuevamente para tomar la foto." else "Se requiere acceso a la cámara."
    }

    val tomarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val ruta = rutaPendiente
        val ubicacion = ubicacionPendiente
        rutaPendiente = null
        ubicacionPendiente = null
        val archivoTemporal = ruta?.let(::File)
        if (!exito || archivoTemporal == null || ubicacion == null || !archivoTemporal.isFile || archivoTemporal.length() == 0L) {
            archivoTemporal?.delete()
            error = "No se pudo guardar la foto o no había una ubicación GPS asociada."
        } else {
            procesando = true
            scope.launch {
                try {
                    val rutaMarcada = withContext(Dispatchers.IO) {
                        val fechaCaptura = archivoTemporal.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
                        val directorio = File(context.filesDir, "evidencias_sisfv/$subdirectorio")
                        if (!directorio.exists() && !directorio.mkdirs()) {
                            throw IllegalStateException("No se pudo crear la carpeta definitiva de evidencias.")
                        }
                        val destino = File(directorio, "${nombreArchivo}_${fechaCaptura}_marcada.jpg")
                        generarFotoConMarcaAgua(
                            context = context,
                            rutaCapturaTemporal = archivoTemporal.absolutePath,
                            rutaFinal = destino.absolutePath,
                            codigoSisfv = codigoSisfv,
                            nui = nui,
                            tecnicoNombre = tecnico.nombre,
                            tecnicoId = tecnico.id,
                            tituloFoto = titulo,
                            fechaCaptura = fechaCaptura,
                            latitud = ubicacion.latitude,
                            longitud = ubicacion.longitude,
                            precision = ubicacion.accuracy,
                            coordenadasManuales = ubicacion.provider == "manual"
                        )
                        if (!destino.isFile || destino.length() == 0L) {
                            throw IllegalStateException("La foto marcada no se pudo guardar.")
                        }
                        destino.absolutePath
                    }
                    error = null
                    onFotoCapturada(rutaMarcada, Location(ubicacion))
                } catch (e: Exception) {
                    error = "No se pudo aplicar la marca de agua: ${e.localizedMessage ?: "error de imagen"}"
                } finally {
                    archivoTemporal.delete()
                    procesando = false
                }
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(titulo, fontWeight = FontWeight.SemiBold)
            Text(
                if (rutaGuardada?.let { File(it).isFile } == true) "✓ Foto marcada guardada" else "Fotografía obligatoria pendiente",
                color = if (rutaGuardada?.let { File(it).isFile } == true) SolarPrimary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = when {
                    ubicacionManual != null || ubicacionActual?.provider == "manual" -> "Coordenadas manuales confirmadas para esta foto."
                    ubicacionGpsReciente(ubicacionActual) -> "GPS listo · ${ubicacionActual?.latitude}, ${ubicacionActual?.longitude}"
                    else -> "Ingresa y confirma las coordenadas antes de tomar la fotografía, o espera una ubicación GPS reciente."
                },
                style = MaterialTheme.typography.bodySmall
            )
            if (mostrarCoordenadasManuales) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latitudManual,
                    onValueChange = { latitudManual = filtrarEntradaCoordenada(it); coordenadasManualesConfirmadas = false },
                    modifier = Modifier.weight(1f),
                    label = { Text("Latitud") },
                    placeholder = { Text("7.3345") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = longitudManual,
                    onValueChange = { longitudManual = filtrarEntradaCoordenada(it); coordenadasManualesConfirmadas = false },
                    modifier = Modifier.weight(1f),
                    label = { Text("Longitud") },
                    placeholder = { Text("-76.1212") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
                Button(
                onClick = {
                    val formato = Regex("^-?\\d+\\.\\d{4}$")
                    val latitud = latitudManual.toDoubleOrNull()
                    val longitud = longitudManual.toDoubleOrNull()
                    if (!formato.matches(latitudManual) || !formato.matches(longitudManual) ||
                        latitud == null || longitud == null || latitud !in -90.0..90.0 || longitud !in -180.0..180.0
                    ) {
                        error = "Ingrese latitud y longitud válidas con punto y 4 decimales."
                    } else {
                        coordenadasManualesConfirmadas = true
                        error = null
                    }
                },
                modifier = Modifier.fillMaxWidth()
                ) { Text(if (ubicacionManual != null) "COORDENADAS CONFIRMADAS" else "CONFIRMAR COORDENADAS") }
            }
            Button(
                onClick = {
                    error = null
                    val ubicacionCaptura = ubicacionManual ?: ubicacionActual
                    if (ubicacionManual == null && !ubicacionPermitida) {
                        onSolicitarPermisoUbicacion()
                        error = "Autoriza la ubicación GPS o confirma coordenadas manuales antes de tomar la foto."
                    } else if (ubicacionManual == null && !ubicacionGpsReciente(ubicacionActual)) {
                        error = "No hay una ubicación GPS reciente. Activa la ubicación y espera una posición antes de tomar la foto."
                    } else if (!camaraPermitida) {
                        solicitarPermisoCamara.launch(Manifest.permission.CAMERA)
                    } else {
                        try {
                            val directorio = File(context.cacheDir, "evidencias_sisfv_tmp")
                            if (!directorio.exists() && !directorio.mkdirs()) {
                                throw IllegalStateException("No se pudo crear la carpeta temporal de evidencias.")
                            }
                            val archivo = File(directorio, "${nombreArchivo}_${System.currentTimeMillis()}_captura.jpg")
                            if (!archivo.createNewFile()) throw IllegalStateException("No se pudo preparar el archivo de foto.")
                            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
                            rutaPendiente = archivo.absolutePath
                            ubicacionPendiente = Location(ubicacionCaptura!!)
                            tomarFoto.launch(uri)
                        } catch (e: Exception) {
                            rutaPendiente?.let { File(it).delete() }
                            rutaPendiente = null
                            ubicacionPendiente = null
                            error = e.message ?: "No se pudo abrir la cámara."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = SolarGreen),
                enabled = !procesando
            ) {
                Text(when {
                    procesando -> "APLICANDO MARCA DE AGUA…"
                    rutaGuardada?.let { File(it).isFile } == true -> "VOLVER A TOMAR FOTO"
                    else -> "TOMAR FOTO"
                })
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun tienePermisoCamaraTitularidad(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED



private fun tienePermisoUbicacionTitularidad(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
