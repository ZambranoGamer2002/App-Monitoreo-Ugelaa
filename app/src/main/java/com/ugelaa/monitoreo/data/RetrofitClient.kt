package com.ugelaa.monitoreo.data

import com.google.gson.GsonBuilder
import com.ugelaa.monitoreo.model.Actualizacion
import com.ugelaa.monitoreo.model.LoginRequest
import com.ugelaa.monitoreo.model.LoginResponse
import com.ugelaa.monitoreo.model.LugarVisita
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface ApiService {

    @Headers("Accept: application/json")
    @POST("api/movil/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @Headers("Accept: application/json")
    @GET("api/movil/planApi")
    suspend fun getVisitas(
        @Header("Authorization") token: String
    ): Response<ResponseBody>

    @Headers("Accept: application/json")
    @GET("api/movil/planVisitas/{plan_id}")
    suspend fun getDetalleVisita(
        @Path("plan_id") planId: String,
        @Header("Authorization") token: String
    ): Response<List<LugarVisita>>

    @Headers("Accept: application/json")
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
        @Part("observacion_visita") observacionVisita: RequestBody,
        @Part foto: MultipartBody.Part
    ): Response<Any>

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("api/movil/marcarLugaresVencidos")
    suspend fun marcarLugaresVencidos(
        @Header("Authorization") token: String,
        @Field("lugares_vencidos[]") lugaresVencidos: List<Int>
    ): Response<Any>

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("api/movil/planFinalizado")
    suspend fun planFinalizado(
        @Header("Authorization") token: String,
        @Field("plan_id") planId: Int
    ): Response<Any>

    @Headers("Accept: application/json")
    @GET("api/actualizaciones")
    suspend fun verificarActualizacion(): Response<List<Actualizacion>>

    @Headers("Accept: application/json")
    @GET("api/movil/perfil")
    suspend fun getPerfil(
        @Header("Authorization") token: String
    ): Response<okhttp3.ResponseBody>

}

object RetrofitClient {
    private const val BASE_URL = "https://simoplan.ugelaa.gob.pe/"

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    // Cliente OkHttp que inyecta Accept: application/json automáticamente
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        })
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}