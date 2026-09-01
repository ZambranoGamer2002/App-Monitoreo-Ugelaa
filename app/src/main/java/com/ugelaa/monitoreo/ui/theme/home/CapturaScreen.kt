package com.ugelaa.monitoreo.ui.theme.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ugelaa.monitoreo.data.RetrofitClient
import com.ugelaa.monitoreo.data.local.AppDatabase
import com.ugelaa.monitoreo.data.local.VisitaEvidenciaEntity
import com.ugelaa.monitoreo.model.LugarVisita
import com.ugelaa.monitoreo.ui.theme.AsideFondo
import com.ugelaa.monitoreo.ui.theme.AzulPrincipal
import com.ugelaa.monitoreo.ui.theme.GrisFondoApp
import com.ugelaa.monitoreo.ui.theme.GrisTexto
import com.ugelaa.monitoreo.utils.SessionManager
import com.ugelaa.monitoreo.utils.observeConnectivityAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapturaScreen(navController: NavController, idVisita: String, nombrePlan: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val gson = remember { Gson() }

    val isOnline by observeConnectivityAsFlow(context).collectAsState(initial = true)
    val visitaDao = remember { AppDatabase.getDatabase(context).visitaDao() }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val nombrePlanLimpio = remember(nombrePlan) {
        try { URLDecoder.decode(nombrePlan, "UTF-8") } catch (e: Exception) { nombrePlan.replace("+", " ") }
    }

    val nicknameUsuario by sessionManager.getNickname.collectAsState(initial = "")
    val tokenGuardado by sessionManager.getToken.collectAsState(initial = "")

    val sharedPrefCache = context.getSharedPreferences("CacheVisitas", Context.MODE_PRIVATE)
    var lugaresVisita by remember { mutableStateOf<List<LugarVisita>>(emptyList()) }
    var errorDetalle by remember { mutableStateOf("") }

    var diaExpandidoId by remember { mutableStateOf<Int?>(null) }
    var bitmapPreview by remember { mutableStateOf<Bitmap?>(null) }
    var previewLugarId by remember { mutableStateOf<Int?>(null) }
    var previewEtapa by remember { mutableStateOf<String>("") }

    var isUploading by remember { mutableStateOf(false) }
    var isLoadingDetalle by remember { mutableStateOf(false) }
    var isGpsCargando by remember { mutableStateOf(false) }
    var serverErrorDetails by remember { mutableStateOf("") }
    var mostrarExitoDialog by remember { mutableStateOf(false) }
    var mensajeExitoDialog by remember { mutableStateOf("") }

    var refreshTrigger by remember { mutableStateOf(0) }

    var fechaCaptura by remember { mutableStateOf("") }
    var horaCaptura by remember { mutableStateOf("") }
    var anioCaptura by remember { mutableStateOf("") }
    var mesCaptura by remember { mutableStateOf("") }
    var numeroMesCaptura by remember { mutableStateOf("") }
    var latitudCaptura by remember { mutableStateOf("0.0") }
    var longitudCaptura by remember { mutableStateOf("0.0") }
    var precisionCaptura by remember { mutableStateOf("0.0") }

    var isGpsEnabled by remember { mutableStateOf(checkGpsStatusLocal(context)) }
    var isAutoTimeEnabled by remember { mutableStateOf(checkAutoTimeEnabledLocal(context)) }
    var isAirplaneModeOn by remember { mutableStateOf(checkAirplaneModeLocal(context)) }

    val isSystemReady = isGpsEnabled && isAutoTimeEnabled && !isAirplaneModeOn

    val cargarDetalle = {
        coroutineScope.launch {
            if (tokenGuardado.isNotEmpty()) {
                isLoadingDetalle = true
                try {
                    val response = RetrofitClient.apiService.getDetalleVisita(planId = idVisita, token = "Bearer $tokenGuardado")
                    if (response.isSuccessful && response.body() != null) {
                        val listaApi = response.body()!!

                        val evidenciasPendientes = visitaDao.obtenerEvidenciasPendientes()
                        val lugaresProtegidos = evidenciasPendientes.map { it.lugaresVisitasId }.toSet()

                        val lugaresVencidos = listaApi.filter {
                            getFechaStatus(it.fecha) == "PASADA" &&
                                    it.estado?.uppercase(Locale.ROOT) != "NO CULMINADO" &&
                                    it.estado?.uppercase(Locale.ROOT) != "CULMINADO" &&
                                    it.estado?.uppercase(Locale.ROOT) != "COMPLETADO" &&
                                    !lugaresProtegidos.contains(it.id.toString())
                        }.map { it.id }

                        if (lugaresVencidos.isNotEmpty()) {
                            try {
                                RetrofitClient.apiService.marcarLugaresVencidos("Bearer $tokenGuardado", lugaresVencidos)
                                // Actualizamos la lista local en caliente
                                val listaActualizada = listaApi.map { lugar ->
                                    if (lugaresVencidos.contains(lugar.id)) lugar.copy(estado = "NO CULMINADO") else lugar
                                }
                                lugaresVisita = listaActualizada
                            } catch (e: Exception) {
                                lugaresVisita = listaApi
                            }
                        } else {
                            lugaresVisita = listaApi
                        }

                        sharedPrefCache.edit().putString("detalle_${idVisita}", gson.toJson(lugaresVisita)).apply()
                    }
                } catch (e: Exception) {
                    if (lugaresVisita.isEmpty()) errorDetalle = "Sin internet. Mostrando vista local si existe."
                } finally {
                    isLoadingDetalle = false
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGpsEnabled = checkGpsStatusLocal(context)
                isAutoTimeEnabled = checkAutoTimeEnabledLocal(context)
                isAirplaneModeOn = checkAirplaneModeLocal(context)
                cargarDetalle()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(tokenGuardado) {
        val jsonOffline = sharedPrefCache.getString("detalle_${idVisita}", null)
        if (!jsonOffline.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<LugarVisita>>() {}.type
                lugaresVisita = gson.fromJson(jsonOffline, type)
            } catch (e: Exception) {}
        }
        if (tokenGuardado.isNotEmpty() && lugaresVisita.isEmpty()) {
            cargarDetalle()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            bitmapPreview = bitmap
            val now = Date()
            val localeEs = Locale("es", "ES")

            fechaCaptura = SimpleDateFormat("yyyy-MM-dd", localeEs).format(now)
            horaCaptura = SimpleDateFormat("HH:mm:ss", localeEs).format(now)
            anioCaptura = SimpleDateFormat("yyyy", localeEs).format(now)
            mesCaptura = SimpleDateFormat("MMMM", localeEs).format(now).uppercase(localeEs)
            numeroMesCaptura = SimpleDateFormat("M", localeEs).format(now)

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                isGpsCargando = true
                var gpsResuelto = false

                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                    .addOnSuccessListener { location ->
                        if (!gpsResuelto) {
                            gpsResuelto = true
                            if (location != null) {
                                latitudCaptura = location.latitude.toString()
                                longitudCaptura = location.longitude.toString()
                                precisionCaptura = location.accuracy.toString()
                            } else {
                                latitudCaptura = "0.0"; longitudCaptura = "0.0"; precisionCaptura = "0.0"
                            }
                            isGpsCargando = false
                        }
                    }
                    .addOnFailureListener {
                        if (!gpsResuelto) {
                            gpsResuelto = true
                            latitudCaptura = "0.0"; longitudCaptura = "0.0"; precisionCaptura = "0.0"
                            isGpsCargando = false
                        }
                    }

                coroutineScope.launch {
                    kotlinx.coroutines.delay(7000L)
                    if (!gpsResuelto) {
                        gpsResuelto = true

                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                latitudCaptura = lastLoc.latitude.toString()
                                longitudCaptura = lastLoc.longitude.toString()
                                precisionCaptura = lastLoc.accuracy.toString()
                            } else {
                                latitudCaptura = "0.0"; longitudCaptura = "0.0"; precisionCaptura = "0.0"
                            }
                            isGpsCargando = false
                        }.addOnFailureListener {
                            latitudCaptura = "0.0"; longitudCaptura = "0.0"; precisionCaptura = "0.0"
                            isGpsCargando = false
                        }
                    }
                }

            } else {
                latitudCaptura = "0.0"; longitudCaptura = "0.0"; precisionCaptura = "0.0"
                isGpsCargando = false
            }
        } else {
            bitmapPreview = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (cameraGranted) {
            cameraLauncher.launch()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = GrisFondoApp) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp),
                shadowElevation = 8.dp,
                color = AzulPrincipal
            ) {
                Box(modifier = Modifier.padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.25f), CircleShape).size(48.dp)
                        ) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Registro de Evidencias", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(nombrePlanLimpio, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, letterSpacing = (-0.5).sp)
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {

                if (isLoadingDetalle && lugaresVisita.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AzulPrincipal, strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
                    }
                } else if (errorDetalle.isNotEmpty() && lugaresVisita.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(errorDetalle, color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.widthIn(max = 700.dp).fillMaxSize(),
                        contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp, start = 24.dp, end = 24.dp)
                    ) {

                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                AnimatedVisibility(visible = !isOnline) {
                                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = "MODO OFFLINE ACTIVADO", color = Color(0xFFD32F2F), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 0.5.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            if (!isSystemReady) {
                                ElevatedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFFFEBEE)), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Warning, null, tint = Color(0xFFD32F2F))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("ALERTA DE SEGURIDAD", fontWeight = FontWeight.ExtraBold, color = Color(0xFFD32F2F), fontSize = 15.sp)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        if (isAirplaneModeOn) {
                                            Text("El Modo Avión está activado. Cámara bloqueada.", color = Color.DarkGray, fontSize = 13.sp)
                                        } else if (!isAutoTimeEnabled) {
                                            Text("La Hora Automática está apagada. Cámara bloqueada.", color = Color.DarkGray, fontSize = 13.sp)
                                        } else if (!isGpsEnabled) {
                                            Text("El GPS está apagado. Cámara bloqueada.", color = Color.DarkGray, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }

                        items(lugaresVisita) { lugar ->
                            DiaAccordionItem(
                                lugar = lugar,
                                idVisita = idVisita,
                                isExpanded = diaExpandidoId == lugar.id,
                                onToggleExpand = {
                                    bitmapPreview = null
                                    diaExpandidoId = if (diaExpandidoId == lugar.id) null else lugar.id
                                },
                                isSystemReady = isSystemReady,
                                context = context,
                                bitmapPreview = if(previewLugarId == lugar.id) bitmapPreview else null,
                                previewEtapa = previewEtapa,
                                fechaCaptura = fechaCaptura,
                                horaCaptura = horaCaptura,
                                isUploading = isUploading,
                                isGpsCargando = isGpsCargando,
                                latitudCaptura = latitudCaptura,
                                longitudCaptura = longitudCaptura,
                                isOnline = isOnline,
                                refreshTrigger = refreshTrigger,
                                onTomarFoto = { etapa ->
                                    previewLugarId = lugar.id
                                    previewEtapa = etapa
                                    bitmapPreview = null

                                    val hasCameraPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                    if (hasCameraPerm) {
                                        cameraLauncher.launch()
                                    } else {
                                        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION))
                                    }
                                },
                                onGuardar = { etapa ->
                                    coroutineScope.launch {
                                        isUploading = true
                                        serverErrorDetails = ""
                                        try {
                                            val latDestino = lugar.latitud?.toDoubleOrNull()
                                            val lonDestino = lugar.longitud?.toDoubleOrNull()
                                            val latUsuario = latitudCaptura.toDoubleOrNull()
                                            val lonUsuario = longitudCaptura.toDoubleOrNull()

                                            val estadoRed = if (isOnline) "ONLINE" else "OFFLINE"
                                            var observacionFinal = ""

                                            if (latDestino != null && lonDestino != null && latUsuario != null && lonUsuario != null && latUsuario != 0.0) {
                                                val results = FloatArray(1)
                                                Location.distanceBetween(latDestino, lonDestino, latUsuario, lonUsuario, results)
                                                val distanciaMetros = results[0].toInt()
                                                val textoDistancia = if (distanciaMetros <= 300) "Dentro del radio ($distanciaMetros m)" else "Fuera del radio ($distanciaMetros m)"
                                                observacionFinal = "Red: $estadoRed | $textoDistancia"
                                            } else {
                                                observacionFinal = "Red: $estadoRed | Distancia desconocida"
                                            }

                                            val nombreArchivo = "visita_${idVisita}_dia_${lugar.id}_${etapa.lowercase()}.jpg"
                                            val fileFoto = bitmapToFile(context, bitmapPreview!!, nombreArchivo)

                                            val estadoVisitaReal = if (etapa.uppercase(Locale.ROOT) == "SALIDA") "CULMINADO" else "PROCESO"

                                            if (isOnline) {
                                                val response = RetrofitClient.apiService.guardarVisita(
                                                    token = "Bearer $tokenGuardado",
                                                    planId = idVisita.toRequestBody("text/plain".toMediaTypeOrNull()),
                                                    lugaresVisitasId = lugar.id.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                                                    estadoVisita = estadoVisitaReal.toRequestBody("text/plain".toMediaTypeOrNull()),
                                                    usuarioId = nicknameUsuario.toRequestBody("text/plain".toMediaTypeOrNull()),
                                                    estado = etapa.toRequestBody("text/plain".toMediaTypeOrNull()),
                                                    fecha = fechaCaptura.toRequestBody("text/plain".toMediaTypeOrNull()),
                                                    hora = horaCaptura.toRequestBody("text/plain".toMediaTypeOrNull()),
                                                    anio = anioCaptura.toRequestBody("text/plain".toMediaTypeOrNull()),
                                                    mes = mesCaptura.toRequestBody("text/plain".toMediaTypeOrNull()),
                                                    numeroMes = numeroMesCaptura.toRequestBody("text/plain".toMediaTypeOrNull()),
                                                    latitud = latitudCaptura.toRequestBody("text/plain".toMediaTypeOrNull()),
                                                    longitud = longitudCaptura.toRequestBody("text/plain".toMediaTypeOrNull()),
                                                    precisionGps = precisionCaptura.toRequestBody("text/plain".toMediaTypeOrNull()),
                                                    observacion = observacionFinal.toRequestBody("text/plain".toMediaTypeOrNull()),
                                                    foto = MultipartBody.Part.createFormData("foto", fileFoto.name, fileFoto.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                                                )

                                                if (!response.isSuccessful) {
                                                    serverErrorDetails = "Fallo al subir evidencia. Código HTTP: ${response.code()}"
                                                    return@launch
                                                }
                                                mensajeExitoDialog = "La evidencia de $etapa se subió correctamente."
                                            } else {
                                                val nuevaEvidenciaOffline = VisitaEvidenciaEntity(
                                                    planId = idVisita,
                                                    lugaresVisitasId = lugar.id.toString(),
                                                    estadoVisita = estadoVisitaReal,
                                                    usuarioId = nicknameUsuario,
                                                    estado = etapa,
                                                    fecha = fechaCaptura,
                                                    hora = horaCaptura,
                                                    anio = anioCaptura,
                                                    mes = mesCaptura,
                                                    numeroMes = numeroMesCaptura,
                                                    latitud = latitudCaptura,
                                                    longitud = longitudCaptura,
                                                    precisionGps = precisionCaptura,
                                                    observacion = observacionFinal,
                                                    rutaFotoLocal = fileFoto.absolutePath
                                                )
                                                visitaDao.insertarEvidencia(nuevaEvidenciaOffline)
                                                mensajeExitoDialog = "Modo Offline. Evidencia guardada en el dispositivo de forma segura."
                                            }

                                            val sharedPref = context.getSharedPreferences("EstadoVisitas", Context.MODE_PRIVATE)
                                            val nuevoEstadoGlobal = if (etapa == "SALIDA") "COMPLETADO" else etapa

                                            sharedPref.edit()
                                                .putBoolean("visita_${idVisita}_lugar_${lugar.id}_$etapa", true)
                                                .putString("visita_${idVisita}_lugar_${lugar.id}_${etapa}_hora", horaCaptura)
                                                .putString("visita_${idVisita}", nuevoEstadoGlobal)
                                                .apply()

                                            bitmapPreview = null
                                            refreshTrigger++
                                            mostrarExitoDialog = true

                                        } catch (e: Exception) {
                                            serverErrorDetails = "Problema de red o aplicación:\n\n${e.localizedMessage}"
                                        } finally {
                                            isUploading = false
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }
        }

        if (mostrarExitoDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (isOnline) Icons.Filled.CheckCircle else Icons.Filled.CloudOff, null, tint = if(isOnline) Color(0xFF4CAF50) else Color(0xFFF57C00))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (isOnline) "Guardado Exitoso" else "Guardado Local", color = AsideFondo, fontWeight = FontWeight.ExtraBold)
                    }
                },
                text = { Text(mensajeExitoDialog, color = GrisTexto, fontSize = 15.sp) },
                confirmButton = {
                    Button(onClick = {
                        mostrarExitoDialog = false
                        cargarDetalle() // Recargar datos
                    }, colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal), shape = RoundedCornerShape(12.dp)) {
                        Text("CONTINUAR", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 8.dp
            )
        }

        if (serverErrorDetails.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { serverErrorDetails = "" },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Error de Conexión", color = Color.Red, fontWeight = FontWeight.ExtraBold)
                    }
                },
                text = { Text(serverErrorDetails, fontSize = 13.sp) },
                confirmButton = { TextButton(onClick = { serverErrorDetails = "" }) { Text("CERRAR", fontWeight = FontWeight.Bold) } },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun DiaAccordionItem(
    lugar: LugarVisita, idVisita: String, isExpanded: Boolean, onToggleExpand: () -> Unit,
    isSystemReady: Boolean, context: Context,
    bitmapPreview: Bitmap?, previewEtapa: String, fechaCaptura: String, horaCaptura: String,
    isUploading: Boolean, isGpsCargando: Boolean, latitudCaptura: String, longitudCaptura: String, isOnline: Boolean,
    refreshTrigger: Int,
    onTomarFoto: (String) -> Unit, onGuardar: (String) -> Unit
) {
    val sharedPref = context.getSharedPreferences("EstadoVisitas", Context.MODE_PRIVATE)

    remember(refreshTrigger) { refreshTrigger }

    val estadoBackend = lugar.estado?.uppercase(Locale.ROOT) ?: "PENDIENTE"

    val pesoBackend = when(estadoBackend) {
        "COMPLETADO", "SALIDA", "CULMINADO", "FINALIZÓ", "CULMINO" -> 3
        "MEDIO" -> 2
        "ENTRADA", "PROGRESO", "PROCESO" -> 1
        else -> 0
    }

    val entradaGuardada = sharedPref.getBoolean("visita_${idVisita}_lugar_${lugar.id}_ENTRADA", false) || pesoBackend >= 1
    val medioGuardado = sharedPref.getBoolean("visita_${idVisita}_lugar_${lugar.id}_MEDIO", false) || pesoBackend >= 2
    val salidaGuardada = sharedPref.getBoolean("visita_${idVisita}_lugar_${lugar.id}_SALIDA", false) || pesoBackend >= 3

    val horaEntrada = sharedPref.getString("visita_${idVisita}_lugar_${lugar.id}_ENTRADA_hora", "") ?: ""
    val horaMedio = sharedPref.getString("visita_${idVisita}_lugar_${lugar.id}_MEDIO_hora", "") ?: ""
    val horaSalida = sharedPref.getString("visita_${idVisita}_lugar_${lugar.id}_SALIDA_hora", "") ?: ""

    val etapaActiva = when {
        !entradaGuardada -> "ENTRADA"
        !medioGuardado -> "MEDIO"
        !salidaGuardada -> "SALIDA"
        else -> "COMPLETADO"
    }

    val fechaStatus = getFechaStatus(lugar.fecha)
    val isDateValid = fechaStatus == "HOY"

    val visitaStatusText: String
    val visitaStatusColor: Color

    if (etapaActiva == "COMPLETADO" && estadoBackend != "NO CULMINADO") {
        visitaStatusText = "CULMINADO"
        visitaStatusColor = Color(0xFF2E7D32)
    } else {
        if (fechaStatus == "PASADA" || estadoBackend == "NO CULMINADO") {
            visitaStatusText = "NO CULMINADO"
            visitaStatusColor = Color(0xFFD32F2F)
        } else if (entradaGuardada || medioGuardado || salidaGuardada) {
            visitaStatusText = "PROCESO"
            visitaStatusColor = Color(0xFFF57C00)
        } else {
            visitaStatusText = "PENDIENTE"
            visitaStatusColor = AzulPrincipal
        }
    }

    val iconRotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.elevatedCardColors(containerColor = if (etapaActiva == "COMPLETADO" && estadoBackend != "NO CULMINADO") Color(0xFFF1F8E9) else Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggleExpand() }.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(48.dp).background(if (etapaActiva == "COMPLETADO" && estadoBackend != "NO CULMINADO") Color(0xFF4CAF50) else AzulPrincipal.copy(alpha=0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    if (etapaActiva == "COMPLETADO" && estadoBackend != "NO CULMINADO") Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    else Icon(Icons.Filled.DateRange, null, tint = AzulPrincipal, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Surface(color = visitaStatusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = visitaStatusText,
                            color = visitaStatusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Fecha: ${lugar.fecha}", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = AsideFondo, letterSpacing = (-0.2).sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(lugar.lugares, color = AsideFondo, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)

                    if (!lugar.lugar_gps.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(lugar.lugar_gps, color = GrisTexto, fontSize = 13.sp, lineHeight = 16.sp)
                    }
                }

                FilledTonalIconButton(
                    onClick = { abrirGoogleMaps(context, lugar.latitud, lugar.longitud, lugar.lugares) },
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = AzulPrincipal.copy(alpha = 0.1f),
                        contentColor = AzulPrincipal
                    )
                ) {
                    Icon(imageVector = Icons.Rounded.Map, contentDescription = "Ver Mapa", modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = GrisTexto, modifier = Modifier.rotate(iconRotation).size(28.dp))
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFFAFAFA)).padding(20.dp)) {
                    if (etapaActiva == "COMPLETADO" && estadoBackend != "NO CULMINADO") {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Evidencias Finalizadas", color = Color(0xFF2E7D32), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            CapturedPhotoItem("ENTRADA", horaEntrada, idVisita, lugar.id, context, refreshTrigger)
                            CapturedPhotoItem("MEDIO", horaMedio, idVisita, lugar.id, context, refreshTrigger)
                            CapturedPhotoItem("SALIDA", horaSalida, idVisita, lugar.id, context, refreshTrigger)
                        }
                    } else {
                        // AQUÍ PASAMOS EL estadoBackend AL NODO DE LA LÍNEA DE TIEMPO
                        TimelineNode("1. ENTRADA", entradaGuardada, etapaActiva == "ENTRADA", isSystemReady, isDateValid, fechaStatus, estadoBackend, isUploading, isGpsCargando, latitudCaptura, longitudCaptura, if (previewEtapa == "ENTRADA") bitmapPreview else null, fechaCaptura, horaCaptura, horaEntrada, { onTomarFoto("ENTRADA") }, { onGuardar("ENTRADA") }, false)
                        TimelineNode("2. MEDIO", medioGuardado, etapaActiva == "MEDIO", isSystemReady, isDateValid, fechaStatus, estadoBackend, isUploading, isGpsCargando, latitudCaptura, longitudCaptura, if (previewEtapa == "MEDIO") bitmapPreview else null, fechaCaptura, horaCaptura, horaMedio, { onTomarFoto("MEDIO") }, { onGuardar("MEDIO") }, false)
                        TimelineNode("3. SALIDA", salidaGuardada, etapaActiva == "SALIDA", isSystemReady, isDateValid, fechaStatus, estadoBackend, isUploading, isGpsCargando, latitudCaptura, longitudCaptura, if (previewEtapa == "SALIDA") bitmapPreview else null, fechaCaptura, horaCaptura, horaSalida, { onTomarFoto("SALIDA") }, { onGuardar("SALIDA") }, true)
                    }
                }
            }
        }
    }
}

