package com.ugelaa.monitoreo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "evidencias_offline")
data class VisitaEvidenciaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val planId: String,
    val usuarioId: String,
    val estado: String,
    val fecha: String,
    val hora: String,
    val anio: String,
    val mes: String,
    val numeroMes: String,
    val latitud: String,
    val longitud: String,
    val precisionGps: String,
    val rutaFotoLocal: String
)