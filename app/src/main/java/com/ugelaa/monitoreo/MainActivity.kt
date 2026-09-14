package com.ugelaa.monitoreo

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ugelaa.monitoreo.data.RetrofitClient
import com.ugelaa.monitoreo.ui.theme.AsideFondo
import com.ugelaa.monitoreo.ui.theme.AzulPrincipal
import com.ugelaa.monitoreo.ui.theme.GrisTexto
import com.ugelaa.monitoreo.ui.theme.MonitoreoUGELAATheme
import com.ugelaa.monitoreo.ui.theme.home.HomeScreen
import com.ugelaa.monitoreo.ui.theme.splash.SplashScreen
import com.ugelaa.monitoreo.ui.theme.login.LoginScreen
import com.ugelaa.monitoreo.ui.theme.home.CapturaScreen
import com.ugelaa.monitoreo.utils.SessionManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MonitoreoUGELAATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    val isLoggedIn by sessionManager.isLoggedIn.collectAsState(initial = null)
    val nombreGuardado by sessionManager.getNombre.collectAsState(initial = "")
    val nicknameGuardado by sessionManager.getNickname.collectAsState(initial = "")

    var startRoute by remember { mutableStateOf<String?>(null) }

    // Pasamos navController y sessionManager al Guardián
    GuardianActualizaciones(navController = navController, sessionManager = sessionManager)

    LaunchedEffect(isLoggedIn) {
        if (startRoute == null && isLoggedIn != null) {
            startRoute = if (isLoggedIn == true) "home_monitoreo_directo" else "login_screen"
        }
    }

    if (startRoute == null) {
        SplashScreen(navController = navController)
        return
    }

    NavHost(navController = navController, startDestination = startRoute!!) {

        composable("splash_screen") { SplashScreen(navController = navController) }
        composable("login_screen") { LoginScreen(navController = navController) }

        composable(
            route = "home_monitoreo/{nombre}/{nickname}",
            arguments = listOf(
                navArgument("nombre") { type = NavType.StringType },
                navArgument("nickname") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val nombre = backStackEntry.arguments?.getString("nombre") ?: "Docente"
            val nickname = backStackEntry.arguments?.getString("nickname") ?: ""
            HomeScreen(navController = navController, nombreUser = nombre, nicknameUser = nickname)
        }

        composable("home_monitoreo_directo") {
            HomeScreen(navController = navController, nombreUser = nombreGuardado, nicknameUser = nicknameGuardado)
        }

        composable(
            route = "captura_visita/{idVisita}/{nombrePlan}",
            arguments = listOf(
                navArgument("idVisita") { type = NavType.StringType },
                navArgument("nombrePlan") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val idVisita = backStackEntry.arguments?.getString("idVisita") ?: "0"
            val nombrePlan = backStackEntry.arguments?.getString("nombrePlan") ?: "Visita"
            CapturaScreen(navController = navController, idVisita = idVisita, nombrePlan = nombrePlan)
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENTE: GUARDIÁN DE ACTUALIZACIONES
// -------------------------------------------------------------------------
@Composable
fun GuardianActualizaciones(navController: NavController, sessionManager: SessionManager) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var mostrarDialogoBloqueo by remember { mutableStateOf(false) }

    // Obtener la versión instalada de la app como texto (Ej: "1.0.1")
    val versionInstaladaStr = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    } catch (e: PackageManager.NameNotFoundException) {
        "1.0"
    }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.verificarActualizacion()
            if (response.isSuccessful && response.body() != null) {
                val listaActualizaciones = response.body()!!

                // Buscar la última versión en la base de datos que tenga estado Activo
                val actualizacionActiva = listaActualizaciones.lastOrNull {
                    it.estado.equals("Activo", ignoreCase = true) || it.estado == "1"
                }

                if (actualizacionActiva != null) {
                    val versionServidor = actualizacionActiva.version_actual.trim()

                    // REGLA ESTRICTA: Si la versión no coincide, se cierra la sesión y se bloquea
                    if (versionServidor != versionInstaladaStr.trim()) {

                        // 1. Limpiar sesión (Elimina token, DNI, datos)
                        coroutineScope.launch {
                            sessionManager.limpiarSesion()
                        }

                        // 2. Expulsar al usuario hacia el Login
                        try {
                            navController.navigate("login_screen") {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            // Ignorar error si el navController aún no está listo
                        }

                        // 3. Levantar la pantalla de bloqueo
                        mostrarDialogoBloqueo = true
                    }
                }
            }
        } catch (e: Exception) {
            // Modo "Sin Conexión": Si no hay red, no bloqueamos la app
        }
    }

    if (mostrarDialogoBloqueo) {
        AlertDialog(
            onDismissRequest = { /* Vacío para impedir que se cierre al tocar los bordes */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = AzulPrincipal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Actualización Requerida", fontWeight = FontWeight.Bold, color = AsideFondo)
                }
            },
            text = {
                Text(
                    "Tu versión actual ($versionInstaladaStr) está obsoleta. Por motivos de seguridad, tu sesión ha sido cerrada.\n\nPor favor, ingresa al portal web para descargar la nueva actualización y poder continuar.",
                    color = GrisTexto
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Mandar al usuario al enlace indicado
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://simoplan.ugelaa.gob.pe/login"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("IR AL PORTAL DE DESCARGA", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false, // Impide cerrar con el botón "Atrás" del celular
                dismissOnClickOutside = false
            )
        )
    }
}