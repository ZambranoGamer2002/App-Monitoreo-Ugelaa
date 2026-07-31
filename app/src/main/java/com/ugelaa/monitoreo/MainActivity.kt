package com.ugelaa.monitoreo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

    //Instanciamos el manejador de la sesión
    val sessionManager = remember { SessionManager(context) }

    //Leemos el estado de la sesión de forma reactiva desde la memoria del teléfono
    val isLoggedIn by sessionManager.isLoggedIn.collectAsState(initial = null)
    val nombreGuardado by sessionManager.getNombre.collectAsState(initial = "")
    val nicknameGuardado by sessionManager.getNickname.collectAsState(initial = "")

    if (isLoggedIn == null) {
        SplashScreen(navController = navController)
        return
    }

    // Decidimos la ruta de inicio basados en la sesión
    val startRoute = if (isLoggedIn == true) {
        "home_monitoreo_directo"
    } else {
        "login_screen"
    }

    NavHost(navController = navController, startDestination = startRoute) {

        composable("splash_screen") {
            SplashScreen(navController = navController)
        }

        composable("login_screen") {
            LoginScreen(navController = navController)
        }

        //Cuando el usuario viene del Login
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

        //Cuando la app se abre y ya había sesión guardada
        composable("home_monitoreo_directo") {
            HomeScreen(navController = navController, nombreUser = nombreGuardado, nicknameUser = nicknameGuardado)
        }

        composable("captura_visita") {
            CapturaScreen(navController = navController)
        }
    }
}