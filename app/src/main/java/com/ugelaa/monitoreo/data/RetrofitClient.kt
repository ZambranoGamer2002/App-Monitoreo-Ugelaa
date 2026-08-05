package com.ugelaa.monitoreo.data

import com.ugelaa.monitoreo.model.LoginRequest
import com.ugelaa.monitoreo.model.LoginResponse
import com.ugelaa.monitoreo.model.Visita
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    // Ruta del Login
    @POST("api/movil/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Ruta de las Visitas
    @GET("api/planApi")
    suspend fun getVisitas(@Header("Authorization") token: String): Response<List<Visita>>
}

object RetrofitClient {
    // IP local
    private const val BASE_URL = "http://192.168.16.20:8070/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}