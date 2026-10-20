package com.solaresenergy.sisfv.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SyncWorker gestiona el envío de datos en segundo plano hacia la NAS de la oficina
 * cuando se detecta conexión a internet (Wi-Fi o Datos 4G).
 */
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. Obtener registros pendientes con estado "PENDING_SYNC"
            // 2. Transmitir lote de mediciones y las 16 evidencias fotográficas a la API de la NAS
            // 3. Actualizar estado a "SYNCED" al recibir confirmación 200 OK del servidor NAS

            Result.success()
        } catch (e: Exception) {
            // Reintento automático con curva exponencial si falla la red
            Result.retry()
        }
    }
}
