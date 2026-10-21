package com.solaresenergy.sisfv.ui.galeria

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import com.solaresenergy.sisfv.ui.theme.SolarGreen
import com.solaresenergy.sisfv.ui.theme.SolarPrimary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GaleriaEvidenciasScreen(
    tecnico: TecnicoEntity,
    onVolver: () -> Unit,
    viewModel: GaleriaEvidenciasViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var busqueda by remember { mutableStateOf("") }
    var fotoAmpliada by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tecnico.id) { viewModel.cargarSistemasAsignados(tecnico.id) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    if (uiState.sistemaSeleccionado != null) viewModel.volverALista() else onVolver()
                }) { Text("VOLVER") }
                Text(
                    text = if (uiState.sistemaSeleccionado == null) "Galería de evidencias" else "Evidencias del SISFV",
                    style = MaterialTheme.typography.titleLarge,
                    color = SolarGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.sistemaSeleccionado == null) {
                Text(
                    "Selecciona un SISFV asignado para consultar sus fotos guardadas en este dispositivo.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar por NUI, código, nombre o documento") },
                    singleLine = true
                )
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                val sistemasFiltrados = uiState.sistemas.filter { sistema ->
                    val texto = busqueda.trim()
                    texto.isBlank() || listOfNotNull(
                        sistema.nui,
                        sistema.codigoSisfv,
                        sistema.nombreUsuario,
                        sistema.documentoUsuario
                    ).any { it.contains(texto, ignoreCase = true) }
                }
                sistemasFiltrados.forEach { sistema ->
                    SistemaGaleriaCard(sistema) {
                        viewModel.seleccionarSistema(sistema, tecnico.id)
                    }
                }
                if (!uiState.isLoading && sistemasFiltrados.isEmpty()) {
                    Text("No hay SISFV asignados que coincidan con la búsqueda.")
                }
            } else {
                val sistema = uiState.sistemaSeleccionado!!
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("NUI: ${sistema.nui ?: "No registrado"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SolarPrimary)
                        Text("Código SISFV: ${sistema.codigoSisfv}")
                        Text("Usuario: ${sistema.nombreUsuario}")
                        Text("Documento: ${sistema.documentoUsuario ?: "No registrado"}")
                    }
                }
                if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                uiState.grupos.forEach { grupo ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(grupo.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SolarPrimary)
                            Text(grupo.detalle, style = MaterialTheme.typography.bodySmall)
                            grupo.fotos.chunked(2).forEach { fila ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    fila.forEach { foto ->
                                        FotoGaleriaTile(
                                            foto = foto,
                                            modifier = Modifier.weight(1f),
                                            onClick = { if (foto.disponible) fotoAmpliada = foto.ruta }
                                        )
                                    }
                                    if (fila.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                if (!uiState.isLoading && uiState.grupos.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "No hay fotografías de mantenimiento o titularidad guardadas para este SISFV en el dispositivo.",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }

    fotoAmpliada?.let { ruta ->
        Dialog(onDismissRequest = { fotoAmpliada = null }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FotoGaleriaImagen(ruta, Modifier.fillMaxWidth().height(420.dp), ContentScale.Fit, 1600)
                    Button(onClick = { fotoAmpliada = null }, modifier = Modifier.fillMaxWidth()) { Text("CERRAR") }
                }
            }
        }
    }
}

@Composable
private fun SistemaGaleriaCard(sistema: SistemaSisfvEntity, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("NUI: ${sistema.nui ?: "No registrado"}", fontWeight = FontWeight.Bold, color = SolarPrimary)
            Text("${sistema.codigoSisfv} · ${sistema.nombreUsuario}")
            Text("Documento: ${sistema.documentoUsuario ?: "No registrado"}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FotoGaleriaTile(foto: FotoGaleria, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(enabled = foto.disponible, onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (foto.disponible) {
                FotoGaleriaImagen(foto.ruta, Modifier.fillMaxWidth().height(110.dp), ContentScale.Crop, 600)
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) { Text("Archivo no disponible", color = MaterialTheme.colorScheme.error) }
            }
            Text(foto.titulo, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(fechaLegible(foto.fecha), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun FotoGaleriaImagen(ruta: String, modifier: Modifier, contentScale: ContentScale, maxDimension: Int) {
    val estado by produceState<Pair<Boolean, Bitmap?>>(initialValue = false to null, key1 = ruta, key2 = maxDimension) {
        value = true to withContext(Dispatchers.IO) { decodificarMiniatura(ruta, maxDimension) }
    }
    val bitmap = estado.second
    if (bitmap != null) {
        Image(bitmap.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = contentScale)
    } else if (!estado.first) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
    } else {
        Text("No se pudo cargar la imagen", modifier = modifier.padding(8.dp), color = MaterialTheme.colorScheme.error)
    }
}

private fun decodificarMiniatura(ruta: String, maxDimension: Int): Bitmap? {
    val archivo = File(ruta)
    if (!archivo.isFile) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(ruta, bounds)
    var muestra = 1
    while (bounds.outWidth / muestra > maxDimension || bounds.outHeight / muestra > maxDimension) muestra *= 2
    return BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = muestra })
}

private fun fechaLegible(fecha: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(fecha))
