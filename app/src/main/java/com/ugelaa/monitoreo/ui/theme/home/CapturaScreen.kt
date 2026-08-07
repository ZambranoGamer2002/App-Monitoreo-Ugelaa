package com.ugelaa.monitoreo.ui.theme.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

    val nombrePlanLimpio = remember(nombrePlan) {
        try { URLDecoder.decode(nombrePlan, "UTF-8") } catch (e: Exception) { nombrePlan.replace("+", " ") }
    }

    val nicknameUsuario by sessionManager.getNickname.collectAsState(initial = "Cargando...")
    val tokenGuardado by sessionManager.getToken.collectAsState(initial = "")

    // MEMORIA PERSISTENTE
    val sharedPref = context.getSharedPreferences("EstadoVisitas", Context.MODE_PRIVATE)
    var estadoActual by remember { mutableStateOf(sharedPref.getString("visita_$idVisita", "ENTRADA") ?: "ENTRADA") }

    // SEGURIDAD ANTI-TRAMPAS
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

    // VARIABLES DE INTERFAZ Y FLUJO
    var isUploading by remember { mutableStateOf(false) }
    var serverErrorDetails by remember { mutableStateOf("") }

    var mostrarExitoDialog by remember { mutableStateOf(false) }
    var mensajeExitoDialog by remember { mutableStateOf("") }
    var mostrarTransicionSalida by remember { mutableStateOf(false) } // Controla si se bloquea la Entrada

    var bitmapCaptura by remember { mutableStateOf<Bitmap?>(null) }
    var fechaCaptura by remember { mutableStateOf("") }
    var horaCaptura by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            bitmapCaptura = bitmap
            val now = Date()
            fechaCaptura = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
            horaCaptura = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = GrisFondoApp) {
        Column(modifier = Modifier.fillMaxSize()) {

            // CABECERA
            Box(modifier = Modifier.fillMaxWidth().background(AzulPrincipal, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)).padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape).size(44.dp)) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (estadoActual == "COMPLETADO") "Visita Finalizada" else "Registro de $estadoActual",
                        color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {

                if (!isSystemReady && estadoActual != "COMPLETADO") {
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
                        Text(nombrePlanLimpio, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = AsideFondo, lineHeight = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ==========================================
                // LÓGICA DE VISTAS
                // ==========================================

                if (estadoActual == "COMPLETADO") {
                    // PANTALLA COMPLETADA
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Completado", tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("¡Proceso Finalizado!", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Se han enviado exitosamente las evidencias de ENTRADA y SALIDA para este plan de monitoreo.",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = Color.DarkGray, fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("VOLVER AL INICIO", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // ESTAMOS EN FORMULARIO
                    val esEntradaBloqueada = (estadoActual == "ENTRADA" && mostrarTransicionSalida)
                    val descripcionTexto = if (estadoActual == "ENTRADA") "Toma una foto en la puerta o inicio del evento." else "Toma una foto al finalizar tu jornada de monitoreo."

                    UploadZone(
                        titulo = "Registro de $estadoActual",
                        descripcion = descripcionTexto,
                        bitmap = bitmapCaptura,
                        fechaCaptura = fechaCaptura,
                        horaCaptura = horaCaptura,
                        isEnabled = isSystemReady && !isUploading && !esEntradaBloqueada,
                        isSaved = esEntradaBloqueada
                    ) {
                        if(isSystemReady && !esEntradaBloqueada) cameraLauncher.launch()
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (isUploading) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AzulPrincipal)
                        }
                    } else if (esEntradaBloqueada) {
                        // LA ENTRADA YA FUE GUARDADA - BOTÓN PARA AVANZAR A LA SALIDA
                        Button(
                            onClick = {

                                mostrarTransicionSalida = false
                                estadoActual = "SALIDA"
                                bitmapCaptura = null
                                fechaCaptura = ""
                                horaCaptura = ""
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal)
                        ) { Text("CONTINUAR A SALIDA", color = Color.White, fontWeight = FontWeight.Bold) }
                    } else {
                        // BOTÓN NORMAL DE GUARDADO
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isUploading = true
                                    serverErrorDetails = ""
                                    try {
                                        val fileFoto = bitmapToFile(context, bitmapCaptura!!, "${estadoActual.lowercase()}.jpg")

                                        val response = RetrofitClient.apiService.guardarVisita(
                                            token = "Bearer $tokenGuardado",
                                            planId = idVisita.toRequestBody("text/plain".toMediaTypeOrNull()),
                                            usuarioId = nicknameUsuario.toRequestBody("text/plain".toMediaTypeOrNull()),
                                            estado = estadoActual.toRequestBody("text/plain".toMediaTypeOrNull()),
                                            fecha = fechaCaptura.toRequestBody("text/plain".toMediaTypeOrNull()),
                                            hora = horaCaptura.toRequestBody("text/plain".toMediaTypeOrNull()),
                                            latitud = "-5.90123".toRequestBody("text/plain".toMediaTypeOrNull()),
                                            longitud = "-76.11000".toRequestBody("text/plain".toMediaTypeOrNull()),
                                            precisionGps = "15.0".toRequestBody("text/plain".toMediaTypeOrNull()),
                                            foto = MultipartBody.Part.createFormData("foto", fileFoto.name, fileFoto.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                                        )

                                        if (!response.isSuccessful) {
                                            val errorBody = response.errorBody()?.string() ?: "Sin detalles."
                                            serverErrorDetails = "FALLÓ $estadoActual (Código ${response.code()}):\n\n$errorBody"
                                        } else {
                                            if (estadoActual == "ENTRADA") {

                                                sharedPref.edit().putString("visita_$idVisita", "SALIDA").apply()
                                                mensajeExitoDialog = "El registro de ENTRADA se guardó correctamente en el sistema."
                                                mostrarExitoDialog = true
                                                mostrarTransicionSalida = true
                                            } else {

                                                sharedPref.edit().putString("visita_$idVisita", "COMPLETADO").apply()
                                                mensajeExitoDialog = "El registro de SALIDA se guardó correctamente."
                                                mostrarExitoDialog = true
                                            }
                                        }
                                    } catch (e: Exception) {
                                        serverErrorDetails = "EXCEPCIÓN DE APP/RED:\n\n${e.toString()}"
                                    } finally {
                                        isUploading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = bitmapCaptura != null && isSystemReady,
                            colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal)
                        ) { Text("GUARDAR $estadoActual AHORA", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        // ==========================================
        // DIÁLOGO DE ÉXITO SIMPLE (SOLO "FINALIZAR")
        // ==========================================
        if (mostrarExitoDialog) {
            AlertDialog(
                onDismissRequest = { /* No hacer nada al tocar fuera, obliga a darle a Finalizar */ },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("¡Guardado Exitoso!", color = AsideFondo, fontWeight = FontWeight.Bold)
                    }
                },
                text = { Text(mensajeExitoDialog, color = GrisTexto) },
                confirmButton = {
                    Button(
                        onClick = {
                            mostrarExitoDialog = false
                            if (estadoActual == "SALIDA") {
                                estadoActual = "COMPLETADO" // Actualiza la pantalla a verde final
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal)
                    ) {
                        Text("FINALIZAR", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }

        //CONSOLA DE DEPURACIÓN (DEBUGGER)
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
                    Surface(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp), color = Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                            Text(text = serverErrorDetails, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { serverErrorDetails = "" }) { Text("CERRAR DEBUGGER", color = AzulPrincipal) } }
            )
        }
    }
}

//HERRAMIENTA PARA CONVERTIR FOTOS
fun bitmapToFile(context: Context, bitmap: Bitmap, fileName: String): File {
    val file = File(context.cacheDir, fileName)
    file.createNewFile()
    val bos = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos)
    bos.flush()
    bos.close()
    return file
}

fun checkGpsStatusLocal(context: Context): Boolean = (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.GPS_PROVIDER)
fun checkAutoTimeEnabledLocal(context: Context): Boolean = try { Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME) == 1 } catch (e: Exception) { false }

//ACTUALIZACIÓN DE UPLOADZONE PARA SOPORTAR ESTADO "GUARDADO"
@Composable
fun UploadZone(titulo: String, descripcion: String, bitmap: Bitmap?, fechaCaptura: String, horaCaptura: String, isEnabled: Boolean, isSaved: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSaved || bitmap != null) Color(0xFF4CAF50) else if (!isEnabled) Color.Red.copy(alpha=0.5f) else AzulPrincipal.copy(alpha = 0.5f)
    val bgColor = if (isSaved || bitmap != null) Color(0xFFE8F5E9) else if (!isEnabled) Color(0xFFFFEBEE) else Color.White

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = bgColor), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(if(!isEnabled && !isSaved) Color.Red.copy(alpha=0.1f) else AzulPrincipal.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CameraAlt, null, tint = if(!isEnabled && !isSaved) Color.Red else AzulPrincipal, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isSaved || bitmap != null) Color(0xFF2E7D32) else AsideFondo)
                    if (bitmap == null) Text(text = if(!isEnabled && !isSaved) "Sistema bloqueado: Revisa los ajustes" else descripcion, color = GrisTexto, fontSize = 12.sp)
                }
                if (isSaved || bitmap != null) Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50))
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (bitmap != null) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                Row(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth().background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp)).padding(8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccessTime, null, tint = GrisTexto, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Capturado el: $fechaCaptura a las $horaCaptura", color = GrisTexto, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                if (!isSaved) {
                    TextButton(onClick = onClick, modifier = Modifier.align(Alignment.End), enabled = isEnabled) { Text("REPETIR FOTO", color = AzulPrincipal, fontSize = 12.sp) }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.CenterEnd) {
                        Text("✓ Evidencia Enviada y Bloqueada", color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(8.dp), enabled = isEnabled, colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulPrincipal, containerColor = Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, if(!isEnabled && !isSaved) Color.Red.copy(alpha=0.3f) else AzulPrincipal.copy(alpha=0.5f))) {
                    Icon(Icons.Filled.PhotoCamera, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("TOMAR FOTOGRAFÍA", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}