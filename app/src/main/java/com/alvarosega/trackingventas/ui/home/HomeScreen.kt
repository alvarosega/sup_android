package com.alvarosega.trackingventas.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvarosega.trackingventas.ui.tracking.MainMapViewModel
import kotlinx.coroutines.delay

// Tokens Apple Human Interface Guidelines
private val IosBackgroundGrouped = Color(0xFFF2F2F7)
private val IosCardSurface = Color(0xFFFFFFFF)
private val IosBorderSeparator = Color(0xFFE5E5EA)
private val IosSystemBlue = Color(0xFF007AFF)
private val IosSystemGreen = Color(0xFF34C759)
private val IosSystemGreenLight = Color(0xFFE8F9ED)
private val IosSystemRed = Color(0xFFFF3B30)
private val IosSystemRedLight = Color(0xFFFFEBEA)
private val IosLabelPrimary = Color(0xFF000000)
private val IosLabelSecondary = Color(0xFF8E8E93)
private val IosFillQuaternary = Color(0xFFF2F2F7)

@Composable
fun HomeScreen(
    onNavigateToMap: () -> Unit,
    onLogout: () -> Unit,
    viewModel: MainMapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isOperationActive by viewModel.isOperationActive.collectAsState()
    val activeRoute by viewModel.activeRoute.collectAsState()
    val workdayLimitTime by viewModel.workdayLimitTime.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }

    BackHandler {
        (context as? Activity)?.moveTaskToBack(true)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.checkWorkdayStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        androidx.work.WorkManager.getInstance(context).cancelUniqueWork("TrackingPeriodicSync")
        androidx.work.WorkManager.getInstance(context).cancelUniqueWork("TrackingForegroundSync")
        viewModel.checkWorkdayStatus()
    }

    LaunchedEffect(isOperationActive) {
        while (isOperationActive) {
            kotlinx.coroutines.delay(30000)
            viewModel.checkWorkdayStatus()
        }
    }

    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    fun checkAndStartOperation() {
        if (!viewModel.isGpsEnabled()) {
            Toast.makeText(context, "El GPS es obligatorio para iniciar operación", Toast.LENGTH_SHORT).show()
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }
        viewModel.setOperationState(true)
    }

    val bgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        checkAndStartOperation()
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBg = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasBg) {
                checkAndStartOperation()
            } else {
                bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        } else {
            checkAndStartOperation()
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fine) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                checkAndStartOperation()
            }
        } else {
            Toast.makeText(context, "Los permisos de ubicación son obligatorios", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestStartPermissions() {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine) {
            permLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            checkAndStartOperation()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IosBackgroundGrouped)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Apple Top Navigation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PANEL PRINCIPAL",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosLabelSecondary,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Text(
                        text = "TDB",
                        style = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosLabelPrimary,
                            letterSpacing = (-0.5).sp
                        )
                    )
                }

                // Botón Salir / Logout Estilo iOS Circle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(IosCardSurface)
                        .border(0.8.dp, IosBorderSeparator, CircleShape)
                        .clickable { showLogoutDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Cerrar Sesión",
                        tint = IosSystemRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Dynamic Status Hero Card (Control de Jornada)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color(0x0D000000),
                        spotColor = Color(0x0D000000)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(IosCardSurface)
                    .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isOperationActive) IosSystemGreen else IosLabelSecondary,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOperationActive) "JORNADA EN CURSO" else "JORNADA DETENIDA",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOperationActive) IosSystemGreen else IosLabelSecondary,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (activeRoute.isNotBlank()) "Ruta: $activeRoute" else "Ruta: General Asignada",
                            style = TextStyle(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IosLabelPrimary
                            )
                        )
                    }

                    // Badge de Estado
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isOperationActive) IosSystemGreenLight else IosFillQuaternary)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isOperationActive) "GPS Activo" else "Offline",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOperationActive) IosSystemGreen else IosLabelSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(IosBorderSeparator)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (isOperationActive) {
                            showStopDialog = true
                        } else {
                            requestStartPermissions()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOperationActive) IosSystemRedLight else IosSystemBlue
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = if (isOperationActive) "Finalizar Jornada" else "Iniciar Jornada",
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOperationActive) IosSystemRed else Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3. Módulo de Operación (Hero Plan de Ruta)
            Text(
                text = "OPERACIÓN COMERCIAL",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = IosLabelSecondary,
                    letterSpacing = 0.6.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Celda Navegable Estilo Apple Inset Grouped
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color(0x08000000),
                        spotColor = Color(0x08000000)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(IosCardSurface)
                    .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(16.dp))
                    .clickable {
                        if (!isOperationActive) {
                            Toast.makeText(context, "Debe iniciar jornada primero", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }
                        if (!viewModel.isGpsEnabled()) {
                            Toast.makeText(context, "El GPS está desactivado. Actívalo para acceder al Plan de Rutas.", Toast.LENGTH_LONG).show()
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            return@clickable
                        }
                        onNavigateToMap()
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isOperationActive) Color(0xFFE8F1FF) else IosFillQuaternary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Plan de Ruta",
                            tint = if (isOperationActive) IosSystemBlue else IosLabelSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Plan de Ruta y Visitas",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOperationActive) IosLabelPrimary else IosLabelSecondary
                            )
                        )
                        Text(
                            text = if (isOperationActive) "Visualizar mapa y clientes asignados" else "Requiere jornada activa",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = IosLabelSecondary
                            )
                        )
                    }
                }

                Icon(
                    imageVector = if (isOperationActive) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Default.Lock,
                    contentDescription = null,
                    tint = IosLabelSecondary,
                    modifier = Modifier.size(if (isOperationActive) 20.dp else 18.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4. Inset Grouped: Diagnóstico y Parámetros del Sistema
            Text(
                text = "ESTADO DEL SERVICIO",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = IosLabelSecondary,
                    letterSpacing = 0.6.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(IosCardSurface)
                    .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(16.dp))
            ) {
                // Fila 1: Precisión GPS


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp)
                        .height(0.5.dp)
                        .background(IosBorderSeparator)
                )

                // Fila 2: Ventana de Trabajo Legal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = IosLabelSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Horario Laboral",
                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, color = IosLabelPrimary)
                        )
                    }
                    Text(
                        text = if (!workdayLimitTime.isNullOrBlank()) "Límite: $workdayLimitTime" else "Según Ruta Asignada",
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = IosLabelSecondary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Modal de Cierre de Sesión Estilo iOS
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Cerrar Sesión",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary)
                )
            },
            text = {
                Text(
                    text = "¿Deseas salir del sistema? Si la jornada está en curso, se detendrá el rastreo de ubicación.",
                    style = TextStyle(fontSize = 14.sp, color = IosLabelSecondary)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onLogout()
                    }
                ) {
                    Text("Cerrar Sesión", color = IosSystemRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = IosSystemBlue)
                }
            },
            containerColor = IosCardSurface,
            shape = RoundedCornerShape(14.dp)
        )
    }

    // Modal de Detención de Jornada Estilo iOS
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = {
                Text(
                    text = "Finalizar Jornada",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary)
                )
            },
            text = {
                Text(
                    text = "¿Deseas finalizar la jornada laboral? La telemetría en segundo plano se apagará.",
                    style = TextStyle(fontSize = 14.sp, color = IosLabelSecondary)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopDialog = false
                        viewModel.setOperationState(false)
                        Toast.makeText(context, "Jornada finalizada", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Finalizar", color = IosSystemRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Continuar Ruta", color = IosSystemBlue)
                }
            },
            containerColor = IosCardSurface,
            shape = RoundedCornerShape(14.dp)
        )
    }
}