@Composable
fun CapturedPhotoItem(etapa: String, hora: String, idVisita: String, idLugar: Int, context: Context, refreshTrigger: Int) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(idVisita, idLugar, etapa, refreshTrigger) {
        bitmap = withContext(Dispatchers.IO) {
            getSavedBitmap(context, idVisita, idLugar, etapa)
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // CORRECCIÓN VISUAL: Ícono exitoso de Nube si la foto ya se envió al servidor y no está local.
                Box(modifier = Modifier.size(64.dp).background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.CloudDone, contentDescription = "Sincronizado", tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "Registro $etapa", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = AzulPrincipal)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = GrisTexto)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = hora.ifEmpty { "Sincronizado" }, fontSize = 13.sp, color = GrisTexto, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun TimelineNode(titulo: String, isGuardado: Boolean, isActive: Boolean, isSystemReady: Boolean, isDateValid: Boolean, fechaStatus: String, estadoBackend: String, isUploading: Boolean, isGpsCargando: Boolean, latitudCaptura: String, longitudCaptura: String, bitmapActual: Bitmap?, fechaCaptura: String, horaCaptura: String, horaGuardada: String, onTomarFoto: () -> Unit, onGuardar: () -> Unit, isLast: Boolean) {
    val isCaducado = fechaStatus == "PASADA" || estadoBackend == "NO CULMINADO"

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
            val colorCirculo = if (isGuardado) Color(0xFF4CAF50) else if (isActive && !isCaducado) AzulPrincipal else if (isCaducado && !isGuardado) Color(0xFFD32F2F).copy(alpha = 0.2f) else Color.LightGray

            Box(modifier = Modifier.size(24.dp).background(colorCirculo, CircleShape).border(3.dp, Color.White, CircleShape)) {
                if (isGuardado) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp).align(Alignment.Center))
                else if (isCaducado && !isGuardado) Icon(Icons.Filled.Close, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(14.dp).align(Alignment.Center))
            }
            if (!isLast) {
                val colorLinea = if (isGuardado) Color(0xFF4CAF50).copy(alpha = 0.5f) else if (isCaducado && !isGuardado) Color(0xFFD32F2F).copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.5f)
                Box(modifier = Modifier.weight(1f).width(2.5.dp).background(colorLinea))
            }
        }

        val cardAlpha = if (isGuardado || (isActive && !isCaducado)) 1f else 0.5f

        ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp, start = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = if(isActive && !isCaducado) Color.White else if(isGuardado) Color(0xFFFAFAFA) else Color.Transparent),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = if(isActive && !isCaducado) 6.dp else 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp).alpha(cardAlpha)) {
                Text(text = titulo, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = if (isGuardado) Color(0xFF2E7D32) else AsideFondo)
                Spacer(modifier = Modifier.height(10.dp))

                if (isActive && !isCaducado) {
                    if (!isDateValid) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Block, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Aún no es la fecha programada.", color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isUploading) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AzulPrincipal, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                        }
                    } else if (bitmapActual != null) {
                        Image(bitmap = bitmapActual.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)

                        Column(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth().background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AccessTime, null, tint = GrisTexto, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Capturado: $horaCaptura", color = GrisTexto, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, null, tint = if(isGpsCargando) Color(0xFFE65100) else Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if(isGpsCargando) "Obteniendo coordenadas satelitales..." else "GPS: $latitudCaptura, $longitudCaptura", color = if(isGpsCargando) Color(0xFFE65100) else Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onTomarFoto, enabled = isSystemReady) { Text("REPETIR", color = AzulPrincipal, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold) }

                            if (isGpsCargando) {
                                Button(onClick = {}, enabled = false, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)) {
                                    Text("UBICANDO...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            } else {
                                Button(onClick = onGuardar, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)) {
                                    Text("GUARDAR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp)
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onTomarFoto, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), enabled = isSystemReady,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulPrincipal),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, if(!isSystemReady) Color.Red.copy(alpha=0.3f) else AzulPrincipal.copy(alpha=0.5f))
                        ) {
                            Icon(Icons.Filled.PhotoCamera, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("TOMAR FOTO", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                        }
                    }
                } else if (!isGuardado) {
                    if (isCaducado) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Cancel, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Registro omitido", color = Color(0xFFD32F2F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bloqueado", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if(horaGuardada.isNotEmpty()) "Guardado a las $horaGuardada" else "Completado en servidor", color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

fun abrirGoogleMaps(context: Context, latitud: String?, longitud: String?, titulo: String) {
    if (latitud.isNullOrBlank() || longitud.isNullOrBlank() || latitud == "0.0" || latitud == "null") {
        Toast.makeText(context, "Este lugar aún no tiene coordenadas GPS registradas.", Toast.LENGTH_LONG).show()
        return
    }

    try {
        val gmmIntentUri = Uri.parse("geo:$latitud,$longitud?q=$latitud,$longitud(${Uri.encode(titulo)})")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitud,$longitud")
        val webIntent = Intent(Intent.ACTION_VIEW, webUri)
        context.startActivity(webIntent)
    }
}

fun getSavedBitmap(context: Context, idVisita: String, idLugar: Int, etapa: String): Bitmap? {
    val fileName = "visita_${idVisita}_dia_${idLugar}_${etapa.lowercase()}.jpg"
    val file = File(context.filesDir, fileName)
    if (!file.exists()) return null
    val options = BitmapFactory.Options().apply { inSampleSize = 8 }
    return BitmapFactory.decodeFile(file.absolutePath, options)
}

fun bitmapToFile(context: Context, bitmap: Bitmap, fileName: String): File {
    val file = File(context.filesDir, fileName)
    file.createNewFile()
    val bos = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos)
    bos.flush()
    bos.close()
    return file
}

fun getFechaStatus(fechaLugar: String): String {
    try {
        val formatDB = if (fechaLugar.contains("/")) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateLugar = formatDB.parse(fechaLugar) ?: return "HOY"
        val sdfHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateHoy = sdfHoy.parse(sdfHoy.format(Date())) ?: return "HOY"
        return when {
            dateLugar.before(dateHoy) -> "PASADA"
            dateLugar.after(dateHoy) -> "FUTURA"
            else -> "HOY"
        }
    } catch (e: Exception) {
        return "HOY"
    }
}

fun checkGpsStatusLocal(context: Context): Boolean = (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.GPS_PROVIDER)
fun checkAutoTimeEnabledLocal(context: Context): Boolean = try { Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME) == 1 } catch (e: Exception) { false }
fun checkAirplaneModeLocal(context: Context): Boolean = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0