package com.solaresenergy.sisfv.ui.mantenimiento

import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.solaresenergy.sisfv.ui.theme.SolarPrimary
import java.util.Locale

/**
 * Convierte el texto ingresado por el técnico en una ubicación utilizable.
 * Devuelve null cuando el formato o el rango no son válidos.
 */
internal fun construirUbicacionManual(latitud: String, longitud: String): Location? {
    val formato = Regex("^-?\\d+\\.\\d{4}$")
    if (!formato.matches(latitud) || !formato.matches(longitud)) return null
    val lat = latitud.toDoubleOrNull() ?: return null
    val lon = longitud.toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
    return Location("manual").apply {
        latitude = lat
        this.longitude = lon
        accuracy = 0f
        time = System.currentTimeMillis()
    }
}

/**
 * Sección única de ubicación para todos los formularios con fotografías.
 *
 * Se confirma una sola vez y aplica a todas las evidencias del formulario.
 * Una vez confirmada, la coordenada queda bloqueada y solo puede modificarse
 * pulsando CORREGIR COORDENADAS, igual que al volver a firmar.
 */
@Composable
internal fun SeccionUbicacionFotos(
    codigoSisfv: String,
    ubicacionEfectiva: Location?,
    ubicacionPermitida: Boolean,
    manualConfirmado: Boolean,
    latitud: String,
    longitud: String,
    habilitado: Boolean = true,
    onLatitudChange: (String) -> Unit,
    onLongitudChange: (String) -> Unit,
    onConfirmar: (Location) -> Unit,
    onCorregir: () -> Unit,
    onSolicitarPermiso: () -> Unit,
    modifier: Modifier = Modifier
) {
    var error by remember(codigoSisfv) { mutableStateOf<String?>(null) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Ubicación para todas las fotografías",
                fontWeight = FontWeight.Bold,
                color = SolarPrimary
            )

            Text(
                text = when {
                    manualConfirmado ->
                        "Coordenadas manuales confirmadas: $latitud, $longitud. Se marcan todas las fotos (sin precisión GPS)."
                    !ubicacionPermitida ->
                        "GPS sin permiso. Puedes ingresar las coordenadas manualmente."
                    ubicacionGpsReciente(ubicacionEfectiva) ->
                        "GPS listo: ${ubicacionEfectiva?.latitude}, ${ubicacionEfectiva?.longitude}  ·  Precisión ±${String.format(Locale.getDefault(), "%.0f", ubicacionEfectiva?.accuracy ?: 0f)} m"
                    else ->
                        "GPS buscando ubicación reciente. También puedes ingresar coordenadas manuales."
                },
                style = MaterialTheme.typography.bodySmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latitud,
                    onValueChange = {
                        if (!manualConfirmado) {
                            onLatitudChange(filtrarEntradaCoordenada(it))
                            error = null
                        }
                    },
                    enabled = habilitado && !manualConfirmado,
                    modifier = Modifier.weight(1f),
                    label = { Text("Latitud") },
                    placeholder = { Text("7.3345") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = longitud,
                    onValueChange = {
                        if (!manualConfirmado) {
                            onLongitudChange(filtrarEntradaCoordenada(it))
                            error = null
                        }
                    },
                    enabled = habilitado && !manualConfirmado,
                    modifier = Modifier.weight(1f),
                    label = { Text("Longitud") },
                    placeholder = { Text("-76.1212") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            if (manualConfirmado) {
                Text(
                    text = "Coordenada registrada y bloqueada. Pulsa CORREGIR COORDENADAS para modificarla y confirmarla de nuevo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SolarPrimary
                )
            }

            Button(
                onClick = {
                    if (manualConfirmado) {
                        onCorregir()
                        error = null
                    } else {
                        val manual = construirUbicacionManual(latitud, longitud)
                        if (manual == null) {
                            error = "Ingrese coordenadas válidas con punto y 4 decimales."
                        } else {
                            onConfirmar(manual)
                            error = null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = habilitado
            ) {
                Text(if (manualConfirmado) "CORREGIR COORDENADAS" else "CONFIRMAR COORDENADAS")
            }

            if (!ubicacionPermitida && !manualConfirmado) {
                TextButton(onClick = onSolicitarPermiso, enabled = habilitado) {
                    Text("Permitir acceso al GPS del dispositivo")
                }
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
