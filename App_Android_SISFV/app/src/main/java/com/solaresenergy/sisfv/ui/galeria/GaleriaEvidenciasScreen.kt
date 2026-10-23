package com.solaresenergy.sisfv.ui.galeria

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import com.solaresenergy.sisfv.ui.theme.SolarPrimary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val categoriasGaleria = listOf("Mantenimientos", "Cambios de titularidad", "Cambios de componentes")

@Composable
fun GaleriaEvidenciasScreen(
    tecnico: TecnicoEntity,
    onVolver: () -> Unit,
    viewModel: GaleriaEvidenciasViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var categoriaSeleccionada by rememberSaveable { mutableStateOf<String?>(null) }
    var busqueda by rememberSaveable { mutableStateOf("") }
    var fotoAmpliada by rememberSaveable { mutableStateOf<String?>(null) }
    fun retroceder() {
        when {
            fotoAmpliada != null -> fotoAmpliada = null
            categoriaSeleccionada != null -> { categoriaSeleccionada = null; busqueda = "" }
            else -> onVolver()
        }
    }
    BackHandler(onBack = ::retroceder)

    LaunchedEffect(tecnico.id) { viewModel.cargarSistemasAsignados(tecnico.id) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoriaSeleccionada ?: "Galería de fotografías",
                    style = MaterialTheme.typography.titleLarge,
                    color = SolarPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = ::retroceder) { Text(if (categoriaSeleccionada == null) "VOLVER" else "ATRÁS") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (categoriaSeleccionada == null) {
                Text("Selecciona el tipo de formulario para consultar sus evidencias.", style = MaterialTheme.typography.bodyMedium)
                if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                categoriasGaleria.forEach { categoria ->
                    val gruposCategoria = uiState.grupos.filter { it.categoria == categoria }
                    val cantidadUsuarios = gruposCategoria.distinctBy { it.sistemaId }.size
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            categoriaSeleccionada = categoria
                            busqueda = ""
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(categoria, fontWeight = FontWeight.Bold, color = SolarPrimary, modifier = Modifier.weight(1f))
                            Text("$cantidadUsuarios usuarios", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (!uiState.isLoading && uiState.grupos.isEmpty()) {
                    Text("No hay fotografías guardadas para los formularios asignados.")
                }
            } else {
                Text("Usuarios y fotografías del apartado seleccionado.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar usuario o SISFV") },
                    singleLine = true
                )
                if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                val texto = busqueda.trim()
                val gruposCategoria = uiState.grupos.filter { it.categoria == categoriaSeleccionada }
                val usuarios = gruposCategoria.groupBy { it.sistemaId }
                    .filterValues { gruposUsuario ->
                        texto.isBlank() || gruposUsuario.any { grupo ->
                            listOf(grupo.codigoSisfv, grupo.nombreUsuario, grupo.detalle)
                                .any { it.contains(texto, ignoreCase = true) }
                        }
                    }
                usuarios.values.forEach { gruposUsuario ->
                    val usuario = gruposUsuario.first()
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(usuario.nombreUsuario, fontWeight = FontWeight.Bold, color = SolarPrimary)
                            Text(usuario.codigoSisfv)
                            gruposUsuario.sortedByDescending { it.fecha }.forEach { grupo ->
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
                }
                if (!uiState.isLoading && usuarios.isEmpty()) {
                    Text("No hay usuarios con fotografías en esta categoría que coincidan con la búsqueda.")
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
