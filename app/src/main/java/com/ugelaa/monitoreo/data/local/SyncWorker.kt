package com.ugelaa.monitoreo.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ugelaa.monitoreo.data.RetrofitClient
import com.ugelaa.monitoreo.utils.SessionManager
import kotlinx.coroutines.flow.first
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
        val sessionManager = SessionManager(context)

        val token = sessionManager.getToken.first()
        if (token.isEmpty()) return Result.failure()

        val pendientes = visitaDao.obtenerEvidenciasPendientes()
        if (pendientes.isEmpty()) return Result.success()

        for (evidencia in pendientes) {
            try {
                val fileFoto = File(evidencia.rutaFotoLocal)
                if (!fileFoto.exists()) {
                    visitaDao.eliminarEvidencia(evidencia)
                    continue
                }

                val response = RetrofitClient.apiService.guardarVisita(
                    token = "Bearer $token",
                    planId = evidencia.planId.toRequestBody("text/plain".toMediaTypeOrNull()),
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
                    foto = MultipartBody.Part.createFormData(
                        "foto",
                        fileFoto.name,
                        fileFoto.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                )

                if (response.isSuccessful) {
                    visitaDao.eliminarEvidencia(evidencia)
                }

            } catch (e: Exception) {
                return Result.retry()
            }
        }

        return Result.success()
    }
}