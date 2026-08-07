package com.ugelaa.monitoreo.ui.theme.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.ugelaa.monitoreo.data.RetrofitClient
import com.ugelaa.monitoreo.ui.theme.AsideFondo
import com.ugelaa.monitoreo.ui.theme.AzulPrincipal
import com.ugelaa.monitoreo.ui.theme.GrisFondoApp
import com.ugelaa.monitoreo.ui.theme.GrisTexto
import com.ugelaa.monitoreo.utils.SessionManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
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

    val nicknameUsuario by sessionManager.getNickname.collectAsState(initial = "Cargando...")
    val tokenGuardado by sessionManager.getToken.collectAsState(initial = "")

    var isGpsEnabled by remember { mutableStateOf(checkGpsStatusLocal(context)) }
    var isAutoTimeEnabled by remember { mutableStateOf(checkAutoTimeEnabledLocal(context)) }
    val isSystemReady = isGpsEnabled && isAutoTimeEnabled

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGpsEnabled = checkGpsStatusLocal(context)
                isAutoTimeEnabled = checkAutoTimeEnabledLocal(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var faseActual by remember { mutableStateOf(1) }
    var isUploading by remember { mutableStateOf(false) }

    // ¡NUEVA VARIABLE PARA CAPTURAR EL ERROR EXACTO!
    var serverErrorDetails by remember { mutableStateOf("") }

    var bitmapEntrada by remember { mutableStateOf<Bitmap?>(null) }
    var latitudEntrada by remember { mutableStateOf("") }
    var fechaEntrada by remember { mutableStateOf("") }
    var horaEntrada by remember { mutableStateOf("") }

    var bitmapSalida by remember { mutableStateOf<Bitmap?>(null) }
    var latitudSalida by remember { mutableStateOf("") }
    var fechaSalida by remember { mutableStateOf("") }
    var horaSalida by remember { mutableStateOf("") }

    val cameraLauncherEntrada = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            bitmapEntrada = bitmap
            val now = Date()
            fechaEntrada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
            horaEntrada = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
        }
    }
    val cameraLauncherSalida = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            bitmapSalida = bitmap
            val now = Date()
            fechaSalida = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
            horaSalida = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = GrisFondoApp) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(modifier = Modifier.fillMaxWidth().background(AzulPrincipal, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)).padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape).size(44.dp)) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(if (faseActual == 1) "Fase 1: Entrada" else if (faseActual == 2) "Fase 2: Salida" else "Fase 3: Consolidado", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {

                if (!isSystemReady) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFFFEBEE))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, null, tint = Color(0xFFD32F2F))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("¡ALERTA DE SEGURIDAD!", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (!isAutoTimeEnabled) {
                                Text("Has desactivado la 'Hora Automática'. La cámara ha sido bloqueada. Actívala en los ajustes.", color = Color.DarkGray, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_DATE_SETTINGS)) }) { Text("ARREGLAR HORA", color = AzulPrincipal) }
                            } else if (!isGpsEnabled) {
                                Text("Has apagado el GPS. La cámara ha sido bloqueada. Enciende tu GPS para continuar.", color = Color.DarkGray, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }) { Text("ENCENDER GPS", color = AzulPrincipal) }
                            }
                        }
                    }
                }

                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Visita ID: $idVisita", color = AzulPrincipal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(nombrePlan, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = AsideFondo, lineHeight = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // FASE 1: ENTRADA
                if (faseActual == 1) {
                    Text("Paso 1: Foto de Llegada", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = AsideFondo)
                    Spacer(modifier = Modifier.height(16.dp))
                    UploadZone("Registro de Entrada", "Toma una foto en la puerta.", bitmapEntrada, fechaEntrada, horaEntrada, isSystemReady) { if(isSystemReady) cameraLauncherEntrada.launch() }
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { latitudEntrada = "-5.90123"; faseActual = 2 },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = bitmapEntrada != null && isSystemReady,
                        colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal)
                    ) { Text("GUARDAR ENTRADA Y CONTINUAR", color = Color.White, fontWeight = FontWeight.Bold) }
                }

                // FASE 2: SALIDA
                if (faseActual == 2) {
                    Text("Paso 2: Foto de Retiro", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = AsideFondo)
                    Spacer(modifier = Modifier.height(16.dp))
                    UploadZone("Registro de Salida", "Toma una foto al finalizar tu jornada.", bitmapSalida, fechaSalida, horaSalida, isSystemReady) { if(isSystemReady) cameraLauncherSalida.launch() }
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { latitudSalida = "-5.90150"; faseActual = 3 },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = bitmapSalida != null && isSystemReady,
                        colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal)
                    ) { Text("GUARDAR SALIDA Y VER RESUMEN", color = Color.White, fontWeight = FontWeight.Bold) }
                }

                // FASE 3: RESUMEN Y ENVÍO A LARAVEL
                if (faseActual == 3) {
                    Text("Resumen de Evidencias", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = AsideFondo)
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ResumenFotoItem("Entrada", bitmapEntrada, fechaEntrada, horaEntrada)
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = GrisFondoApp)
                            Spacer(modifier = Modifier.height(16.dp))
                            ResumenFotoItem("Salida", bitmapSalida, fechaSalida, horaSalida)
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))

                    if (isUploading) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AzulPrincipal)
                        }
                    } else {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isUploading = true
                                    serverErrorDetails = "" // Limpiamos errores anteriores
                                    try {
                                        // 1. Preparar y Enviar Entrada
                                        val fileEntrada = bitmapToFile(context, bitmapEntrada!!, "entrada.jpg")
                                        val respEntrada = RetrofitClient.apiService.guardarVisita(
                                            token = "Bearer $tokenGuardado",
                                            planId = idVisita.toRequestBody("text/plain".toMediaTypeOrNull()),
                                            usuarioId = nicknameUsuario.toRequestBody("text/plain".toMediaTypeOrNull()),
                                            estado = "Entrada".toRequestBody("text/plain".toMediaTypeOrNull()),
                                            fecha = fechaEntrada.toRequestBody("text/plain".toMediaTypeOrNull()),
                                            hora = horaEntrada.toRequestBody("text/plain".toMediaTypeOrNull()),
                                            latitud = latitudEntrada.toRequestBody("text/plain".toMediaTypeOrNull()),
                                            longitud = "-76.11000".toRequestBody("text/plain".toMediaTypeOrNull()),
                                            precisionGps = "15.0".toRequestBody("text/plain".toMediaTypeOrNull()),
                                            foto = MultipartBody.Part.createFormData("foto", fileEntrada.name, fileEntrada.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                                        )

                                        if (!respEntrada.isSuccessful) {
                                            // ¡ATRAPAMOS EL ERROR DEL SERVIDOR!
                                            val errorBody = respEntrada.errorBody()?.string() ?: "El servidor no envió detalles."
                                            serverErrorDetails = "FALLÓ ENTRADA (Código ${respEntrada.code()}):\n\n$errorBody"
                                            isUploading = false
                                            return@launch // Detenemos todo
                                        }

                                        // 2. Preparar y Enviar Salida
                                        val fileSalida = bitmapToFile(context, bitmapSalida!!, "salida.jpg")
                                        val respSalida = RetrofitClient.apiService.guardarVisita(
                                            token = "Bearer $tokenGuardado",
                                            planId = idVisita.toRequestBody("text/plain".toMediaTypeOrNull()),
                                            usuarioId = nicknameUsuario.toRequestBody("text/plain".toMediaTypeOrNull()),
                                            estado = "Salida".toRequestBody("text/plain".toMediaTypeOrNull()),
                                            fecha = fechaSalida.toRequestBody("text/plain".toMediaTypeOrNull()),
                                            hora = horaSalida.toRequestBody("text/plain".toMediaTypeOrNull()),
                                            latitud = latitudSalida.toRequestBody("text/plain".toMediaTypeOrNull()),
                                            longitud = "-76.11025".toRequestBody("text/plain".toMediaTypeOrNull()),
                                            precisionGps = "12.5".toRequestBody("text/plain".toMediaTypeOrNull()),
                                            foto = MultipartBody.Part.createFormData("foto", fileSalida.name, fileSalida.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                                        )

                                        if (!respSalida.isSuccessful) {
                                            // ¡ATRAPAMOS EL ERROR DEL SERVIDOR!
                                            val errorBody = respSalida.errorBody()?.string() ?: "El servidor no envió detalles."
                                            serverErrorDetails = "FALLÓ SALIDA (Código ${respSalida.code()}):\n\n$errorBody"
                                            isUploading = false
                                            return@launch // Detenemos todo
                                        }

                                        // SI LLEGA AQUÍ, TODO FUE UN ÉXITO
                                        Toast.makeText(context, "Evidencias enviadas correctamente", Toast.LENGTH_LONG).show()
                                        navController.popBackStack()

                                    } catch (e: Exception) {
                                        // ¡ATRAPAMOS ERRORES DE RED O DE KOTLIN!
                                        serverErrorDetails = "EXCEPCIÓN DE APP/RED:\n\n${e.toString()}"
                                    } finally {
                                        isUploading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = isSystemReady,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) { Text("FINALIZAR Y ENVIAR EVIDENCIAS", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        // ==========================================
        // ¡LA CONSOLA DE DEPURACIÓN (DEBUGGER)!
        // ==========================================
        if (serverErrorDetails.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { serverErrorDetails = "" },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Detalles del Error", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Surface(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                            Text(
                                text = serverErrorDetails,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { serverErrorDetails = "" }) {
                        Text("CERRAR DEBUGGER", color = AzulPrincipal)
                    }
                }
            )
        }
    }
}

