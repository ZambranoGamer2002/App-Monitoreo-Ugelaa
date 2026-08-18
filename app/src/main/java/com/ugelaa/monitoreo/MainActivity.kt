package com.ugelaa.monitoreo

import android.content.Intent
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

    GuardianActualizaciones()

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
fun GuardianActualizaciones() {
    val context = LocalContext.current
    var mostrarDialogoBloqueo by remember { mutableStateOf(false) }

    // URL DE DESCARGA: Reemplaza con la ruta de tu servidor o web
    var urlDescarga by remember { mutableStateOf("https://www.ugelaa.gob.pe/") }

    // Obtener la versión instalada (VersionCode del build.gradle.kts)
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionInstalada = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo.longVersionCode.toInt()
    } else {
        packageInfo.versionCode
    }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.verificarActualizacion()
            if (response.isSuccessful && response.body() != null) {
                val listaActualizaciones = response.body()!!

                // Buscar la última versión que esté con estado "Activo"
                val actualizacionActiva = listaActualizaciones.lastOrNull {
                    it.estado.equals("Activo", ignoreCase = true)
                }

                if (actualizacionActiva != null) {
                    val versionServidor = actualizacionActiva.version_actual.toIntOrNull() ?: 0

                    // Si el servidor exige una versión mayor a la que tenemos, BLOQUEAMOS
                    if (versionServidor > versionInstalada) {
                        mostrarDialogoBloqueo = true
                    }
                }
            }
        } catch (e: Exception) {
            // Si no hay red, no bloqueamos para permitir el Modo Offline de la app
        }
    }

    if (mostrarDialogoBloqueo) {
        AlertDialog(
            onDismissRequest = { /* Vacío: No se cierra al tocar fuera */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = AzulPrincipal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Actualización Requerida", fontWeight = FontWeight.Bold, color = AsideFondo)
                }
            },
            text = {
                Text(
                    "Tu aplicación está desactualizada. Para continuar y poder iniciar sesión o registrar visitas, es obligatorio descargar la nueva versión.",
                    color = GrisTexto
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlDescarga))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("DESCARGAR E INSTALAR", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}