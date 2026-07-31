package com.ugelaa.monitoreo.model

data class LoginRequest(
    val nickname: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String,
    val token_type: String,
    val usuario: Usuario
)

data class Usuario(
    val id: Int,
    val dni: String?,
    val nombre_completo: String,
    val nickname: String
)