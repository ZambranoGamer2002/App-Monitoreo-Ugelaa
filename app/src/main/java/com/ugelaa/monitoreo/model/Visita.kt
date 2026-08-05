package com.ugelaa.monitoreo.model

data class Visita(
    val id: Int,
    val nombre_visitas: String,
    val asunto: String,
    val lugar_visita: String,
    val nombre_trabajador: String,
    val documento_trabajador: String,
    val numero_documento: String,
    val tipodocumento_id: Int,
    val fecha_documento: String,
    val fecha_inicio: String,
    val fecha_fin: String
)