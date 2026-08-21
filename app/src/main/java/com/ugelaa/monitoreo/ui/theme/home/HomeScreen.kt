package com.ugelaa.monitoreo.ui.theme.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ugelaa.monitoreo.R
import com.ugelaa.monitoreo.data.RetrofitClient
import com.ugelaa.monitoreo.data.local.AppDatabase
import com.ugelaa.monitoreo.data.local.SyncWorker
import com.ugelaa.monitoreo.model.LugarVisita
import com.ugelaa.monitoreo.model.Visita
import com.ugelaa.monitoreo.ui.theme.AsideFondo
import com.ugelaa.monitoreo.ui.theme.AzulPrincipal
import com.ugelaa.monitoreo.ui.theme.GrisFondoApp
import com.ugelaa.monitoreo.ui.theme.GrisTexto
import com.ugelaa.monitoreo.utils.SessionManager
import com.ugelaa.monitoreo.utils.observeConnectivityAsFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, nombreUser: String, nicknameUser: String) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var pantallaActual by remember { mutableStateOf("Visitas") }
    var mostrarDialogoCerrarSesion by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isOnline by observeConnectivityAsFlow(context).collectAsState(initial = true)
    val sessionManager = remember { SessionManager(context) }

    val tokenGuardado by sessionManager.getToken.collectAsState(initial = "")

    LaunchedEffect(isOnline) {
        if (isOnline) {
            iniciarSincronizacion(context)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = AsideFondo,
                drawerContentColor = Color.White,
                modifier = Modifier.width(320.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Spacer(modifier = Modifier.height(40.dp))

                    Column(modifier = Modifier.padding(horizontal = 28.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.fondo_1),
                            contentDescription = "Logo UGELAA",
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(bottom = 8.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!isOnline) {
                        Surface(
                            color = Color(0xFFD32F2F).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Icon(Icons.Filled.WifiOff, contentDescription = "Sin Internet", tint = Color(0xFFEF5350), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("MODO OFFLINE", color = Color(0xFFEF5350), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp)
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))

                    DrawerItemModern(icon = Icons.Filled.Home, label = "Inicio", isSelected = pantallaActual == "Inicio") {
                        scope.launch { drawerState.close(); pantallaActual = "Inicio" }
                    }
                    DrawerItemModern(icon = Icons.Filled.LocationOn, label = "Visitas Activas", isSelected = pantallaActual == "Visitas") {
                        scope.launch { drawerState.close(); pantallaActual = "Visitas" }
                    }
                    DrawerItemModern(icon = Icons.Filled.History, label = "Historial de Visitas", isSelected = pantallaActual == "Historial") {
                        scope.launch { drawerState.close(); pantallaActual = "Historial" }
                    }
                    DrawerItemModern(icon = Icons.Filled.Person, label = "Datos Personales", isSelected = pantallaActual == "Datos Personales") {
                        scope.launch { drawerState.close(); pantallaActual = "Datos Personales" }
                    }
                    DrawerItemModern(icon = Icons.Filled.Settings, label = "Configuración", isSelected = pantallaActual == "Configuración") {
                        scope.launch { drawerState.close(); pantallaActual = "Configuración" }
                    }

                    Spacer(modifier = Modifier.weight(1f, fill = false))
                    Spacer(modifier = Modifier.height(32.dp))

                    Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(modifier = Modifier.height(16.dp))

                    DrawerItemModern(icon = Icons.Filled.ExitToApp, label = "Cerrar Sesión", isSelected = false) { mostrarDialogoCerrarSesion = true }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = GrisFondoApp) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

                CustomHeader(
                    nombreDocente = nombreUser,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )

                Crossfade(
                    targetState = pantallaActual,
                    animationSpec = tween(durationMillis = 400),
                    modifier = Modifier.weight(1f)
                ) { pantalla ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        Box(modifier = Modifier.widthIn(max = 650.dp).fillMaxSize()) {
                            when (pantalla) {
                                "Inicio" -> PantallaInicio(nombreUser)
                                "Visitas" -> PantallaVisitas(navController, tokenGuardado)
                                "Historial" -> PantallaHistorialVisitas(tokenGuardado)
                                "Datos Personales" -> PantallaDatosPersonales(nombreUser, nicknameUser)
                                "Configuración" -> PantallaConfiguracion()
                            }
                        }
                    }
                }
            }

            if (mostrarDialogoCerrarSesion) {
                AlertDialog(
                    onDismissRequest = { mostrarDialogoCerrarSesion = false },
                    title = { Text(text = "Cerrar Sesión", fontWeight = FontWeight.Bold, color = AsideFondo, fontSize = 20.sp) },
                    text = { Text(text = "¿Estás seguro de que deseas salir? Todos los datos cacheados se borrarán por seguridad.", color = GrisTexto, fontSize = 15.sp) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                mostrarDialogoCerrarSesion = false
                                scope.launch {
                                    context.getSharedPreferences("CacheVisitas", Context.MODE_PRIVATE).edit().clear().apply()
                                    context.getSharedPreferences("EstadoVisitas", Context.MODE_PRIVATE).edit().clear().apply()
                                    sessionManager.limpiarSesion()

                                    navController.navigate("login_screen") {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                }
                            }
                        ) { Text("SÍ, SALIR", color = Color(0xFFD32F2F), fontWeight = FontWeight.ExtraBold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarDialogoCerrarSesion = false }) { Text("CANCELAR", color = AzulPrincipal, fontWeight = FontWeight.Bold) }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 8.dp
                )
            }
        }
    }
}

