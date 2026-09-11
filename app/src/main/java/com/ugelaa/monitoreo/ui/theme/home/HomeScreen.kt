package com.ugelaa.monitoreo.ui.theme.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
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
import androidx.compose.ui.text.font.FontFamily
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
import com.ugelaa.monitoreo.model.PerfilResponse
import com.ugelaa.monitoreo.model.Visita
import com.ugelaa.monitoreo.ui.theme.AsideFondo
import com.ugelaa.monitoreo.ui.theme.AzulPrincipal
import com.ugelaa.monitoreo.ui.theme.GrisFondoApp
import com.ugelaa.monitoreo.ui.theme.GrisTexto
import com.ugelaa.monitoreo.utils.SessionManager
import com.ugelaa.monitoreo.utils.observeConnectivityAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val dniGuardado by sessionManager.getDni.collectAsState(initial = "")

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
                            painter = painterResource(id = R.drawable.logo_ugelaa_2),
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
                                Text("MODO SIN CONEXIÓN", color = Color(0xFFEF5350), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp)
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
                                "Datos Personales" -> PantallaDatosPersonales(
                                    nombreUser = nombreUser,
                                    dniUser = dniGuardado.ifEmpty { "No registrado" },
                                    token = tokenGuardado
                                )
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
                    text = { Text(text = "¿Estás seguro de que deseas salir? Tu progreso local está a salvo y no se borrará.", color = GrisTexto, fontSize = 15.sp) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                mostrarDialogoCerrarSesion = false
                                scope.launch {
                                    context.getSharedPreferences("CacheVisitas", Context.MODE_PRIVATE).edit().clear().apply()
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PantallaVisitas(navController: NavController, token: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sharedPrefCache = context.getSharedPreferences("CacheVisitas", Context.MODE_PRIVATE)
    val sharedPrefEstados = context.getSharedPreferences("EstadoVisitas", Context.MODE_PRIVATE)
    val visitaDao = remember { AppDatabase.getDatabase(context).visitaDao() }
    val gson = remember { Gson() }
    val coroutineScope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }

    var listaVisitas by remember { mutableStateOf<List<Visita>>(emptyList()) }
    var cantidadPendientesOffline by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    var mostrarDialogoAlertaFecha by remember { mutableStateOf(false) }

    var isGpsEnabled by remember { mutableStateOf(checkGpsStatus(context)) }
    var isAutoTimeEnabled by remember { mutableStateOf(checkAutoTimeEnabled(context)) }
    var isAirplaneModeOn by remember { mutableStateOf(checkAirplaneMode(context)) }
    val isSystemReady = isGpsEnabled && isAutoTimeEnabled && !isAirplaneModeOn

    val cargarVisitas = {
        coroutineScope.launch {
            if (token.isNotEmpty()) {
                isLoading = true
                try {
                    val response = RetrofitClient.apiService.getVisitas("Bearer $token")
                    if (response.isSuccessful && response.body() != null) {
                        var rawString = response.body()!!.string().trim()

                        if (rawString.startsWith("\"") && rawString.endsWith("\"")) {
                            rawString = try {
                                gson.fromJson(rawString, String::class.java).trim()
                            } catch (e: Exception) {
                                rawString.substring(1, rawString.length - 1).replace("\\\"", "\"")
                            }
                        }

                        if (rawString.startsWith("[")) {
                            val type = object : TypeToken<List<Visita>>() {}.type
                            val listaApi: List<Visita> = gson.fromJson(rawString, type)

                            listaApi.forEach { plan ->
                                val estadoLocal = sharedPrefEstados.getString("visita_${plan.id}", "")
                                val estaFinalizadoLocal = estadoLocal == "FINALIZADA" || estadoLocal == "COMPLETADO"

                                if (estaFinalizadoLocal) {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            RetrofitClient.apiService.planFinalizado("Bearer $token", plan.id)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                            val evidenciasPendientes = visitaDao.obtenerEvidenciasPendientes()
                            val idsPlanesConOffline = evidenciasPendientes.map { it.planId }.toSet()

                            val jsonAntiguo = sharedPrefCache.getString("planes_offline", "[]")
                            val typeAntiguo = object : TypeToken<List<Visita>>() {}.type
                            val listaAntigua: List<Visita> = try { gson.fromJson(jsonAntiguo, typeAntiguo) ?: emptyList() } catch (e: Exception) { emptyList() }

                            val planesRescatados = listaAntigua.filter { planAntiguo ->
                                listaApi.none { it.id == planAntiguo.id } && idsPlanesConOffline.contains(planAntiguo.id.toString())
                            }.map { planHuerfano ->
                                val nombreActual = planHuerfano.nombre_visitas ?: "Visita"
                                if (!nombreActual.contains("(Plan Eliminado)")) {
                                    planHuerfano.copy(nombre_visitas = "$nombreActual (Plan Eliminado)")
                                } else {
                                    planHuerfano
                                }
                            }

                            listaVisitas = listaApi + planesRescatados
                            sharedPrefCache.edit().putString("planes_offline", gson.toJson(listaVisitas)).apply()
                        }
                    } else if (response.code() == 401) {
                        sessionManager.limpiarSesion()
                        navController.navigate("login_screen") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    val pullRefreshState = rememberPullRefreshState(refreshing = isLoading, onRefresh = { cargarVisitas() })

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

    LaunchedEffect(token) {
        if (token.isNotEmpty() && listaVisitas.isEmpty()) {
            cargarVisitas()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGpsEnabled = checkGpsStatus(context)
                isAutoTimeEnabled = checkAutoTimeEnabled(context)
                isAirplaneModeOn = checkAirplaneMode(context)
                cargarVisitas()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val visitasActivas = listaVisitas.filter { esVisitaActiva(it.fecha_fin) }

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Visitas Activas", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = AsideFondo, letterSpacing = (-0.5).sp)
            Text(text = "Desliza hacia abajo para refrescar tus planes programados.", color = GrisTexto, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp, bottom = 24.dp))

            if (cantidadPendientesOffline > 0) {
                Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth()) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tienes $cantidadPendientesOffline registro(s) sin conexión.", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFFE65100))
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
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                                Icon(Icons.Filled.TimerOff, contentDescription = null, tint = Color(0xFFB71C1C), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("La 'Hora Automática' está desactivada.", color = Color(0xFFB71C1C), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            if (isLoading && visitasActivas.isEmpty()) {
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
                    val estadoLocal = sharedPrefEstados.getString("visita_${visita.id}", "")
                    val estaFinalizada = estadoLocal == "FINALIZADA" || estadoLocal == "COMPLETADO" || visita.estado?.uppercase(Locale.ROOT) == "FINALIZADA"
                    val isFutura = esVisitaFutura(visita.fecha_inicio)

                    val textoEstadoPlan: String
                    val colorEstadoPlan: Color

                    if (estaFinalizada) {
                        textoEstadoPlan = "FINALIZADA"
                        colorEstadoPlan = Color(0xFF2E7D32)
                    } else if (isFutura) {
                        textoEstadoPlan = "EN PROCESO"
                        colorEstadoPlan = Color(0xFFF57C00)
                    } else {
                        textoEstadoPlan = "PENDIENTE"
                        colorEstadoPlan = AzulPrincipal
                    }

                    VisitaCardPremium(
                        nombrePlan = visita.nombre_visitas ?: "Sin Nombre",
                        fecha = "Del ${visita.fecha_inicio ?: "-"} al ${visita.fecha_fin ?: "-"}",
                        estado = textoEstadoPlan,
                        colorBadge = colorEstadoPlan,
                        isExpired = false,
                        onClick = {
                            if (isFutura && !estaFinalizada) {
                                mostrarDialogoAlertaFecha = true
                            } else if (isSystemReady) {
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

        PullRefreshIndicator(
            refreshing = isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = Color.White,
            contentColor = AzulPrincipal
        )

        if (mostrarDialogoAlertaFecha) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoAlertaFecha = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.EventBusy, contentDescription = null, tint = Color(0xFFF57C00))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Acceso Restringido", fontWeight = FontWeight.Bold, color = AsideFondo, fontSize = 20.sp)
                    }
                },
                text = { Text(text = "El plan aún no está dentro de la fecha, aún no puede realizar sus actividades.", color = GrisTexto, fontSize = 15.sp) },
                confirmButton = {
                    TextButton(
                        onClick = { mostrarDialogoAlertaFecha = false }
                    ) { Text("ENTENDIDO", color = AzulPrincipal, fontWeight = FontWeight.ExtraBold) }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 8.dp
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PantallaHistorialVisitas(token: String) {
    val context = LocalContext.current
    val sharedPrefCache = context.getSharedPreferences("CacheVisitas", Context.MODE_PRIVATE)
    val gson = remember { Gson() }
    val coroutineScope = rememberCoroutineScope()

    var listaVisitas by remember { mutableStateOf<List<Visita>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val cargarHistorial = {
        coroutineScope.launch {
            if (token.isNotEmpty()) {
                isLoading = true
                try {
                    val response = RetrofitClient.apiService.getVisitas("Bearer $token")
                    if (response.isSuccessful && response.body() != null) {
                        var rawString = response.body()!!.string().trim()

                        if (rawString.startsWith("\"") && rawString.endsWith("\"")) {
                            rawString = try {
                                gson.fromJson(rawString, String::class.java).trim()
                            } catch (e: Exception) {
                                rawString.substring(1, rawString.length - 1).replace("\\\"", "\"")
                            }
                        }

                        if (rawString.startsWith("[")) {
                            val type = object : TypeToken<List<Visita>>() {}.type
                            val listaApi: List<Visita> = gson.fromJson(rawString, type)
                            listaVisitas = listaApi
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    val pullRefreshState = rememberPullRefreshState(refreshing = isLoading, onRefresh = { cargarHistorial() })

    LaunchedEffect(Unit) {
        val jsonGuardado = sharedPrefCache.getString("planes_offline", null)
        if (!jsonGuardado.isNullOrEmpty() && jsonGuardado != "null") {
            try {
                val type = object : TypeToken<List<Visita>>() {}.type
                val datosCacheados: List<Visita>? = gson.fromJson(jsonGuardado, type)
                if (datosCacheados != null) {
                    listaVisitas = datosCacheados
                }
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(token) {
        if (token.isNotEmpty() && listaVisitas.isEmpty()) {
            cargarHistorial()
        }
    }

    val visitasHistorial = listaVisitas.filter { visita ->
        val fechaFinPasada = !esVisitaActiva(visita.fecha_fin) && !visita.fecha_fin.isNullOrBlank()
        fechaFinPasada && !esVisitaExpirada(visita.fecha_fin)
    }

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Historial de Visitas", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = AsideFondo, letterSpacing = (-0.5).sp)
            Text(text = "Desliza hacia abajo para cargar visitas finalizadas.", color = GrisTexto, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp, bottom = 28.dp))

            if (isLoading && visitasHistorial.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AzulPrincipal, strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
                }
            } else if (visitasHistorial.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.History, contentDescription = null, tint = GrisTexto.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Aún no tienes visitas en el historial.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = GrisTexto, fontSize = 15.sp)
                    }
                }
            } else {
                visitasHistorial.forEach { visita ->
                    VisitaCardPremium(
                        nombrePlan = visita.nombre_visitas ?: "Sin Nombre",
                        fecha = "Del ${visita.fecha_inicio ?: "-"} al ${visita.fecha_fin ?: "-"}",
                        estado = "FINALIZADA",
                        colorBadge = Color(0xFF2E7D32),
                        isExpired = true,
                        onClick = {}
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        PullRefreshIndicator(
            refreshing = isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = Color.White,
            contentColor = AzulPrincipal
        )
    }
}

@Composable
fun VisitaCardPremium(
    nombrePlan: String,
    fecha: String,
    estado: String,
    colorBadge: Color,
    isExpired: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = if (isExpired && colorBadge != Color(0xFF2E7D32)) Color(0xFFFAFAFA) else Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isExpired && colorBadge != Color(0xFF2E7D32)) 2.dp else 8.dp)
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
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = nombrePlan,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = AsideFondo,
                lineHeight = 26.sp,
                letterSpacing = (-0.2).sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = GrisFondoApp, thickness = 2.dp)
            Spacer(modifier = Modifier.height(20.dp))

            DetailRowPremium(icon = Icons.Rounded.DateRange, text = fecha, isExpired = isExpired && colorBadge != Color(0xFF2E7D32))
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
                horizontalArrangement = Arrangement.Start
            ) {
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
        Text(text = "Estás listo para gestionar tus visitas y reportar evidencias de forma rápida, tanto con conexión como sin conexión.", color = GrisTexto, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 26.sp)
    }
}

@Composable
fun PantallaDatosPersonales(nombreUser: String, dniUser: String, token: String) {
    val gson = remember { Gson() }

    var cargo by remember { mutableStateOf("Cargando...") }
    var oficina by remember { mutableStateOf("Cargando...") }

    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(token) {
        if (token.isNotEmpty()) {
            isLoading = true
            try {
                val response = RetrofitClient.apiService.getPerfil("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val rawString = response.body()!!.string().trim()
                    try {
                        val perfil = gson.fromJson(rawString, PerfilResponse::class.java)
                        if (perfil.success && perfil.usuario != null) {
                            cargo = perfil.usuario.cargo ?: "No asignado"
                            oficina = perfil.usuario.oficina ?: "No asignada"
                        } else {
                            cargo = "No disponible"
                            oficina = "No disponible"
                        }
                    } catch (e: Exception) {
                        cargo = "Error de formato"
                        oficina = "Error de formato"
                    }
                } else {
                    cargo = "Error al cargar"
                    oficina = "Error al cargar"
                }
            } catch (e: Exception) {
                cargo = "Sin conexión"
                oficina = "Sin conexión"
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Tu Perfil", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = AsideFondo, letterSpacing = (-0.5).sp)
        Text(
            text = "Información del docente registrada en el sistema.",
            color = GrisTexto,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                if (isLoading && cargo == "Cargando...") {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AzulPrincipal, strokeWidth = 3.dp)
                    }
                } else {
                    CampoLectura(label = "DNI", valor = dniUser, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(20.dp))
                    CampoLectura(label = "Nombres y Apellidos", valor = nombreUser, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(20.dp))
                    CampoLectura(label = "Oficina", valor = oficina, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(20.dp))
                    CampoLectura(label = "Cargo", valor = cargo, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
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

fun esVisitaActiva(fechaFin: String?): Boolean {
    if (fechaFin.isNullOrBlank()) return true
    return try {
        val formatStr = if (fechaFin.contains("/")) "dd/MM/yyyy" else "yyyy-MM-dd"
        val sdfFin = SimpleDateFormat(formatStr, Locale.getDefault())
        val dateFin = sdfFin.parse(fechaFin) ?: return true

        val sdfHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateHoy = sdfHoy.parse(sdfHoy.format(Date())) ?: return true

        !dateFin.before(dateHoy)
    } catch (e: Exception) {
        true
    }
}

fun esVisitaFutura(fechaInicio: String?): Boolean {
    if (fechaInicio.isNullOrBlank()) return false
    return try {
        val formatStr = if (fechaInicio.contains("/")) "dd/MM/yyyy" else "yyyy-MM-dd"
        val sdfInicio = SimpleDateFormat(formatStr, Locale.getDefault())
        val dateInicio = sdfInicio.parse(fechaInicio) ?: return false

        val sdfHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateHoy = sdfHoy.parse(sdfHoy.format(Date())) ?: return false

        dateInicio.after(dateHoy)
    } catch (e: Exception) {
        false
    }
}

fun esVisitaExpirada(fechaFin: String?): Boolean {
    if (fechaFin.isNullOrBlank()) return false
    return try {
        val formatStr = if (fechaFin.contains("/")) "dd/MM/yyyy" else "yyyy-MM-dd"
        val sdfFin = SimpleDateFormat(formatStr, Locale.getDefault())
        val dateFin = sdfFin.parse(fechaFin) ?: return false

        val sdfHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateHoy = sdfHoy.parse(sdfHoy.format(Date())) ?: return false

        dateFin.before(dateHoy)
    } catch (e: Exception) {
        false
    }
}