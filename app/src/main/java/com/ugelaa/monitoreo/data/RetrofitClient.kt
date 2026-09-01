package com.ugelaa.monitoreo.data

import com.ugelaa.monitoreo.model.Actualizacion
import com.ugelaa.monitoreo.model.LoginRequest
import com.ugelaa.monitoreo.model.LoginResponse
import com.ugelaa.monitoreo.model.Visita
import com.ugelaa.monitoreo.model.LugarVisita
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
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field

interface ApiService {

    @POST("api/movil/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/movil/planApi")
    suspend fun getVisitas(
        @Header("Authorization") token: String
    ): Response<List<Visita>>

    @GET("api/movil/planVisitas/{plan_id}")
    suspend fun getDetalleVisita(
        @Path("plan_id") planId: String,
        @Header("Authorization") token: String
    ): Response<List<LugarVisita>>

    // GUARDAR EVIDENCIA
    @Multipart
    @POST("api/movil/guardarVisitas")
    suspend fun guardarVisita(
        @Header("Authorization") token: String,
        @Part("plan_id") planId: RequestBody,
        @Part("lugares_visitas_id") lugaresVisitasId: RequestBody,
        @Part("estado_visita") estadoVisita: RequestBody,
        @Part("usuario_id") usuarioId: RequestBody,
        @Part("estado") estado: RequestBody,
        @Part("fecha") fecha: RequestBody,
        @Part("hora") hora: RequestBody,
        @Part("anio") anio: RequestBody,
        @Part("mes") mes: RequestBody,
        @Part("numero_mes") numeroMes: RequestBody,
        @Part("latitud") latitud: RequestBody,
        @Part("longitud") longitud: RequestBody,
        @Part("precision_gps") precisionGps: RequestBody,
        @Part("observacion") observacion: RequestBody,
        @Part foto: MultipartBody.Part
    ): Response<Any>

    // MARCAR PLANES PADRE VENCIDOS
    @FormUrlEncoded
    @POST("api/movil/marcarVencidos")
    suspend fun marcarVisitasVencidas(
        @Header("Authorization") token: String,
        @Field("planes_vencidos[]") planesVencidos: List<Int>
    ): Response<Any>

    // MARCAR LUGARES HIJOS VENCIDOS
    @FormUrlEncoded
    @POST("api/movil/marcarLugaresVencidos")
    suspend fun marcarLugaresVencidos(
        @Header("Authorization") token: String,
        @Field("lugares_vencidos[]") lugaresVencidos: List<Int>
    ): Response<Any>

    @GET("api/movil/actualizaciones")
    suspend fun verificarActualizacion(): Response<List<Actualizacion>>

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