@Composable
fun PantallaVisitas(navController: NavController, token: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sharedPref = context.getSharedPreferences("EstadoVisitas", Context.MODE_PRIVATE)
    val sharedPrefCache = context.getSharedPreferences("CacheVisitas", Context.MODE_PRIVATE)
    val visitaDao = remember { AppDatabase.getDatabase(context).visitaDao() }
    val gson = remember { Gson() }

    var listaVisitas by remember { mutableStateOf<List<Visita>>(emptyList()) }
    var cantidadPendientesOffline by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    var isGpsEnabled by remember { mutableStateOf(checkGpsStatus(context)) }
    var isAutoTimeEnabled by remember { mutableStateOf(checkAutoTimeEnabled(context)) }
    var isAirplaneModeOn by remember { mutableStateOf(checkAirplaneMode(context)) }
    val isSystemReady = isGpsEnabled && isAutoTimeEnabled && !isAirplaneModeOn

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val fechaHoy = sdf.format(Date())

    LaunchedEffect(Unit) {
        val jsonGuardado = sharedPrefCache.getString("planes_offline", null)
        if (!jsonGuardado.isNullOrEmpty() && jsonGuardado != "null") {
            try {
                val type = object : TypeToken<List<Visita>>() {}.type
                val datosCacheados: List<Visita>? = gson.fromJson(jsonGuardado, type)
                if (datosCacheados != null) {
                    listaVisitas = datosCacheados
                }
            } catch (e: Exception) {
                sharedPrefCache.edit().remove("planes_offline").apply()
            }
        }
        val pendientes = visitaDao.obtenerEvidenciasPendientes()
        cantidadPendientesOffline = pendientes.size
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGpsEnabled = checkGpsStatus(context)
                isAutoTimeEnabled = checkAutoTimeEnabled(context)
                isAirplaneModeOn = checkAirplaneMode(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(token) {
        if (token.isNotEmpty()) {
            try {
                isLoading = true
                val response = RetrofitClient.apiService.getVisitas("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    listaVisitas = response.body()!!
                    sharedPrefCache.edit().putString("planes_offline", gson.toJson(listaVisitas)).apply()
                } else {
                    errorMessage = "Error de servidor. Código: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Sin conexión a internet. Mostrando vista local."
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    val visitasActivas = listaVisitas.filter { (it.fecha_fin ?: "") >= fechaHoy }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Visitas Activas", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = AsideFondo, letterSpacing = (-0.5).sp)
        Text(text = "Planes programados para realizarse hoy o en los próximos días.", color = GrisTexto, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp, bottom = 24.dp))

        if (cantidadPendientesOffline > 0) {
            Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth()) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tienes $cantidadPendientesOffline registro(s) offline.", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFFE65100))
                        Text("Se subirán automáticamente cuando haya internet.", fontSize = 12.sp, color = Color(0xFFEF6C00))
                    }
                }
            }
        }

        if (!isSystemReady) {
            Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(bottom = 20.dp).fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("ATENCIÓN DE SEGURIDAD", color = Color(0xFFC62828), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isAirplaneModeOn) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                            Icon(Icons.Filled.AirplanemodeActive, contentDescription = null, tint = Color(0xFFB71C1C), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Modo Avión activado. Debes apagarlo.", color = Color(0xFFB71C1C), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (!isGpsEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                            Icon(Icons.Filled.LocationOff, contentDescription = null, tint = Color(0xFFB71C1C), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("El GPS está desactivado. Debes encenderlo.", color = Color(0xFFB71C1C), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (!isAutoTimeEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.TimerOff, contentDescription = null, tint = Color(0xFFB71C1C), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("La 'Hora Automática' está desactivada.", color = Color(0xFFB71C1C), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AzulPrincipal, strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
            }
        } else if (visitasActivas.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.EventAvailable, contentDescription = null, tint = GrisTexto.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "No hay visitas activas en este momento.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = GrisTexto, fontSize = 15.sp)
                }
            }
        } else {
            visitasActivas.forEach { visita ->
                val estadoMemoria = sharedPref.getString("visita_${visita.id}", "PENDIENTE")
                val (textoEstado, colorEstado) = when (estadoMemoria) {
                    "COMPLETADO" -> Pair("FINALIZADA", Color(0xFF4CAF50))
                    "SALIDA" -> Pair("EN CURSO", Color(0xFFF57C00))
                    "MEDIO" -> Pair("EN CURSO", Color(0xFFF57C00))
                    "ENTRADA" -> Pair("EN CURSO", Color(0xFFF57C00))
                    else -> Pair("PENDIENTE", AzulPrincipal)
                }

                //Extracción de coordenadas directas o de la caché
                var latitudDestino = visita.latitud
                var longitudDestino = visita.longitud

                if (latitudDestino.isNullOrEmpty() || longitudDestino.isNullOrEmpty()) {
                    val jsonDetalle = sharedPrefCache.getString("detalle_${visita.id}", null)
                    if (!jsonDetalle.isNullOrEmpty()) {
                        try {
                            val type = object : TypeToken<List<LugarVisita>>() {}.type
                            val lugares: List<LugarVisita> = gson.fromJson(jsonDetalle, type)
                            val primerLugarConGps = lugares.firstOrNull { !it.latitud.isNullOrEmpty() && !it.longitud.isNullOrEmpty() }
                            if (primerLugarConGps != null) {
                                latitudDestino = primerLugarConGps.latitud
                                longitudDestino = primerLugarConGps.longitud
                            }
                        } catch (e: Exception) {}
                    }
                }

                VisitaCardPremium(
                    nombrePlan = visita.nombre_visitas ?: "Sin Nombre",
                    fecha = "Del ${visita.fecha_inicio ?: "-"} al ${visita.fecha_fin ?: "-"}",
                    estado = textoEstado,
                    colorBadge = colorEstado,
                    isExpired = false,
                    onVerMapa = {
                        abrirGoogleMaps(context, latitudDestino, longitudDestino, visita.nombre_visitas ?: "Visita")
                    },
                    onClick = {
                        if (isSystemReady) {
                            val idCodificado = visita.id.toString()
                            val nombreCodificado = URLEncoder.encode(visita.nombre_visitas ?: "Visita", StandardCharsets.UTF_8.toString())
                            navController.navigate("captura_visita/$idCodificado/$nombreCodificado")
                        } else {
                            if (isAirplaneModeOn) context.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
                            else if (!isGpsEnabled) context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            else context.startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
                        }
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PantallaHistorialVisitas(token: String) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("EstadoVisitas", Context.MODE_PRIVATE)
    val sharedPrefCache = context.getSharedPreferences("CacheVisitas", Context.MODE_PRIVATE)
    val gson = remember { Gson() }

    var listaVisitas by remember { mutableStateOf<List<Visita>>(emptyList()) }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val fechaHoy = sdf.format(Date())

    LaunchedEffect(Unit) {
        val jsonGuardado = sharedPrefCache.getString("planes_offline", null)
        if (!jsonGuardado.isNullOrEmpty() && jsonGuardado != "null") {
            try {
                val type = object : TypeToken<List<Visita>>() {}.type
                val datosCacheados: List<Visita>? = gson.fromJson(jsonGuardado, type)
                if (datosCacheados != null) {
                    listaVisitas = datosCacheados
                }
            } catch (e: Exception) {
                sharedPrefCache.edit().remove("planes_offline").apply()
            }
        }
    }

    val visitasPasadas = listaVisitas.filter {
        (it.fecha_fin ?: "") < fechaHoy && (it.fecha_fin ?: "").isNotEmpty()
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Historial de Visitas", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = AsideFondo, letterSpacing = (-0.5).sp)
        Text(text = "Aquí se archivan las visitas cuya fecha límite ya terminó.", color = GrisTexto, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp, bottom = 28.dp))

        if (visitasPasadas.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = GrisTexto.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Aún no tienes visitas en el historial.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = GrisTexto, fontSize = 15.sp)
                }
            }
        } else {
            visitasPasadas.forEach { visita ->
                val estadoMemoria = sharedPref.getString("visita_${visita.id}", "PENDIENTE")
                val (textoEstado, colorEstado, isExpired) = when (estadoMemoria) {
                    "COMPLETADO" -> Triple("CULMINADA A TIEMPO", Color(0xFF2E7D32), false)
                    else -> Triple("NO CULMINADA / VENCIDA", Color(0xFFD32F2F), true)
                }

                VisitaCardPremium(
                    nombrePlan = visita.nombre_visitas ?: "Sin Nombre",
                    fecha = "Del ${visita.fecha_inicio ?: "-"} al ${visita.fecha_fin ?: "-"}",
                    estado = textoEstado,
                    colorBadge = colorEstado,
                    isExpired = isExpired,
                    onVerMapa = {
                        abrirGoogleMaps(context, visita.latitud, visita.longitud, visita.nombre_visitas ?: "Visita")
                    },
                    onClick = { /* Bloqueado en historial */ }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun VisitaCardPremium(
    nombrePlan: String,
    fecha: String,
    estado: String,
    colorBadge: Color,
    isExpired: Boolean,
    onVerMapa: () -> Unit,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = if (isExpired) Color(0xFFFAFAFA) else Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isExpired) 2.dp else 8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = colorBadge.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
                    Text(
                        text = estado,
                        color = colorBadge,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        letterSpacing = 0.8.sp
                    )
                }

                FilledTonalIconButton(
                    onClick = onVerMapa,
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = AzulPrincipal.copy(alpha = 0.1f),
                        contentColor = AzulPrincipal
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Map,
                        contentDescription = "Abrir en Google Maps",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = nombrePlan,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = if (isExpired) GrisTexto else AsideFondo,
                lineHeight = 26.sp,
                letterSpacing = (-0.2).sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = GrisFondoApp, thickness = 2.dp)
            Spacer(modifier = Modifier.height(20.dp))
            DetailRowPremium(icon = Icons.Rounded.DateRange, text = fecha, isExpired = isExpired)
        }
    }
}

