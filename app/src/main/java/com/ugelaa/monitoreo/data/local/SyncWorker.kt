package com.ugelaa.monitoreo.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ugelaa.monitoreo.data.RetrofitClient
import com.ugelaa.monitoreo.utils.SessionManager
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class SyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(context)
        val visitaDao = database.visitaDao()

        val evidenciasPendientes = visitaDao.obtenerEvidenciasPendientes()

        if (evidenciasPendientes.isEmpty()) {
            return Result.success()
        }

        val sessionManager = SessionManager(context)
        val token = sessionManager.getToken.firstOrNull() ?: return Result.failure()

        var allSuccessful = true

        for (evidencia in evidenciasPendientes) {
            val file = File(evidencia.rutaFotoLocal)

            if (!file.exists()) {
                visitaDao.eliminarEvidencia(evidencia)
                continue
            }

            try {
                val response = RetrofitClient.apiService.guardarVisita(
                    token = "Bearer $token",
                    planId = evidencia.planId.toRequestBody("text/plain".toMediaTypeOrNull()),
                    lugaresVisitasId = evidencia.lugaresVisitasId.toRequestBody("text/plain".toMediaTypeOrNull()),
                    estadoVisita = evidencia.estadoVisita.toRequestBody("text/plain".toMediaTypeOrNull()),
                    usuarioId = evidencia.usuarioId.toRequestBody("text/plain".toMediaTypeOrNull()),
                    estado = evidencia.estado.toRequestBody("text/plain".toMediaTypeOrNull()),
                    fecha = evidencia.fecha.toRequestBody("text/plain".toMediaTypeOrNull()),
                    hora = evidencia.hora.toRequestBody("text/plain".toMediaTypeOrNull()),
                    anio = evidencia.anio.toRequestBody("text/plain".toMediaTypeOrNull()),
                    mes = evidencia.mes.toRequestBody("text/plain".toMediaTypeOrNull()),
                    numeroMes = evidencia.numeroMes.toRequestBody("text/plain".toMediaTypeOrNull()),
                    latitud = evidencia.latitud.toRequestBody("text/plain".toMediaTypeOrNull()),
                    longitud = evidencia.longitud.toRequestBody("text/plain".toMediaTypeOrNull()),
                    precisionGps = evidencia.precisionGps.toRequestBody("text/plain".toMediaTypeOrNull()),
                    observacion = evidencia.observacion.toRequestBody("text/plain".toMediaTypeOrNull()),
                    observacionVisita = evidencia.observacion.toRequestBody("text/plain".toMediaTypeOrNull()),
                    foto = MultipartBody.Part.createFormData("foto", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                )

                if (response.isSuccessful) {

                    visitaDao.eliminarEvidencia(evidencia)
                    file.delete()
                } else {

                    allSuccessful = false
                }
            } catch (e: Exception) {
                allSuccessful = false
            }
        }
        
        return if (allSuccessful) Result.success() else Result.retry()
    }
}