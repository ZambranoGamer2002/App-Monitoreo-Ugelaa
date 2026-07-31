package com.ugelaa.monitoreo.ui.theme.home

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
import androidx.navigation.NavController
import com.ugelaa.monitoreo.R
import com.ugelaa.monitoreo.ui.theme.AsideFondo
import com.ugelaa.monitoreo.ui.theme.AzulPrincipal
import com.ugelaa.monitoreo.ui.theme.GrisFondoApp
import com.ugelaa.monitoreo.ui.theme.GrisTexto
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.WifiOff
import com.ugelaa.monitoreo.utils.observeConnectivityAsFlow
import com.ugelaa.monitoreo.utils.SessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
//Recibimos nicknameUser
fun HomeScreen(navController: NavController, nombreUser: String, nicknameUser: String) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var pantallaActual by remember { mutableStateOf("Inicio") }

    //Estado para controlar si se muestra el cuadro de confirmación
    var mostrarDialogoCerrarSesion by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isOnline by observeConnectivityAsFlow(context).collectAsState(initial = true)
    val sessionManager = remember { SessionManager(context) }

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

                // Aviso del modo Offline
                if (!isOnline) {
                    Surface(
                        color = Color(0xFFD32F2F).copy(alpha = 0.15f), // Rojo transparente y suave
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

                DrawerItemModern(
                    icon = Icons.Filled.Home,
                    label = "Inicio",
                    isSelected = pantallaActual == "Inicio",
                    onClick = { pantallaActual = "Inicio"; scope.launch { drawerState.close() } }
                )
                DrawerItemModern(
                    icon = Icons.Filled.Person,
                    label = "Datos Personales",
                    isSelected = pantallaActual == "Datos Personales",
                    onClick = { pantallaActual = "Datos Personales"; scope.launch { drawerState.close() } }
                )
                DrawerItemModern(
                    icon = Icons.Filled.LocationOn,
                    label = "Visitas (Monitoreo)",
                    isSelected = pantallaActual == "Visitas",
                    onClick = { pantallaActual = "Visitas"; scope.launch { drawerState.close() } }
                )

                Spacer(modifier = Modifier.weight(1f))
                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(16.dp))

                DrawerItemModern(
                    icon = Icons.Filled.ExitToApp,
                    label = "Cerrar Sesión",
                    isSelected = false,
                    onClick = {
                        //En lugar de cerrar de golpe, mostramos la alerta
                        mostrarDialogoCerrarSesion = true
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = GrisFondoApp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                CustomHeader(
                    nombreDocente = nombreUser,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )

                when (pantallaActual) {
                    "Inicio" -> PantallaInicio(nombreUser)
                    "Datos Personales" -> PantallaDatosPersonales(nombreUser, nicknameUser)
                    "Visitas" -> PantallaVisitas(navController)
                }
            }

            if (mostrarDialogoCerrarSesion) {
                AlertDialog(
                    onDismissRequest = { mostrarDialogoCerrarSesion = false },
                    title = {
                        Text(text = "Cerrar Sesión", fontWeight = FontWeight.Bold, color = AsideFondo)
                    },
                    text = {
                        Text(
                            text = "¿Estás seguro de que deseas salir?",
                            color = GrisTexto
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                mostrarDialogoCerrarSesion = false
                                scope.launch {
                                    drawerState.close()
                                    sessionManager.limpiarSesion()
                                    navController.navigate("login_screen") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        ) {
                            Text("SÍ, SALIR", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) // Texto en rojo de alerta
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                // Si dice que no, solo cerramos la alerta
                                mostrarDialogoCerrarSesion = false
                            }
                        ) {
                            Text("NO", color = AzulPrincipal, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
fun PantallaInicio(nombreUser: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.WavingHand,
            contentDescription = "Bienvenida",
            tint = AzulPrincipal,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "¡Bienvenido, $nombreUser!",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            color = AsideFondo,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Desde aquí podrás gestionar tus visitas de monitoreo, actualizar tus datos y reportar evidencias en tiempo real.",
            color = GrisTexto,
            fontSize = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
fun PantallaDatosPersonales(nombreUser: String, nicknameUser: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Datos Personales",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            color = AsideFondo
        )
        Spacer(modifier = Modifier.height(16.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CampoLectura(label = "Usuario", valor = nicknameUser, modifier = Modifier.weight(1f))
                    CampoLectura(label = "Celular", valor = "989397693", modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp))

                CampoLectura(label = "Nombres y Apellidos", valor = nombreUser, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))

                CampoLectura(label = "Correo Electrónico", valor = "a.pedropaulozp@gmail.com", modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                CampoLectura(label = "Fecha de Nacimiento", valor = "10/03/2002", modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))

                CampoLectura(label = "Dirección", valor = "Calle Milagro - Las Américas - Picaflor 2", modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                CampoLectura(label = "Ubicación", valor = "Loreto - Alto Amazonas - Yurimaguas", modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun PantallaVisitas(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Tus Visitas Programadas",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            color = AsideFondo
        )
        Text(
            text = "Selecciona una visita para iniciar el monitoreo",
            color = GrisTexto,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        VisitaCardPremium(
            nombrePlan = "Capacitación Módulos AIRHS, MCAR, MCPP WEB",
            asunto = "Misión de servicio (Seguridad y Confianza Digital)",
            lugar = "Yurimaguas - Tarapoto - Lima y viceversa",
            fecha = "Del 06 al 11-07-2026",
            estado = "ASIGNADA",
            onClick = {
                navController.navigate("captura_visita")
            }
        )
    }
}

@Composable
fun CampoLectura(label: String, valor: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = valor,
        onValueChange = {},
        readOnly = true,
        label = { Text(label, color = GrisTexto) },
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GrisTexto.copy(alpha = 0.5f),
            unfocusedBorderColor = GrisTexto.copy(alpha = 0.3f),
            focusedTextColor = AsideFondo,
            unfocusedTextColor = AsideFondo
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun CustomHeader(nombreDocente: String, onMenuClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AzulPrincipal,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(top = 48.dp, bottom = 32.dp, start = 20.dp, end = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .size(44.dp)
                ) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menú", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Hola,", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text(
                        text = nombreDocente,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Perfil",
                    tint = AzulPrincipal,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun DrawerItemModern(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) AzulPrincipal else Color.Transparent
    val contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun VisitaCardPremium(nombrePlan: String, asunto: String, lugar: String, fecha: String, estado: String, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Surface(color = AzulPrincipal.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = estado, color = AzulPrincipal, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    letterSpacing = 1.sp
                )
            }
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
        Box(
            modifier = Modifier.size(36.dp).background(AzulPrincipal.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AzulPrincipal, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
    }
}