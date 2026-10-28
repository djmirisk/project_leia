package com.solaresenergy.sisfv.data.local

import android.content.Context

/**
 * Sesión local del técnico.
 *
 * El técnico de campo sale de la app con frecuencia (cámara, llamadas, cambios de
 * aplicación) y el sistema puede cerrar el proceso en segundo plano. Guardar el
 * identificador en preferencias evita que tenga que volver a iniciar sesión cada
 * vez que regresa. La sesión solo se borra cuando se pulsa SALIR.
 */
object SesionTecnico {

    private const val PREFERENCIAS = "sisfv_sesion"
    private const val CLAVE_TECNICO_ID = "tecnico_id"

    private fun preferencias(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)

    fun guardar(context: Context, tecnicoId: Int) {
        preferencias(context).edit().putInt(CLAVE_TECNICO_ID, tecnicoId).apply()
    }

    fun leer(context: Context): Int? {
        val id = preferencias(context).getInt(CLAVE_TECNICO_ID, -1)
        return id.takeIf { it > 0 }
    }

    fun limpiar(context: Context) {
        preferencias(context).edit().remove(CLAVE_TECNICO_ID).apply()
    }
}
