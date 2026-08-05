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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.ugelaa.monitoreo.ui.theme.AsideFondo
import com.ugelaa.monitoreo.ui.theme.AzulPrincipal
import com.ugelaa.monitoreo.ui.theme.GrisFondoApp
import com.ugelaa.monitoreo.ui.theme.GrisTexto
import com.ugelaa.monitoreo.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapturaScreen(navController: NavController, idVisita: String, nombrePlan: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sessionManager = remember { SessionManager(context) }

    val nicknameUsuario by sessionManager.getNickname.collectAsState(initial = "Cargando...")
    val nombreUsuario by sessionManager.getNombre.collectAsState(initial = "Cargando...")

    // --- ESCÁNER DE TRAMPAS: Vigila el GPS en tiempo real en TODAS las fases ---
    var isGpsEnabled by remember { mutableStateOf(checkGpsStatusLocal(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Cada vez que vuelve de la cámara o de minimizar, revisa el GPS
                isGpsEnabled = checkGpsStatusLocal(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var faseActual by remember { mutableStateOf(1) }

    var bitmapEntrada by remember { mutableStateOf<Bitmap?>(null) }
    var latitudEntrada by remember { mutableStateOf("") }
    var fechaEntrada by remember { mutableStateOf("") }
    var horaEntrada by remember { mutableStateOf("") }

    var bitmapSalida by remember { mutableStateOf<Bitmap?>(null) }
    var latitudSalida by remember { mutableStateOf("") }
    var fechaSalida by remember { mutableStateOf("") }
    var horaSalida by remember { mutableStateOf("") }

    val cameraLauncherEntrada = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) bitmapEntrada = bitmap
    }
    val cameraLauncherSalida = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) bitmapSalida = bitmap
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

                // --- ALERTA ANTI-TRAMPAS EN TIEMPO REAL ---
                if (!isGpsEnabled) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("¡ALERTA DE SEGURIDAD!", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Has apagado el GPS. Por políticas de la UGELAA, los botones de guardado y cámara han sido bloqueados. Enciende tu GPS para continuar.", color = Color.DarkGray, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }) {
                                Text("ENCENDER GPS", color = AzulPrincipal, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Monitoreando Visita ID: $idVisita", color = AzulPrincipal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(nombrePlan, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = AsideFondo)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ------------- FASE 1: ENTRADA -------------
                if (faseActual == 1) {
                    Text("Paso 1: Foto de Llegada", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = AsideFondo)
                    Spacer(modifier = Modifier.height(16.dp))
                    UploadZone(
                        titulo = "Registro de Entrada",
                        descripcion = "Toma una foto en la puerta o inicio del evento.",
                        bitmap = bitmapEntrada,
                        isEnabled = isGpsEnabled // ¡BLOQUEADO SI APAGA EL GPS!
                    ) {
                        if(isGpsEnabled) cameraLauncherEntrada.launch()
                    }
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            val fechaActual = Date()
                            latitudEntrada = "-5.90123"
                            fechaEntrada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(fechaActual)
                            horaEntrada = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(fechaActual)
                            faseActual = 2
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = bitmapEntrada != null && isGpsEnabled, // ¡BLOQUEADO SI APAGA EL GPS!
                        colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal)
                    ) { Text("GUARDAR ENTRADA Y CONTINUAR", color = Color.White, fontWeight = FontWeight.Bold) }
                }

                // ------------- FASE 2: SALIDA -------------
                if (faseActual == 2) {
                    Text("Paso 2: Foto de Retiro", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = AsideFondo)
                    Spacer(modifier = Modifier.height(16.dp))
                    UploadZone(
                        titulo = "Registro de Salida",
                        descripcion = "Toma una foto al finalizar tu jornada de monitoreo.",
                        bitmap = bitmapSalida,
                        isEnabled = isGpsEnabled // ¡BLOQUEADO SI APAGA EL GPS!
                    ) {
                        if(isGpsEnabled) cameraLauncherSalida.launch()
                    }
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            val fechaActual = Date()
                            latitudSalida = "-5.90150"
                            fechaSalida = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(fechaActual)
                            horaSalida = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(fechaActual)
                            faseActual = 3
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = bitmapSalida != null && isGpsEnabled, // ¡BLOQUEADO SI APAGA EL GPS!
                        colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal)
                    ) { Text("GUARDAR SALIDA Y VER RESUMEN", color = Color.White, fontWeight = FontWeight.Bold) }
                }

                // ------------- FASE 3: MODO DESARROLLADOR Y GALERÍA -------------
                if (faseActual == 3) {
                    Text("Resumen de Evidencias", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = AsideFondo)
                    Spacer(modifier = Modifier.height(16.dp))

                    // ¡NUEVO! GALERÍA DE FOTOS CAPTURADAS
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        bitmapEntrada?.let {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Entrada", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrisTexto)
                                Spacer(modifier = Modifier.height(4.dp))
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Foto Entrada",
                                    modifier = Modifier.height(120.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        bitmapSalida?.let {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Salida", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrisTexto)
                                Spacer(modifier = Modifier.height(4.dp))
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Foto Salida",
                                    modifier = Modifier.height(120.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // PANEL JSON
                    Surface(color = Color.Black.copy(alpha = 0.85f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Code, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SIMULADOR JSON PARA LARAVEL", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = """
[
  {
    "plan_id": $idVisita,
    "usuario_id": "$nicknameUsuario",
    "estado": "Entrada",
    "fecha": "$fechaEntrada",
    "hora": "$horaEntrada",
    "latitud": "$latitudEntrada",
    "longitud": "-76.11000",
    "precision_gps": "15.0",
    "foto": "[Archivo_Binario_1.jpg]"
  },
  {
    "plan_id": $idVisita,
    "usuario_id": "$nicknameUsuario",
    "estado": "Salida",
    "fecha": "$fechaSalida",
    "hora": "$horaSalida",
    "latitud": "$latitudSalida",
    "longitud": "-76.11025",
    "precision_gps": "12.5",
    "foto": "[Archivo_Binario_2.jpg]"
  }
]
                                """.trimIndent(),
                                color = Color(0xFF00FF00),
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = isGpsEnabled, // ¡BLOQUEADO SI APAGA EL GPS!
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) { Text("FINALIZAR Y ENVIAR AL SERVIDOR", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// Función auxiliar para leer el GPS
fun checkGpsStatusLocal(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}

@Composable
fun UploadZone(titulo: String, descripcion: String, bitmap: Bitmap?, isEnabled: Boolean, onClick: () -> Unit) {
    val borderColor = if (bitmap != null) Color(0xFF4CAF50) else if (!isEnabled) Color.Red.copy(alpha=0.5f) else AzulPrincipal.copy(alpha = 0.5f)
    val bgColor = if (bitmap != null) Color(0xFFE8F5E9) else if (!isEnabled) Color(0xFFFFEBEE) else Color.White

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bgColor).border(2.dp, borderColor, RoundedCornerShape(16.dp)).clickable(enabled = isEnabled) { onClick() }.padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(60.dp).background(if (bitmap != null) Color.Transparent else AzulPrincipal.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                if (bitmap != null) Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(60.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                else Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = if(!isEnabled) Color.Red else AzulPrincipal, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (bitmap != null) Color(0xFF2E7D32) else AsideFondo)
                Text(text = if (!isEnabled) "GPS Apagado: Acción Bloqueada" else if (bitmap != null) "Foto guardada en memoria" else descripcion, color = GrisTexto, fontSize = 13.sp)
            }
            if (bitmap != null) Icon(Icons.Filled.CheckCircle, contentDescription = "Ok", tint = Color(0xFF4CAF50))
        }
    }
}