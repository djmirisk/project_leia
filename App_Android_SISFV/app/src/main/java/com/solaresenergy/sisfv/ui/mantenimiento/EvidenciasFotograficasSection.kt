package com.solaresenergy.sisfv.ui.mantenimiento

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Matrix
import android.media.ExifInterface
import android.graphics.Typeface
import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.solaresenergy.sisfv.ui.theme.SolarPrimary
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class FotoRequerida(
    val codigo: String,
    val titulo: String,
    val indicacion: String = ""
)

private val catalogoFotosSisfv = listOf(
    FotoRequerida("PANORAMICA_VIVIENDA", "Panorámica de vivienda", "Debe verse el poste, la estructura con paneles y la vivienda al fondo."),
    FotoRequerida("POSTE_PANELES", "Poste y paneles", "Vista general del poste y los paneles instalados."),
    FotoRequerida("PANELES_DESDE_ABAJO", "Paneles desde abajo", "Evidencia visual de que no estén fisurados o quemados."),
    FotoRequerida("SPT_PUESTA_TIERRA", "Sistema de puesta a tierra (SPT)", "Mostrar los elementos visibles de la puesta a tierra."),
    FotoRequerida("GABINETE_CERRADO", "Gabinete cerrado", "Evitar que haya objetos apoyados encima del gabinete."),
    FotoRequerida("GABINETE_ABIERTO", "Gabinete abierto", "Deben distinguirse los componentes internos."),
    FotoRequerida("INVERSOR", "Inversor", "Mostrar el equipo y su estado visible."),
    FotoRequerida("CONTROLADOR", "Controlador", "Mostrar el controlador y su estado visible."),
    FotoRequerida("BATERIA", "Batería", "Mostrar la batería y sus conexiones visibles."),
    FotoRequerida("PROTECCIONES", "Protecciones", "Mostrar las protecciones eléctricas."),
    FotoRequerida("MEDIDOR_GABINETE", "Medidor instalado en gabinete", "Fotografía del medidor instalado."),
    FotoRequerida("TABLERO_DISTRIBUCION_AC", "Tablero de distribución AC", "Mostrar el tablero ubicado sobre el gabinete."),
    FotoRequerida("INTERRUPTOR_LUCES", "Interruptor de luces", "Fotografía del suiche de iluminación."),
    FotoRequerida("TOMACORRIENTES", "Tomacorrientes", "Mostrar los tomacorrientes de la instalación."),
    FotoRequerida("BOMBILLO_ENCENDIDO", "Bombillo de iluminación", "Si está apagado, registre NO FUNCIONAL para todo el SISFV."),
    FotoRequerida("VOLTAJE_BATERIA_HERRAMIENTA", "Medición de voltaje de batería", "Use los EPP adecuados y muestre la medición en tester o pinza voltamperimétrica."),
    FotoRequerida("VOLTAJE_PANEL_HERRAMIENTA", "Medición de voltaje del panel", "Use los EPP adecuados y muestre la medición en tester o pinza voltamperimétrica."),
    FotoRequerida("VOLTAJE_TOMACORRIENTE_HERRAMIENTA","Medición de voltaje en tomacorriente","Use los EPP adecuados y muestre la medición del voltaje en el tomacorriente con un multímetro o tester."
    )
)
internal const val MAX_ANTIGUEDAD_GPS_MS = 60 * 60 * 1000L

internal fun ubicacionGpsReciente(location: Location?): Boolean =
    location != null && System.currentTimeMillis() - location.time in 0L..MAX_ANTIGUEDAD_GPS_MS

internal fun filtrarEntradaCoordenada(entrada: String): String {
    val resultado = StringBuilder()
    // Los teclados numéricos en español muestran la coma como separador decimal.
    entrada.replace(',', '.').forEach { caracter ->
        when {
            caracter in '0'..'9' && resultado.length < 8 -> resultado.append(caracter)
            caracter == '.' && '.' !in resultado && resultado.length < 8 -> resultado.append(caracter)
            caracter == '-' && resultado.isEmpty() && resultado.length < 8 -> resultado.append(caracter)
        }
    }
    return resultado.toString()
}

