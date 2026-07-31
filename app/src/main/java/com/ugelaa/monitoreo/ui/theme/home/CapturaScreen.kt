package com.ugelaa.monitoreo.ui.theme.home

import android.Manifest
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ugelaa.monitoreo.ui.theme.AsideFondo
import com.ugelaa.monitoreo.ui.theme.AzulPrincipal
import com.ugelaa.monitoreo.ui.theme.GrisFondoApp
import com.ugelaa.monitoreo.ui.theme.GrisTexto

@Composable
fun CapturaScreen(navController: NavController) {
    val context = LocalContext.current

    var fotoEntrada by remember { mutableStateOf<Bitmap?>(null) }
    var fotoSalida by remember { mutableStateOf<Bitmap?>(null) }

    var tipoCaptura by remember { mutableStateOf("") }

    val launcherEntrada = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        fotoEntrada = bitmap
    }

    val launcherSalida = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        fotoSalida = bitmap
    }

    val permisoCamaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {

            if (tipoCaptura == "entrada") launcherEntrada.launch(null)
            else if (tipoCaptura == "salida") launcherSalida.launch(null)
        } else {

            Toast.makeText(context, "Se necesita permiso de cámara para la evidencia", Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = GrisFondoApp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = AzulPrincipal,
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Evidencia Fotográfica",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Capacitación Módulos AIRHS, MCAR, MCPP WEB",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AsideFondo
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = AzulPrincipal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Yurimaguas - Tarapoto - Lima", color = GrisTexto, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Zonas de Captura",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = AsideFondo,
                    modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
                )

                UploadZoneCard(
                    titulo = if (fotoEntrada != null) "Entrada Registrada" else "Foto de Entrada",
                    subtitulo = "Se registrará tu ubicación GPS actual",
                    icono = Icons.Filled.CameraAlt,
                    colorTema = AzulPrincipal,
                    bitmap = fotoEntrada,
                    onClick = {
                        tipoCaptura = "entrada" // Guardamos qué botón se apretó
                        permisoCamaraLauncher.launch(Manifest.permission.CAMERA) // Pedimos el permiso
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                UploadZoneCard(
                    titulo = if (fotoSalida != null) "Salida Registrada" else "Foto de Salida",
                    subtitulo = "Sube la evidencia al finalizar la visita",
                    icono = Icons.Filled.CameraAlt,
                    colorTema = Color(0xFFE63946),
                    bitmap = fotoSalida,
                    onClick = {
                        tipoCaptura = "salida" // Guardamos qué botón se apretó
                        permisoCamaraLauncher.launch(Manifest.permission.CAMERA) // Pedimos el permiso
                    }
                )
            }
        }
    }
}

@Composable
fun UploadZoneCard(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    colorTema: Color,
    bitmap: Bitmap?,
    onClick: () -> Unit
) {
    val isCompleted = bitmap != null
    val backgroundColor = if (isCompleted) Color(0xFFE8F5E9) else Color.White
    val borderColor = if (isCompleted) Color(0xFF4CAF50) else colorTema.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            if (isCompleted && bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Foto capturada",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFF4CAF50), CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(colorTema.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icono, contentDescription = null, tint = colorTema, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isCompleted) Color(0xFF2E7D32) else AsideFondo
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitulo,
                    color = GrisTexto,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}