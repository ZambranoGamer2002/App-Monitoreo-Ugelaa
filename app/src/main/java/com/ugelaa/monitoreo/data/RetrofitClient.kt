package com.ugelaa.monitoreo.data

import com.ugelaa.monitoreo.model.LoginRequest
import com.ugelaa.monitoreo.model.LoginResponse
import com.ugelaa.monitoreo.model.Visita
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @POST("api/movil/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/movil/planApi")
    suspend fun getVisitas(@Header("Authorization") token: String): Response<List<Visita>>

    @Multipart
    @POST("api/movil/guardarVisitas")
    suspend fun guardarVisita(
        @Header("Authorization") token: String,
        @Part("plan_id") planId: RequestBody,
        @Part("usuario_id") usuarioId: RequestBody,
        @Part("estado") estado: RequestBody,
        @Part("fecha") fecha: RequestBody,
        @Part("hora") hora: RequestBody,
        @Part("latitud") latitud: RequestBody,
        @Part("longitud") longitud: RequestBody,
        @Part("precision_gps") precisionGps: RequestBody,
        @Part foto: MultipartBody.Part
    ): Response<Any>
}

object RetrofitClient {
    private const val BASE_URL = "http://192.168.16.20:8070/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}