@Composable
fun DetailRowPremium(icon: ImageVector, text: String, isExpired: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).background(if (isExpired) GrisTexto.copy(alpha=0.08f) else AzulPrincipal.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, null, tint = if (isExpired) GrisTexto else AzulPrincipal, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = text, fontSize = 15.sp, color = if (isExpired) GrisTexto else Color.DarkGray, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
    }
}

fun abrirGoogleMaps(context: Context, latitud: String?, longitud: String?, titulo: String) {
    if (latitud.isNullOrBlank() || longitud.isNullOrBlank() || latitud == "0.0" || latitud == "null") {
        Toast.makeText(context, "Este plan aún no tiene coordenadas GPS registradas.", Toast.LENGTH_LONG).show()
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

fun iniciarSincronizacion(context: Context) {
    val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(constraints).build()
    WorkManager.getInstance(context).enqueue(syncRequest)
}

@Composable
fun CustomHeader(nombreDocente: String, onMenuClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp),
        shadowElevation = 8.dp,
        color = AzulPrincipal
    ) {
        Box(modifier = Modifier.padding(top = 48.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.25f), CircleShape).size(48.dp)
                    ) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menú", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Hola,", color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(text = nombreDocente, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Box(
                    modifier = Modifier.size(54.dp).background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.Person, contentDescription = "Perfil", tint = AzulPrincipal, modifier = Modifier.size(34.dp))
                }
            }
        }
    }
}