// --- HERRAMIENTA PARA CONVERTIR FOTOS ---
fun bitmapToFile(context: Context, bitmap: Bitmap, fileName: String): File {
    val file = File(context.cacheDir, fileName)
    file.createNewFile()
    val bos = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos) // 80% de calidad para no saturar la red
    bos.flush()
    bos.close()
    return file
}

fun checkGpsStatusLocal(context: Context): Boolean = (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.GPS_PROVIDER)
fun checkAutoTimeEnabledLocal(context: Context): Boolean = try { Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME) == 1 } catch (e: Exception) { false }

@Composable
fun UploadZone(titulo: String, descripcion: String, bitmap: Bitmap?, fechaCaptura: String, horaCaptura: String, isEnabled: Boolean, onClick: () -> Unit) {
    val borderColor = if (bitmap != null) Color(0xFF4CAF50) else if (!isEnabled) Color.Red.copy(alpha=0.5f) else AzulPrincipal.copy(alpha = 0.5f)
    val bgColor = if (bitmap != null) Color(0xFFE8F5E9) else if (!isEnabled) Color(0xFFFFEBEE) else Color.White

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = bgColor), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(if(!isEnabled) Color.Red.copy(alpha=0.1f) else AzulPrincipal.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CameraAlt, null, tint = if(!isEnabled) Color.Red else AzulPrincipal, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (bitmap != null) Color(0xFF2E7D32) else AsideFondo)
                    if (bitmap == null) Text(text = if(!isEnabled) "Sistema bloqueado: Revisa los ajustes" else descripcion, color = GrisTexto, fontSize = 12.sp)
                }
                if (bitmap != null) Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50))
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (bitmap != null) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                Row(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth().background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp)).padding(8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccessTime, null, tint = GrisTexto, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Capturado el: $fechaCaptura a las $horaCaptura", color = GrisTexto, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = onClick, modifier = Modifier.align(Alignment.End), enabled = isEnabled) { Text("REPETIR FOTO", color = AzulPrincipal, fontSize = 12.sp) }
            } else {
                OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(8.dp), enabled = isEnabled, colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulPrincipal, containerColor = Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, if(bitmap != null) Color(0xFF4CAF50) else if(!isEnabled) Color.Red.copy(alpha=0.3f) else AzulPrincipal.copy(alpha=0.5f))) {
                    Icon(Icons.Filled.PhotoCamera, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("TOMAR FOTOGRAFÍA", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ResumenFotoItem(titulo: String, bitmap: Bitmap?, fecha: String, hora: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (bitmap != null) Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
        else Box(modifier = Modifier.size(70.dp).background(Color.LightGray, RoundedCornerShape(8.dp)))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = "Registro de $titulo", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AsideFondo)
            Text(text = "✓ $fecha | $hora", color = Color(0xFF2E7D32), fontSize = 12.sp)
        }
    }
}