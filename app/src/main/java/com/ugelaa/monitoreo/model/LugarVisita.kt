package com.ugelaa.monitoreo.model

data class LugarVisita(
    val id: Int,
    val lugares: String,
    val lugar_gps: String?,
    val latitud: String?,
    val longitud: String?,
    val fecha: String,
    var estado: String?
)