@Composable
fun PantallaConfiguracion() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(checkPermission(context, Manifest.permission.CAMERA)) }
    var hasLocationPermission by remember { mutableStateOf(checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) }
    var isGpsEnabled by remember { mutableStateOf(checkGpsStatus(context)) }
    var isAutoTimeEnabled by remember { mutableStateOf(checkAutoTimeEnabled(context)) }
    var isAirplaneModeOn by remember { mutableStateOf(checkAirplaneMode(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = checkPermission(context, Manifest.permission.CAMERA)
                hasLocationPermission = checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                isGpsEnabled = checkGpsStatus(context)
                isAutoTimeEnabled = checkAutoTimeEnabled(context)
                isAirplaneModeOn = checkAirplaneMode(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Configuración", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = AsideFondo, letterSpacing = (-0.5).sp)
        Text(text = "Verifica que tu dispositivo esté 100% listo para monitorear.", color = GrisTexto, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp, bottom = 28.dp))

        ItemConfiguracion("Permiso de Cámara", "Necesario para tomar las fotos.", hasCameraPermission, Icons.Filled.CameraAlt) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) })
        }
        Spacer(modifier = Modifier.height(16.dp))
        ItemConfiguracion("Permiso de Ubicación", "Necesario para obtener las coordenadas.", hasLocationPermission, Icons.Filled.LocationOn) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) })
        }
        Spacer(modifier = Modifier.height(16.dp))
        ItemConfiguracion("Sensor GPS", "El GPS físico debe estar activado.", isGpsEnabled, Icons.Filled.GpsFixed) {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        Spacer(modifier = Modifier.height(16.dp))
        ItemConfiguracion("Hora Automática", "Garantiza una hora de reporte inalterable.", isAutoTimeEnabled, Icons.Filled.Schedule) {
            context.startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
        }
        Spacer(modifier = Modifier.height(16.dp))
        ItemConfiguracion("Modo Avión", "Debe estar apagado para enviar los datos.", !isAirplaneModeOn, Icons.Filled.AirplanemodeActive) {
            context.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

fun checkPermission(context: Context, permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
fun checkGpsStatus(context: Context) = (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.GPS_PROVIDER)
fun checkAutoTimeEnabled(context: Context) = try { Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME) == 1 } catch (e: Exception) { false }
fun checkAirplaneMode(context: Context): Boolean = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0

@Composable
fun ItemConfiguracion(titulo: String, descripcion: String, isOk: Boolean, icon: ImageVector, onClickArreglar: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column {
            Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(52.dp).background(if (isOk) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = if (isOk) Color(0xFF4CAF50) else Color(0xFFD32F2F), modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.width(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = titulo, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = AsideFondo, letterSpacing = (-0.2).sp)
                    Text(text = descripcion, color = GrisTexto, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Icon(
                            imageVector = if (isOk) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            tint = if (isOk) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOk) "Activo y permitido" else "Requiere atención",
                            color = if (isOk) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            if (!isOk) {
                Divider(color = GrisFondoApp, thickness = 1.dp)
                TextButton(
                    onClick = onClickArreglar,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("SOLUCIONAR AHORA", color = AzulPrincipal, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

@Composable
fun PantallaInicio(nombreUser: String) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(imageVector = Icons.Filled.WavingHand, contentDescription = "Bienvenida", tint = AzulPrincipal, modifier = Modifier.size(90.dp))
        Spacer(modifier = Modifier.height(28.dp))
        Text(text = "¡Hola, $nombreUser!", fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = AsideFondo, textAlign = androidx.compose.ui.text.style.TextAlign.Center, letterSpacing = (-0.5).sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Estás listo para gestionar tus visitas y reportar evidencias de forma rápida, tanto online como offline.", color = GrisTexto, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 26.sp)
    }
}

@Composable
fun PantallaDatosPersonales(nombreUser: String, nicknameUser: String) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Tu Perfil", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = AsideFondo, letterSpacing = (-0.5).sp)
        Spacer(modifier = Modifier.height(24.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(32.dp)) {
                CampoLectura(label = "DNI / Usuario", valor = nicknameUser, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))
                CampoLectura(label = "Nombres y Apellidos", valor = nombreUser, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun CampoLectura(label: String, valor: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = valor,
        onValueChange = {},
        readOnly = true,
        label = { Text(label, color = GrisTexto, fontWeight = FontWeight.Medium) },
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GrisTexto.copy(alpha = 0.2f),
            unfocusedBorderColor = GrisTexto.copy(alpha = 0.2f),
            focusedTextColor = AsideFondo,
            unfocusedTextColor = AsideFondo,
            focusedContainerColor = Color(0xFFFAFAFA),
            unfocusedContainerColor = Color(0xFFFAFAFA)
        ),
        shape = RoundedCornerShape(16.dp),
        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    )
}

@Composable
fun DrawerItemModern(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) AzulPrincipal else Color.Transparent
    val contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(50)).background(bgColor).clickable { onClick() }.padding(horizontal = 22.dp, vertical = 16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(18.dp))
        Text(text = label, color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
    }
}