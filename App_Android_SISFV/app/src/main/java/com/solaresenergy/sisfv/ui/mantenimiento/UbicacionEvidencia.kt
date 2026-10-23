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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

internal data class EstadoUbicacionEvidencia(
    val ubicacion: Location?,
    val permisoConcedido: Boolean,
    val solicitarPermiso: () -> Unit
)

data class EvidenciaFotoUbicacion(
    val codigo: String,
    val ruta: String,
    val latitud: Double,
    val longitud: Double,
    val precision: Float,
    val fechaCaptura: Long
)

@Composable
internal fun rememberUbicacionEvidencia(): EstadoUbicacionEvidencia {
    val context = LocalContext.current
    var permisoConcedido by remember { mutableStateOf(tienePermisoUbicacionEvidencia(context)) }
    var ubicacion by remember { mutableStateOf<Location?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        permisoConcedido = resultado[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            resultado[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            tienePermisoUbicacionEvidencia(context)
    }

    DisposableEffect(context, permisoConcedido) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var listener: LocationListener? = null
        if (permisoConcedido) {
            val receptor = object : LocationListener {
                override fun onLocationChanged(location: Location) { ubicacion = location }
                @Deprecated("Deprecated by Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }
            listener = receptor
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
                try {
                    manager.getLastKnownLocation(provider)?.let { actual ->
                        if (ubicacion == null || actual.time > ubicacion!!.time) ubicacion = actual
                    }
                    if (manager.isProviderEnabled(provider)) {
                        manager.requestLocationUpdates(provider, 2_000L, 0f, receptor, Looper.getMainLooper())
                    }
                } catch (_: SecurityException) { }
            }
        }
        onDispose { try { listener?.let(manager::removeUpdates) } catch (_: Exception) { } }
    }

    return EstadoUbicacionEvidencia(
        ubicacion = ubicacion,
        permisoConcedido = permisoConcedido,
        solicitarPermiso = {
            launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    )
}

private fun tienePermisoUbicacionEvidencia(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
