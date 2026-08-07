package com.ugelaa.monitoreo.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VisitaDao {

    // Al devolver Long, KSP ya no lee la letra "V" (Void) y no explota.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEvidencia(evidencia: VisitaEvidenciaEntity): Long

    @Query("SELECT * FROM evidencias_offline")
    suspend fun obtenerEvidenciasPendientes(): List<VisitaEvidenciaEntity>

    // Al devolver Int, engañamos a KSP y compila perfecto en Kotlin.
    @Delete
    suspend fun eliminarEvidencia(evidencia: VisitaEvidenciaEntity): Int
}