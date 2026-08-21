package com.ugelaa.monitoreo.ui.theme.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ugelaa.monitoreo.R
import com.ugelaa.monitoreo.ui.theme.AsideFondo
import com.ugelaa.monitoreo.ui.theme.AzulPrincipal
import com.ugelaa.monitoreo.ui.theme.GrisTexto
import com.ugelaa.monitoreo.utils.SessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    // Variables de estado
    var usuarioInput by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.fondo_1),
            contentDescription = "Fondo de red abstracta",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Column(
                modifier = Modifier.widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_ugelaa_2),
                    contentDescription = "Logo UGELAA",
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .padding(bottom = 36.dp),
                    contentScale = ContentScale.Fit
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 32.dp, vertical = 40.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "¡Bienvenido!",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            color = AsideFondo,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ingresa tus credenciales para continuar",
                            color = GrisTexto,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        OutlinedTextField(
                            value = usuarioInput,
                            onValueChange = { usuarioInput = it },
                            label = { Text("Usuario", color = GrisTexto) },
                            leadingIcon = {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = AzulPrincipal)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AzulPrincipal,
                                focusedLabelColor = AzulPrincipal,
                                cursorColor = AzulPrincipal,
                                unfocusedBorderColor = GrisTexto.copy(alpha = 0.2f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña", color = GrisTexto) },
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = AzulPrincipal)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(16.dp),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !isLoading) {
                                    Icon(imageVector = image, contentDescription = "Ver contraseña", tint = GrisTexto.copy(alpha = 0.6f))
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AzulPrincipal,
                                focusedLabelColor = AzulPrincipal,
                                cursorColor = AzulPrincipal,
                                unfocusedBorderColor = GrisTexto.copy(alpha = 0.2f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = ""

                                    try {
                                        val request = com.ugelaa.monitoreo.model.LoginRequest(
                                            nickname = usuarioInput,
                                            password = password
                                        )

                                        val response = com.ugelaa.monitoreo.data.RetrofitClient.apiService.login(request)

                                        if (response.isSuccessful && response.body() != null) {
                                            val loginResponse = response.body()!!

                                            if (loginResponse.success) {
                                                val nombreReal = loginResponse.usuario.nombre_completo
                                                val nicknameReal = loginResponse.usuario.nickname
                                                val tokenSanctum = loginResponse.token

                                                sessionManager.guardarSesion(
                                                    token = tokenSanctum,
                                                    nombre = nombreReal,
                                                    nickname = nicknameReal
                                                )

                                                navController.navigate("home_monitoreo/$nombreReal/$nicknameReal") {
                                                    popUpTo("login_screen") { inclusive = true }
                                                }
                                            } else {
                                                errorMessage = loginResponse.message
                                            }
                                        } else {
                                            errorMessage = "Error HTTP: ${response.code()} - Credenciales incorrectas."
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error técnico: ${e.message}"
                                        e.printStackTrace()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Text("INGRESAR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
                            }
                        }
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        color = Color(0xFFCACFF5),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                color = Color(0xFFD32F2F),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}