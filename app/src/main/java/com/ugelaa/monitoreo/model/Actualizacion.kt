package com.ugelaa.monitoreo.model

data class Actualizacion(
    val id: Int,
    val version_actual: String,
    val fecha: String,
    val hora: String,
    val estado: String
)