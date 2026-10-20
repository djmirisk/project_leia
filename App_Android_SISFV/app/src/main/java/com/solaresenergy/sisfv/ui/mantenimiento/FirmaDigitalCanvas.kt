package com.solaresenergy.sisfv.ui.mantenimiento

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun FirmaDigitalCanvas(
    titulo: String,
    nombreArchivo: String,
    firmaGuardadaPath: String?,
    onFirmaGuardada: (String) -> Unit,
    onEliminarFirma: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var trazos by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var tamanoCanvas by remember { mutableStateOf(IntSize.Zero) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        if (!firmaGuardadaPath.isNullOrBlank()) {
            Text(
                text = "✓ Firma guardada en el dispositivo",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
            )
            OutlinedButton(
                onClick = {
                    onEliminarFirma()
                    trazos = emptyList()
                    error = null
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CAMBIAR FIRMA")
            }
        } else {
            Text(
                text = "Firma dentro del recuadro usando el dedo o un lápiz táctil.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .onSizeChanged { tamanoCanvas = it }
                    .pointerInput(Unit) {
                        // Cada gesto tendrá su propio trazo independiente.
                        var indiceTrazoActual = -1

                        detectDragGestures(
                            onDragStart = { posicion ->
                                // Creamos un nuevo trazo al comenzar cada gesto.
                                indiceTrazoActual = trazos.size

                                trazos = trazos + listOf(listOf(posicion))
                                error = null
                            },

                            onDrag = { cambio, _ ->
                                cambio.consume()

                                // Modificamos únicamente el trazo del gesto actual.
                                val indice = indiceTrazoActual

                                if (indice in trazos.indices) {
                                    val copia = trazos.toMutableList()

                                    copia[indice] =
                                        copia[indice] + cambio.position

                                    trazos = copia
                                }
                            },

                            onDragEnd = {
                                indiceTrazoActual = -1
                            },

                            onDragCancel = {
                                indiceTrazoActual = -1
                            }
                        )
                    }
            ) {
                trazos.forEach { puntos ->
                    if (puntos.size == 1) {
                        drawCircle(
                            color = Color.Black,
                            radius = 2.5.dp.toPx(),
                            center = puntos.first()
                        )
                    } else {
                        puntos.zipWithNext().forEach { (inicio, fin) ->
                            drawLine(
                                color = Color.Black,
                                start = inicio,
                                end = fin,
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
            if (error != null) {
                Text(
                    text = error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { trazos = emptyList(); error = null },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("LIMPIAR")
                }
                Button(
                    onClick = {
                        if (trazos.isEmpty() || tamanoCanvas.width <= 0 || tamanoCanvas.height <= 0) {
                            error = "Dibuje la firma antes de guardarla."
                        } else {
                            try {
                                val ruta = guardarFirmaPng(
                                    context = context,
                                    nombreArchivo = nombreArchivo,
                                    trazos = trazos,
                                    ancho = tamanoCanvas.width,
                                    alto = tamanoCanvas.height
                                )
                                val archivo = File(ruta)
                                if (!archivo.isFile || archivo.length() == 0L) {
                                    throw IllegalStateException("El archivo de firma quedó vacío.")
                                }
                                onFirmaGuardada(ruta)
                                error = null
                            } catch (e: Exception) {
                                error = e.message ?: "No fue posible guardar la firma."
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("GUARDAR FIRMA")
                }
            }
        }
    }
}

private fun guardarFirmaPng(
    context: Context,
    nombreArchivo: String,
    trazos: List<List<Offset>>,
    ancho: Int,
    alto: Int
): String {
    val directorio = File(context.filesDir, "firmas_sisfv")
    if (!directorio.exists() && !directorio.mkdirs()) {
        throw IllegalStateException("No se pudo crear la carpeta de firmas.")
    }
    val nombreSeguro = nombreArchivo.replace(Regex("[^A-Za-z0-9_-]"), "_")
    val archivo = File(directorio, "${nombreSeguro}_${System.currentTimeMillis()}.png")
    val bitmap = Bitmap.createBitmap(ancho.coerceAtLeast(1), alto.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    try {
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val pintura = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = AndroidPaint.Style.STROKE
            strokeWidth = 4f
            strokeCap = AndroidPaint.Cap.ROUND
            strokeJoin = AndroidPaint.Join.ROUND
        }
        trazos.forEach { puntos ->
            if (puntos.size == 1) {
                val punto = puntos.first()
                canvas.drawCircle(punto.x, punto.y, 2.5f, pintura)
            } else if (puntos.size > 1) {
                val camino = AndroidPath().apply {
                    moveTo(puntos.first().x, puntos.first().y)
                    puntos.drop(1).forEach { lineTo(it.x, it.y) }
                }
                canvas.drawPath(camino, pintura)
            }
        }
        FileOutputStream(archivo).use { salida ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida)) {
                throw IllegalStateException("No fue posible codificar la firma PNG.")
            }
            salida.flush()
        }
    } catch (e: Exception) {
        archivo.delete()
        throw e
    } finally {
        bitmap.recycle()
    }
    return archivo.absolutePath
}
