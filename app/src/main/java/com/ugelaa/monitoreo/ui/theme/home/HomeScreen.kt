package com.ugelaa.monitoreo.ui.theme.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
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
import com.ugelaa.monitoreo.R
import com.ugelaa.monitoreo.ui.theme.AsideFondo
import com.ugelaa.monitoreo.ui.theme.AzulPrincipal
import com.ugelaa.monitoreo.ui.theme.GrisFondoApp
import com.ugelaa.monitoreo.ui.theme.GrisTexto
import kotlinx.coroutines.launch
import com.ugelaa.monitoreo.data.RetrofitClient
import com.ugelaa.monitoreo.model.Visita
import com.ugelaa.monitoreo.utils.observeConnectivityAsFlow
import com.ugelaa.monitoreo.utils.SessionManager
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = AsideFondo,
                drawerContentColor = Color.White,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_ugelaa_2),
                        contentDescription = "Logo UGELAA",
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(bottom = 8.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isOnline) {
                    Surface(
                        color = Color(0xFFD32F2F).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(Icons.Filled.WifiOff, contentDescription = "Sin Internet", tint = Color(0xFFEF5350), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("MODO OFFLINE", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                // Menú con Regla de Cortesía
                DrawerItemModern(icon = Icons.Filled.Home, label = "Inicio", isSelected = pantallaActual == "Inicio") {
                    scope.launch { drawerState.close(); pantallaActual = "Inicio" }
                }
                DrawerItemModern(icon = Icons.Filled.LocationOn, label = "Visitas (Monitoreo)", isSelected = pantallaActual == "Visitas") {
                    scope.launch { drawerState.close(); pantallaActual = "Visitas" }
                }
                DrawerItemModern(icon = Icons.Filled.Person, label = "Datos Personales", isSelected = pantallaActual == "Datos Personales") {
                    scope.launch { drawerState.close(); pantallaActual = "Datos Personales" }
                }
                DrawerItemModern(icon = Icons.Filled.Settings, label = "Configuración", isSelected = pantallaActual == "Configuración") {
                    scope.launch { drawerState.close(); pantallaActual = "Configuración" }
                }

                Spacer(modifier = Modifier.weight(1f))
                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(16.dp))

                DrawerItemModern(icon = Icons.Filled.ExitToApp, label = "Cerrar Sesión", isSelected = false) { mostrarDialogoCerrarSesion = true }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = GrisFondoApp) {
            Column(modifier = Modifier.fillMaxSize()) {

                CustomHeader(nombreDocente = nombreUser, onMenuClick = { scope.launch { drawerState.open() } })

                when (pantallaActual) {
                    "Inicio" -> PantallaInicio(nombreUser)
                    "Visitas" -> PantallaVisitas(navController, tokenGuardado)
                    "Datos Personales" -> PantallaDatosPersonales(nombreUser, nicknameUser)
                    "Configuración" -> PantallaConfiguracion()
                }
            }

            if (mostrarDialogoCerrarSesion) {
                AlertDialog(
                    onDismissRequest = { mostrarDialogoCerrarSesion = false },
                    title = { Text(text = "Cerrar Sesión", fontWeight = FontWeight.Bold, color = AsideFondo) },
                    text = { Text(text = "¿Estás seguro de que deseas salir? Si no tienes internet, no podrás volver a ingresar.", color = GrisTexto) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                mostrarDialogoCerrarSesion = false
                                scope.launch {
                                    sessionManager.limpiarSesion()
                                    navController.navigate("login_screen") {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                }
                            }
                        ) { Text("SÍ, SALIR", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarDialogoCerrarSesion = false }) { Text("NO", color = AzulPrincipal, fontWeight = FontWeight.Bold) }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// PANTALLA DE VISITAS (Restaurada y Segura)
// -------------------------------------------------------------------------
@Composable
fun PantallaVisitas(navController: NavController, token: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var listaVisitas by remember { mutableStateOf<List<Visita>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    var isGpsEnabled by remember { mutableStateOf(checkGpsStatus(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGpsEnabled = checkGpsStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // EL MOTOR DE BÚSQUEDA RESTAURADO
    LaunchedEffect(token) {
        if (token.isNotEmpty()) {
            try {
                isLoading = true
                val response = RetrofitClient.apiService.getVisitas("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    listaVisitas = response.body()!!
                } else {
                    errorMessage = "Error de servidor. Código: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: Verifica tu internet o el backend."
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Tus Visitas Programadas", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AsideFondo)

        // Alerta GPS
        if (!isGpsEnabled) {
            Surface(
                color = Color(0xFFFFEBEE),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp).fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOff, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GPS APAGADO: Debes encenderlo para realizar monitoreos.",
                        color = Color(0xFFC62828),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Lógica de Vistas (Cargando, Error o Lista)
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AzulPrincipal)
            }
        } else if (errorMessage.isNotEmpty()) {
            Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(text = errorMessage, color = Color(0xFFD32F2F), modifier = Modifier.padding(16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else if (listaVisitas.isEmpty()) {
            Text(text = "No tienes visitas asignadas por el momento.", modifier = Modifier.fillMaxWidth().padding(top = 32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = GrisTexto)
        } else {
            listaVisitas.forEach { visita ->
                VisitaCardPremium(
                    nombrePlan = visita.nombre_visitas,
                    asunto = visita.asunto,
                    lugar = visita.lugar_visita,
                    fecha = "Del ${visita.fecha_inicio} al ${visita.fecha_fin}",
                    estado = "ASIGNADA",
                    onClick = {
                        if (isGpsEnabled) {
                            val idCodificado = visita.id.toString()
                            val nombreCodificado = URLEncoder.encode(visita.nombre_visitas, StandardCharsets.UTF_8.toString())
                            navController.navigate("captura_visita/$idCodificado/$nombreCodificado")
                        } else {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// -------------------------------------------------------------------------
// PANTALLA DE CONFIGURACIÓN
// -------------------------------------------------------------------------
@Composable
fun PantallaConfiguracion() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(checkPermission(context, Manifest.permission.CAMERA)) }
    var hasLocationPermission by remember { mutableStateOf(checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) }
    var isGpsEnabled by remember { mutableStateOf(checkGpsStatus(context)) }
    var isAutoTimeEnabled by remember { mutableStateOf(checkAutoTimeEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = checkPermission(context, Manifest.permission.CAMERA)
                hasLocationPermission = checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                isGpsEnabled = checkGpsStatus(context)
                isAutoTimeEnabled = checkAutoTimeEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Configuración del Dispositivo", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AsideFondo)
        Text(text = "Verifica que el sistema esté listo para el monitoreo.", color = GrisTexto, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))

        ItemConfiguracion("Permiso de Cámara", "Necesario para registrar las evidencias.", hasCameraPermission, Icons.Filled.CameraAlt) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) })
        }
        Spacer(modifier = Modifier.height(16.dp))
        ItemConfiguracion("Permiso de Ubicación", "Necesario para obtener coordenadas exactas.", hasLocationPermission, Icons.Filled.LocationOn) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) })
        }
        Spacer(modifier = Modifier.height(16.dp))
        ItemConfiguracion("Sensor GPS", "Verifica si la ubicación física está encendida.", isGpsEnabled, Icons.Filled.GpsFixed) {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        Spacer(modifier = Modifier.height(16.dp))
        ItemConfiguracion("Hora Automática (Red)", "Garantiza que la hora de la evidencia sea 100% real.", isAutoTimeEnabled, Icons.Filled.Schedule) {
            context.startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

fun checkPermission(context: Context, permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
fun checkGpsStatus(context: Context) = (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.GPS_PROVIDER)
fun checkAutoTimeEnabled(context: Context) = try { Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME) == 1 } catch (e: Exception) { false }

@Composable
fun ItemConfiguracion(titulo: String, descripcion: String, isOk: Boolean, icon: ImageVector, onClickArreglar: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(if (isOk) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), CircleShape), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = if (isOk) Color(0xFF4CAF50) else Color(0xFFD32F2F), modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AsideFondo)
                Text(text = descripcion, color = GrisTexto, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 4.dp))
                Text(text = if (isOk) "✓ Activo y permitido" else "✕ Requiere atención", color = if (isOk) Color(0xFF2E7D32) else Color(0xFFC62828), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        if (!isOk) {
            Divider(color = GrisFondoApp)
            TextButton(onClick = onClickArreglar, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) { Text("SOLUCIONAR (ABRIR AJUSTES)", color = AzulPrincipal, fontWeight = FontWeight.Bold) }
        }
    }
}

// -------------------------------------------------------------------------
// PANTALLAS SIMPLES Y COMPONENTES
// -------------------------------------------------------------------------
@Composable
fun PantallaInicio(nombreUser: String) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(imageVector = Icons.Filled.WavingHand, contentDescription = "Bienvenida", tint = AzulPrincipal, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "¡Bienvenido, $nombreUser!", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = AsideFondo, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Desde aquí podrás gestionar tus visitas de monitoreo, actualizar tus datos y reportar evidencias en tiempo real.", color = GrisTexto, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 24.sp)
    }
}

@Composable
fun PantallaDatosPersonales(nombreUser: String, nicknameUser: String) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Datos Personales", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AsideFondo)
        Spacer(modifier = Modifier.height(16.dp))
        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                CampoLectura(label = "DNI / Usuario", valor = nicknameUser, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                CampoLectura(label = "Nombres y Apellidos", valor = nombreUser, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun CampoLectura(label: String, valor: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value = valor, onValueChange = {}, readOnly = true, label = { Text(label, color = GrisTexto) }, modifier = modifier, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GrisTexto.copy(alpha = 0.5f), unfocusedBorderColor = GrisTexto.copy(alpha = 0.3f), focusedTextColor = AsideFondo, unfocusedTextColor = AsideFondo), shape = RoundedCornerShape(12.dp))
}

@Composable
fun CustomHeader(nombreDocente: String, onMenuClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(color = AzulPrincipal, shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)).padding(top = 48.dp, bottom = 32.dp, start = 20.dp, end = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenuClick, modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape).size(44.dp)) { Icon(Icons.Filled.Menu, contentDescription = "Menú", tint = Color.White) }
                Spacer(modifier = Modifier.width(16.dp))
                Column { Text(text = "Hola,", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp); Text(text = nombreDocente, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }
            Box(modifier = Modifier.size(50.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Filled.Person, contentDescription = "Perfil", tint = AzulPrincipal, modifier = Modifier.size(32.dp)) }
        }
    }
}

@Composable
fun DrawerItemModern(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) AzulPrincipal else Color.Transparent
    val contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(50)).background(bgColor).clickable { onClick() }.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Icon(imageVector = icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun VisitaCardPremium(nombrePlan: String, asunto: String, lugar: String, fecha: String, estado: String, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Surface(color = AzulPrincipal.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) { Text(text = estado, color = AzulPrincipal, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), letterSpacing = 1.sp) }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = nombrePlan, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = AsideFondo, lineHeight = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = GrisFondoApp, thickness = 1.5.dp)
            Spacer(modifier = Modifier.height(16.dp))
            DetailRowPremium(icon = Icons.Rounded.Info, text = asunto)
            Spacer(modifier = Modifier.height(12.dp))
            DetailRowPremium(icon = Icons.Rounded.LocationOn, text = lugar)
            Spacer(modifier = Modifier.height(12.dp))
            DetailRowPremium(icon = Icons.Rounded.DateRange, text = fecha)
        }
    }
}

@Composable
fun DetailRowPremium(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).background(AzulPrincipal.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(imageVector = icon, null, tint = AzulPrincipal, modifier = Modifier.size(18.dp)) }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
    }
}