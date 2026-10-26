package com.solaresenergy.sisfv.ui.mantenimiento

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import com.solaresenergy.sisfv.ui.theme.SolarPrimary
import java.io.File

@Composable
fun CambioComponenteScreen(
    tecnico: TecnicoEntity,
    sistema: SistemaSisfvEntity,
    onVolver: () -> Unit,
    onContinuarMantenimiento: (SistemaSisfvEntity) -> Unit,
    viewModel: CambioComponenteViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val componentes = listOf("Paneles", "Controlador", "Inversor", "Medidor", "Batería")
    var componente by rememberSaveable(sistema.id) { mutableStateOf("") }
    var causa by rememberSaveable(sistema.id) { mutableStateOf("") }
    var nombreReceptor by rememberSaveable(sistema.id) { mutableStateOf("") }
    var documentoReceptor by rememberSaveable(sistema.id) { mutableStateOf("") }
    var receptorEsTitular by rememberSaveable(sistema.id) { mutableStateOf<Boolean?>(null) }
    var fotoMalo by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var serialMalo by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var fotoNuevo by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var serialNuevo by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var segundoPanelMalo by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var segundoSerialMalo by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var segundoPanelNuevo by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var segundoSerialNuevo by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var firmaUsuario by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var firmaTecnico by rememberSaveable(sistema.id) { mutableStateOf<String?>(null) }
    var mostrarConfirmacionSalida by rememberSaveable(sistema.id) { mutableStateOf(false) }
    var ubicacionPermitida by remember { mutableStateOf(tienePermisoUbicacionComponente(context)) }
    var ubicacionGps by remember { mutableStateOf<Location?>(null) }
    var mostrarConfirmacion by rememberSaveable(sistema.id) { mutableStateOf(false) }

    val solicitarPermisoUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        ubicacionPermitida = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true || tienePermisoUbicacionComponente(context)
    }

    DisposableEffect(context, ubicacionPermitida) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var listener: LocationListener? = null
        if (ubicacionPermitida) {
            val receptor = object : LocationListener {
                override fun onLocationChanged(location: Location) { ubicacionGps = location }
                @Deprecated("Deprecated by Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }
            listener = receptor
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
                try {
                    manager.getLastKnownLocation(provider)?.let { current ->
                        if (ubicacionGps == null || current.time > ubicacionGps!!.time) ubicacionGps = current
                    }
                    if (manager.isProviderEnabled(provider)) {
                        manager.requestLocationUpdates(provider, 2_000L, 0f, receptor, Looper.getMainLooper())
                    }
                } catch (_: SecurityException) { }
            }
        }
        onDispose { try { listener?.let(manager::removeUpdates) } catch (_: Exception) { } }
    }

    LaunchedEffect(sistema.id) { viewModel.limpiarEstado() }
    LaunchedEffect(uiState.saveSuccess) { if (uiState.saveSuccess) mostrarConfirmacion = true }
    BackHandler { if (!uiState.isSaving && !mostrarConfirmacion) mostrarConfirmacionSalida = true }

    if (mostrarConfirmacionSalida) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionSalida = false },
            title = { Text("Confirmar salida") },
            text = { Text("¿Está seguro de salir? Eso eliminará el formulario que se está diligenciando.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacionSalida = false
                    listOfNotNull(
                        fotoMalo, serialMalo, fotoNuevo, serialNuevo,
                        segundoPanelMalo, segundoSerialMalo, segundoPanelNuevo, segundoSerialNuevo,
                        firmaUsuario, firmaTecnico
                    ).distinct().forEach { File(it).delete() }
                    onVolver()
                }) { Text("SALIR SIN GUARDAR") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionSalida = false }) { Text("CONTINUAR DILIGENCIANDO") }
            }
        )
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Cambio de componente guardado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Recuerde cerrar PQR con visita de mantenimiento correctivo.")
                    Text("Debe realizar el mantenimiento correctivo de inmediato.", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { onContinuarMantenimiento(sistema) }) { Text("CONTINUAR") }
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cambio de componente", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = SolarPrimary, modifier = Modifier.weight(1f))
                TextButton(onClick = { mostrarConfirmacionSalida = true }, enabled = !uiState.isSaving && !mostrarConfirmacion) { Text("VOLVER") }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("SISFV seleccionado", fontWeight = FontWeight.Bold, color = SolarPrimary)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(sistema.codigoSisfv, fontWeight = FontWeight.Bold, color = SolarPrimary)
                    Text(sistema.nombreUsuario, fontWeight = FontWeight.Bold)
                    Text("Documento: ${sistema.documentoUsuario ?: "No registrado"}")
                    Text("NUI: ${sistema.nui ?: "No registrado"}")
                    sistema.comunidad?.let { Text("Comunidad: $it") }
                    sistema.vereda?.let { Text("Vereda: $it") }
                    Text("Proyecto ID: ${sistema.proyectoId}")
                    Text("Técnico: ${tecnico.nombre}")
                }
            }

            HorizontalDivider()

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("1. Componente mayor", style = MaterialTheme.typography.titleMedium, color = SolarPrimary, fontWeight = FontWeight.Bold)
                    componentes.forEach { opcion ->
                        OpcionRadio(opcion, seleccionado = componente == opcion, onClick = { componente = opcion })
                    }
                }
            }

            OutlinedTextField(
                value = causa, onValueChange = { causa = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Indique causa del fallo") }, minLines = 3, maxLines = 5
            )

            Text("2. Evidencias fotográficas obligatorias", style = MaterialTheme.typography.titleMedium, color = SolarPrimary, fontWeight = FontWeight.Bold)
            Text(
                when {
                    !ubicacionPermitida -> "Permite la ubicación para añadir coordenadas GPS a cada fotografía."
                    ubicacionGps != null && ubicacionGps?.let(::ubicacionGpsReciente) == true -> "GPS listo: ±${ubicacionGps?.accuracy?.toInt()} m"
                    else -> "GPS buscando una ubicación reciente."
                }, style = MaterialTheme.typography.bodySmall
            )
            val pedirUbicacion = {
                solicitarPermisoUbicacion.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
            val esPaneles = componente == "Paneles"
            CapturaFotoTitularidad(if (esPaneles) "Panel viejo 1" else "Componente malo", if (esPaneles) "panel_viejo_1" else "componente_malo", fotoMalo, sistema.codigoSisfv, sistema.nui, tecnico, ubicacionGps, ubicacionPermitida, pedirUbicacion, "cambio_componente") { path, _ -> fotoMalo = path }
            CapturaFotoTitularidad(if (esPaneles) "Serial del panel viejo 1" else "Serial del componente malo", if (esPaneles) "serial_panel_viejo_1" else "serial_componente_malo", serialMalo, sistema.codigoSisfv, sistema.nui, tecnico, ubicacionGps, ubicacionPermitida, pedirUbicacion, "cambio_componente") { path, _ -> serialMalo = path }
            CapturaFotoTitularidad(if (esPaneles) "Panel nuevo 1" else "Componente nuevo", if (esPaneles) "panel_nuevo_1" else "componente_nuevo", fotoNuevo, sistema.codigoSisfv, sistema.nui, tecnico, ubicacionGps, ubicacionPermitida, pedirUbicacion, "cambio_componente") { path, _ -> fotoNuevo = path }
            CapturaFotoTitularidad(if (esPaneles) "Serial del panel nuevo 1" else "Serial del componente nuevo", if (esPaneles) "serial_panel_nuevo_1" else "serial_componente_nuevo", serialNuevo, sistema.codigoSisfv, sistema.nui, tecnico, ubicacionGps, ubicacionPermitida, pedirUbicacion, "cambio_componente") { path, _ -> serialNuevo = path }
            if (esPaneles) {
                CapturaFotoTitularidad("Panel viejo 2", "panel_viejo_2", segundoPanelMalo, sistema.codigoSisfv, sistema.nui, tecnico, ubicacionGps, ubicacionPermitida, pedirUbicacion, "cambio_componente") { path, _ -> segundoPanelMalo = path }
                CapturaFotoTitularidad("Serial del panel viejo 2", "serial_panel_viejo_2", segundoSerialMalo, sistema.codigoSisfv, sistema.nui, tecnico, ubicacionGps, ubicacionPermitida, pedirUbicacion, "cambio_componente") { path, _ -> segundoSerialMalo = path }
                CapturaFotoTitularidad("Panel nuevo 2", "panel_nuevo_2", segundoPanelNuevo, sistema.codigoSisfv, sistema.nui, tecnico, ubicacionGps, ubicacionPermitida, pedirUbicacion, "cambio_componente") { path, _ -> segundoPanelNuevo = path }
                CapturaFotoTitularidad("Serial del panel nuevo 2", "serial_panel_nuevo_2", segundoSerialNuevo, sistema.codigoSisfv, sistema.nui, tecnico, ubicacionGps, ubicacionPermitida, pedirUbicacion, "cambio_componente") { path, _ -> segundoSerialNuevo = path }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3. Responsable de la visita", style = MaterialTheme.typography.titleMedium, color = SolarPrimary, fontWeight = FontWeight.Bold)
                    Text("La persona presente puede ser distinta del titular registrado. Identifique a quien recibe la visita.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(nombreReceptor, { nombreReceptor = it }, Modifier.fillMaxWidth(), label = { Text("Nombre completo") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), singleLine = true)
                    OutlinedTextField(documentoReceptor, { documentoReceptor = it }, Modifier.fillMaxWidth(), label = { Text("Número de documento") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    Text("¿La persona que recibe es el titular del SISFV?")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OpcionRadio("Sí", seleccionado = receptorEsTitular == true, onClick = { receptorEsTitular = true })
                        OpcionRadio("No", seleccionado = receptorEsTitular == false, onClick = { receptorEsTitular = false })
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("4. Firmas", style = MaterialTheme.typography.titleMedium, color = SolarPrimary, fontWeight = FontWeight.Bold)
                    Text("La firma del usuario confirma que se encuentra conforme con el servicio prestado y funcionamiento.", style = MaterialTheme.typography.bodySmall)
                    FirmaDigitalCanvas("Conformidad del usuario", "cambio_componente_usuario_${sistema.id}", firmaUsuario, { firmaUsuario = it }, { firmaUsuario = null })
                    FirmaDigitalCanvas("Firma del técnico que ejecutó el cambio", "cambio_componente_tecnico_${sistema.id}", firmaTecnico, { firmaTecnico = it }, { firmaTecnico = null })
                }
            }

            uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    viewModel.guardar(sistema, tecnico, CambioComponenteData(
                        componente, causa, fotoMalo, serialMalo, fotoNuevo, serialNuevo,
                        segundoPanelMalo, segundoSerialMalo, segundoPanelNuevo, segundoSerialNuevo,
                        nombreReceptor, documentoReceptor, receptorEsTitular, firmaUsuario, firmaTecnico
                    ))
                }, enabled = !uiState.isSaving, modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (uiState.isSaving) "GUARDANDO..." else "CERRAR CAMBIO DE COMPONENTE") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun tienePermisoUbicacionComponente(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