private fun tienePermisoCamara(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

@Composable
fun EvidenciasFotograficasSection(
    codigoSisfv: String,
    nui: String?,
    tecnicoNombre: String,
    tecnicoId: Int,
    fotos: List<FotoMantenimientoCapturada>,
    onFotosChange: (List<FotoMantenimientoCapturada>) -> Unit,
    bombilloEncendido: Boolean?,
    onBombilloEncendidoChange: (Boolean) -> Unit,
    ubicacionActual: Location?,
    ubicacionPermitida: Boolean,
    onSolicitarPermisoUbicacion: () -> Unit,
    onUbicacionManualConfirmada: (Location) -> Unit,
    onCoordenadasManualesEditadas: () -> Unit,
    guardando: Boolean = false,
    modifier: Modifier = Modifier,
    medidorInstalado: Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var errorFoto by remember { mutableStateOf<String?>(null) }
    var mensajeFoto by remember { mutableStateOf<String?>(null) }
    var procesandoFoto by remember { mutableStateOf(false) }
    var camaraPermitida by remember { mutableStateOf(tienePermisoCamara(context)) }
    var fotoPendiente by remember { mutableStateOf<FotoRequerida?>(null) }
    var rutaPendiente by remember { mutableStateOf<String?>(null) }
    var ubicacionCapturaPendiente by remember { mutableStateOf<Location?>(null) }
    var latitudManual by rememberSaveable(codigoSisfv) { mutableStateOf("") }
    var longitudManual by rememberSaveable(codigoSisfv) { mutableStateOf("") }
    var errorCoordenadasManuales by rememberSaveable(codigoSisfv) { mutableStateOf<String?>(null) }

    val solicitarPermisoCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        camaraPermitida = concedido
        errorFoto = if (concedido) {
            "Permiso de cámara concedido. Pulsa nuevamente la fotografía que deseas tomar."
        } else {
            "Debes permitir el acceso a la cámara para registrar las evidencias."
        }
    }

    val tomarFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito ->
        val ruta = rutaPendiente
        val requerido = fotoPendiente
        val ubicacion = ubicacionCapturaPendiente
        rutaPendiente = null
        fotoPendiente = null
        ubicacionCapturaPendiente = null

        if (!exito || ruta == null || requerido == null || ubicacion == null) {
            ruta?.let { File(it).delete() }
            procesandoFoto = false
            errorFoto = "No se pudo completar la fotografía. Inténtalo nuevamente."
        } else {
            procesandoFoto = true
            scope.launch {
                try {
                    val resultado = withContext(Dispatchers.IO) {
                        val fechaCaptura = File(ruta).lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
                        val directorioFinal = File(context.filesDir, "evidencias_sisfv")
                        if (!directorioFinal.exists() && !directorioFinal.mkdirs()) {
                            throw IllegalStateException("No se pudo crear la carpeta definitiva de evidencias.")
                        }
                        val rutaFinal = File(
                            directorioFinal,
                            "${requerido.codigo}_${fechaCaptura}_marcada.jpg"
                        ).absolutePath
                        generarFotoConMarcaAgua(
                            context = context,
                            rutaCapturaTemporal = ruta,
                            rutaFinal = rutaFinal,
                            codigoSisfv = codigoSisfv,
                            nui = nui,
                            tecnicoNombre = tecnicoNombre,
                            tecnicoId = tecnicoId,
                            tituloFoto = requerido.titulo,
                            fechaCaptura = fechaCaptura,
                            latitud = ubicacion.latitude,
                            longitud = ubicacion.longitude,
                            precision = ubicacion.accuracy,
                            coordenadasManuales = ubicacion.provider == "manual"
                        )
                        val hash = calcularSha256(File(rutaFinal))
                        FotoMantenimientoCapturada(
                            codigoFoto = requerido.codigo,
                            orden = catalogoFotosSisfv.indexOfFirst {
                                it.codigo == requerido.codigo
                            } + 1,
                            titulo = requerido.titulo,
                            rutaEvidencia = rutaFinal,
                            fechaCaptura = fechaCaptura,
                            latitudGps = ubicacion.latitude,
                            longitudGps = ubicacion.longitude,
                            precisionGps = ubicacion.accuracy,
                            hashSha256 = hash
                        )
                    }

                    File(ruta).delete() // La imagen sin marca de agua solo existió como archivo temporal.
                    val anterior = fotos.firstOrNull { it.codigoFoto == resultado.codigoFoto }
                    onFotosChange(
                        (fotos.filterNot { it.codigoFoto == resultado.codigoFoto } + resultado)
                            .sortedBy { it.orden }
                    )
                    if (anterior != null && anterior.rutaEvidencia != resultado.rutaEvidencia) {
                        File(anterior.rutaEvidencia).delete()
                    }
                    errorFoto = null
                    mensajeFoto = "Evidencia ${resultado.orden}/18 guardada con marca de agua."
                } catch (e: Exception) {
                    File(ruta).delete()
                    errorFoto = "No se pudo aplicar la marca de agua: ${e.message ?: "error de imagen"}"
                } finally {
                    procesandoFoto = false
                }
            }
        }
    }

    fun iniciarCaptura(foto: FotoRequerida) {
        errorFoto = null
        mensajeFoto = null
        if (!camaraPermitida) {
            solicitarPermisoCamara.launch(Manifest.permission.CAMERA)
            return
        }
        if (!ubicacionPermitida) {
            onSolicitarPermisoUbicacion()
            errorFoto = "Solicitando permiso GPS. Después de concederlo, espera una posición y pulsa nuevamente."
            return
        }
        val ubicacion = ubicacionActual
        if (!ubicacionGpsReciente(ubicacion)) {
            errorFoto = "No hay una ubicación GPS reciente. Activa la ubicación y espera a que la tablet obtenga una posición antes de capturar."
            return
        }

        try {
            val directorio = File(context.cacheDir, "evidencias_sisfv_tmp")
            if (!directorio.exists() && !directorio.mkdirs()) {
                throw IllegalStateException("No se pudo crear la carpeta temporal de evidencias.")
            }
            val archivo = File(
                directorio,
                "${foto.codigo}_${System.currentTimeMillis()}_captura.jpg"
            )
            if (!archivo.createNewFile()) {
                throw IllegalStateException("No se pudo crear el archivo de evidencia.")
            }
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                archivo
            )
            fotoPendiente = foto
            rutaPendiente = archivo.absolutePath
            ubicacionCapturaPendiente = ubicacion
            tomarFoto.launch(uri)
        } catch (e: Exception) {
            rutaPendiente?.let { File(it).delete() }
            rutaPendiente = null
            fotoPendiente = null
            ubicacionCapturaPendiente = null
            errorFoto = "No se pudo abrir la cámara: ${e.message ?: "error desconocido"}"
        }
    }

    val cantidad = fotos.map { it.codigoFoto }.distinct().size
    val ubicacionReciente = ubicacionGpsReciente(ubicacionActual)

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "6. Evidencias fotográficas obligatorias",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SolarPrimary
            )
            Text("Fotografías registradas: $cantidad de 18")
            Text(
                text = when {
                    ubicacionActual?.provider == "manual" -> "Coordenadas manuales confirmadas."
                    !ubicacionPermitida -> "GPS: permiso pendiente."
                    ubicacionReciente -> "GPS listo: ±${ubicacionActual?.accuracy?.let { String.format(Locale.getDefault(), "%.0f", it) } ?: "?"} m"
                    else -> "GPS buscando posición reciente… mantén la ubicación activada."
                },
                color = if (ubicacionReciente) SolarPrimary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Si la tablet no tiene GPS, ingrese latitud y longitud en grados decimales, con punto y exactamente 4 decimales.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latitudManual,
                    onValueChange = {
                        latitudManual = filtrarEntradaCoordenada(it)
                        errorCoordenadasManuales = null
                        onCoordenadasManualesEditadas()
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Latitud") },
                    placeholder = { Text("7.3345") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = longitudManual,
                    onValueChange = {
                        longitudManual = filtrarEntradaCoordenada(it)
                        errorCoordenadasManuales = null
                        onCoordenadasManualesEditadas()
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Longitud") },
                    placeholder = { Text("-76.1212") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
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
                        errorCoordenadasManuales = "Ingrese coordenadas válidas con punto y exactamente 4 decimales (ej. 7.3345 y -76.1212)."
                    } else {
                        val location = Location("manual").apply {
                            latitude = latitud
                            this.longitude = longitud
                            accuracy = 0f
                            time = System.currentTimeMillis()
                        }
                        onUbicacionManualConfirmada(location)
                        errorCoordenadasManuales = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !guardando && !procesandoFoto
            ) {
                Text(if (ubicacionActual?.provider == "manual") "COORDENADAS CONFIRMADAS" else "CONFIRMAR COORDENADAS")
            }
            errorCoordenadasManuales?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                if (ubicacionActual?.provider == "manual") {
                    "Cada foto se guarda con marca de agua, código SISFV, técnico, fecha/hora y coordenadas ingresadas manualmente."
                } else {
                    "Cada foto se guarda con marca de agua, código SISFV, técnico, fecha/hora y coordenadas GPS con precisión."
                },
                style = MaterialTheme.typography.bodySmall
            )

            catalogoFotosSisfv.forEachIndexed { indice, fotoOriginal ->

                val foto = if (
                    fotoOriginal.codigo == "MEDIDOR_GABINETE" &&
                    !medidorInstalado
                ) {
                    fotoOriginal.copy(
                        titulo = "Ubicación prevista del medidor (no instalado)",
                        indicacion = "Fotografíe el gabinete o el lugar donde debería estar instalado el medidor."
                    )
                } else {
                    fotoOriginal
                }

                val capturada = fotos.any { it.codigoFoto == foto.codigo }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "${indice + 1}. ${foto.titulo}",
                            fontWeight = FontWeight.SemiBold
                        )
                        if (foto.indicacion.isNotBlank()) {
                            Text(foto.indicacion, style = MaterialTheme.typography.bodySmall)
                        }
                        if (foto.codigo == "BOMBILLO_ENCENDIDO") {
                            Text("¿El bombillo enciende durante la prueba? Esta respuesta define el estado de todo el SISFV.")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OpcionRadio(
                                    texto = "Sí, enciende",
                                    seleccionado = bombilloEncendido == true,
                                    onClick = { onBombilloEncendidoChange(true) }
                                )
                                OpcionRadio(
                                    texto = "No enciende",
                                    seleccionado = bombilloEncendido == false,
                                    onClick = { onBombilloEncendidoChange(false) }
                                )
                            }
                        }
                        Text(
                            if (capturada) "✓ Evidencia marcada guardada" else "Fotografía obligatoria pendiente",
                            color = if (capturada) SolarPrimary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { iniciarCaptura(foto) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !guardando && !procesandoFoto
                        ) {
                            Text(if (capturada) "VOLVER A TOMAR FOTO" else "TOMAR FOTO")
                        }
                    }
                }
            }

            if (errorFoto != null) {
                Text(errorFoto!!, color = MaterialTheme.colorScheme.error)
            }
            if (mensajeFoto != null) {
                Text(mensajeFoto!!, color = SolarPrimary)
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

internal fun generarFotoConMarcaAgua(
    context: Context,
    rutaCapturaTemporal: String,
    rutaFinal: String,
    codigoSisfv: String,
    nui: String?,
    tecnicoNombre: String,
    tecnicoId: Int,
    tituloFoto: String,
    fechaCaptura: Long,
    latitud: Double,
    longitud: Double,
    precision: Float,
    coordenadasManuales: Boolean = precision == 0f
) {
    val archivoCaptura = File(rutaCapturaTemporal)
    val archivoFinal = File(rutaFinal)
    if (!archivoCaptura.exists() || archivoCaptura.length() == 0L) {
        throw IllegalStateException("La cámara no produjo una imagen válida.")
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(archivoCaptura.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw IllegalStateException("El archivo de fotografía no es una imagen válida.")
    }

    var sample = 1
    while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > 2560) {
        sample *= 2
    }
    val opciones = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val bitmapDecodificado: Bitmap = BitmapFactory.decodeFile(archivoCaptura.absolutePath, opciones)
        ?: throw IllegalStateException("No se pudo procesar la fotografía.")

    // Respeta la orientación EXIF para que las fotos verticales no queden giradas.
    val orientacion = try {
        ExifInterface(archivoCaptura.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val matriz = Matrix()
    when (orientacion) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matriz.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matriz.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matriz.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matriz.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matriz.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matriz.postScale(-1f, 1f)
            matriz.postRotate(270f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matriz.postScale(-1f, 1f)
            matriz.postRotate(90f)
        }
    }
    // Primero corregimos la orientación EXIF de la fotografía.
    val bitmapOrientado = if (matriz.isIdentity) {
        bitmapDecodificado
    } else {
        Bitmap.createBitmap(
            bitmapDecodificado,
            0,
            0,
            bitmapDecodificado.width,
            bitmapDecodificado.height,
            matriz,
            true
        ).also { bitmapTransformado ->
            if (bitmapTransformado !== bitmapDecodificado) {
                bitmapDecodificado.recycle()
            }
        }
    }

    // BitmapFactory puede devolver un bitmap inmutable. Canvas(bitmap) requiere
    // un bitmap mutable para poder dibujar la marca de agua; por eso creamos
    // una copia editable cuando sea necesario.
    val bitmap = if (bitmapOrientado.isMutable) {
        bitmapOrientado
    } else {
        val copiaMutable = bitmapOrientado.copy(Bitmap.Config.ARGB_8888, true)
            ?: run {
                bitmapOrientado.recycle()
                throw IllegalStateException(
                    "No se pudo preparar una imagen editable para aplicar la marca de agua."
                )
            }
        if (copiaMutable !== bitmapOrientado) {
            bitmapOrientado.recycle()
        }
        copiaMutable
    }

    val canvas = Canvas(bitmap)
    val nuiVisible = nui?.trim()?.takeIf { it.isNotEmpty() } ?: "NO REGISTRADO"
    val lineas = listOf(
        "NUI: $nuiVisible",
        "CÓDIGO SISFV: $codigoSisfv",
        "TECNICO: $tecnicoNombre (ID $tecnicoId)",
        "EVIDENCIA: $tituloFoto",
        "FECHA/HORA: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "CO")).format(Date(fechaCaptura))}",
        if (coordenadasManuales) {
            "COORDENADAS MANUALES: ${String.format(Locale.US, "%.4f", latitud)}, ${String.format(Locale.US, "%.4f", longitud)}"
        } else {
            "GPS: ${String.format(Locale.US, "%.6f", latitud)}, ${String.format(Locale.US, "%.6f", longitud)}  |  PRECISION: ±${String.format(Locale.getDefault(), "%.0f", precision)} m"
        }
    )
    val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = (bitmap.width / 72f).coerceIn(18f, 42f)
    }
    val margen = texto.textSize * 0.7f
    val salto = texto.textSize * 1.28f
    val altoFranja = margen * 2 + salto * lineas.size
    val tope = (bitmap.height - altoFranja).coerceAtLeast(0f)
    val fondo = Paint().apply { color = Color.argb(210, 5, 15, 22) }
    canvas.drawRect(0f, tope, bitmap.width.toFloat(), bitmap.height.toFloat(), fondo)

    val anchoMaximo = bitmap.width - margen * 2
    var y = tope + margen + texto.textSize
    lineas.forEach { linea ->
        var visible = linea
        while (texto.measureText(visible) > anchoMaximo && visible.length > 8) {
            visible = visible.dropLast(4) + "…"
        }
        canvas.drawText(visible, margen, y, texto)
        y += salto
    }

    val temporalMarcado = File(context.cacheDir, "marca_${System.nanoTime()}.jpg")
    try {
        FileOutputStream(temporalMarcado).use { salida ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, salida)) {
                throw IllegalStateException("No se pudo generar la imagen con marca de agua.")
            }
            salida.flush()
        }
        temporalMarcado.copyTo(archivoFinal, overwrite = true)
    } catch (e: Exception) {
        archivoFinal.delete()
        throw e
    } finally {
        bitmap.recycle()
        temporalMarcado.delete()
    }
    if (!archivoFinal.exists() || archivoFinal.length() == 0L) {
        throw IllegalStateException("La imagen final con marca de agua quedó vacía.")
    }
}

private fun calcularSha256(archivo: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    archivo.inputStream().use { input ->
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val leidos = input.read(buffer)
            if (leidos <= 0) break
            digest.update(buffer, 0, leidos)
        }
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}
