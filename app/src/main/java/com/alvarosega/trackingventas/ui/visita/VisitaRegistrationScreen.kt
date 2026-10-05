package com.alvarosega.trackingventas.ui.visita

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.alvarosega.trackingventas.data.local.entity.PlanRuteoEntity
import com.alvarosega.trackingventas.data.local.entity.tieneDatosIncompletos

// Tokens Oficiales Apple Human Interface Guidelines
private val IosBackgroundGrouped = Color(0xFFF2F2F7)
private val IosCardSurface = Color(0xFFFFFFFF)
private val IosBorderSeparator = Color(0xFFE5E5EA)
private val IosSystemBlue = Color(0xFF007AFF)
private val IosSystemGreen = Color(0xFF34C759)
private val IosSystemGreenLight = Color(0xFFE8F9ED)
private val IosLabelPrimary = Color(0xFF000000)
private val IosLabelSecondary = Color(0xFF8E8E93)
private val IosFillQuaternary = Color(0xFFE5E5EA)

@Composable
fun VisitaRegistrationScreen(
    viewModel: VisitaViewModel,
    sellerCode: String,
    onNavigateBack: () -> Unit,
    onRegisteredSuccessfully: () -> Unit,
    onNavigateToSaneamiento: (Long?) -> Unit
) {
    val context = LocalContext.current
    val appContext = remember { context.applicationContext }

    val proximityState by viewModel.proximityState.collectAsState()
    val isOperationActive by viewModel.isOperationActive.collectAsState()
    val activeRoute by viewModel.activeRoute.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()
    val capturedPhotos by viewModel.capturedPhotos.collectAsState()

    var selectedClient by remember { mutableStateOf<PlanRuteoEntity?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var comments by remember { mutableStateOf("") }
    var photoCaptured by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showCameraPreview by remember { mutableStateOf(false) }

    LaunchedEffect(selectedClient?.clientId) {
        selectedStatus = null
        comments = ""
        isSaving = false
    }

    val statusOptions = listOf("PREVENTA", "SIN_DINERO", "TIENDA_CERRADA", "AUSENTE")

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCameraPreview = true
        } else {
            Toast.makeText(appContext, "Permiso de cámara obligatorio", Toast.LENGTH_SHORT).show()
        }
    }

    fun dispatchCameraCapture() {
        if (!viewModel.isGpsEnabled()) {
            Toast.makeText(appContext, "El GPS debe estar encendido para tomar la fotografía de evidencia", Toast.LENGTH_LONG).show()
            context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            showCameraPreview = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(errorMsg) {
        errorMsg?.let {
            Toast.makeText(appContext, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
            isSaving = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IosBackgroundGrouped)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Bar estilo iOS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(IosCardSurface)
                    .border(0.5.dp, IosBorderSeparator)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (!isSaving) {
                                viewModel.resetForm()
                                onNavigateBack()
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = IosSystemBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ruta",
                        style = TextStyle(fontSize = 16.sp, color = IosSystemBlue, fontWeight = FontWeight.Normal)
                    )
                }

                Text(
                    text = "Registrar Visita",
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IosLabelPrimary)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(IosFillQuaternary)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (activeRoute.isNotBlank()) activeRoute else "Ruta",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = IosLabelSecondary)
                    )
                }
            }

            // Contenido con Scroll
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {

                // 1. SECCIÓN: Identificación de Cliente
                Text(
                    text = "DESTINO COMERCIAL",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                when (val state = proximityState) {
                    is ProximityUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(IosCardSurface)
                                .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(16.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = IosSystemBlue, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Verificando ubicación con satélites...", fontSize = 14.sp, color = IosLabelSecondary)
                            }
                        }
                    }

                    is ProximityUiState.SingleClientMatch -> {
                        selectedClient = state.client
                        val tieneInconsistencias = state.client.tieneDatosIncompletos()
                        val isFarFromPoint = state.distance > 150.0f

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(IosCardSurface)
                                .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isFarFromPoint) Color(0xFFFF9500) else IosSystemGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = state.client.clientName,
                                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isFarFromPoint) Color(0xFFFFF3E0) else IosSystemGreenLight)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isFarFromPoint) "A ${state.distance.toInt()}m (Desfase > 150m)" else "A ${state.distance.toInt()}m",
                                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isFarFromPoint) Color(0xFFE65100) else IosSystemGreen)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Código: #${state.client.clientId} | ${state.client.address ?: "Sin dirección registrada"}",
                                style = TextStyle(fontSize = 13.sp, color = IosLabelSecondary)
                            )

                            // Advertencia si está a más de 150m del punto
                            if (isFarFromPoint) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFFFF3E0))
                                        .border(0.5.dp, Color(0xFFFFB74D), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Advertencia: Te encuentras a ${state.distance.toInt()}m del cliente (> 150m). La visita se registrará con la distancia auditada.",
                                        fontSize = 12.sp,
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    is ProximityUiState.OutOfRange -> {
                        selectedClient = null
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(IosCardSurface)
                                .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = IosSystemBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Alta Cliente",
                                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Te encuentras fuera del radio de los clientes de tu ruta. Para registrar este punto comercial debes completar el alta con georreferenciación y fachada.",
                                style = TextStyle(fontSize = 13.sp, color = IosLabelSecondary, lineHeight = 18.sp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { onNavigateToSaneamiento(null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IosSystemBlue),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Abrir Formulario de Alta Cliente",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    is ProximityUiState.MultipleClientsMatch -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(IosCardSurface)
                                .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(16.dp))
                        ) {
                            state.clientsWithDistance.forEachIndexed { index, (client, dist) ->
                                val isSelected = selectedClient?.clientId == client.clientId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedClient = client }
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = client.clientName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = IosLabelPrimary)
                                        Text(text = "#${client.clientId} • A ${dist.toInt()}m", fontSize = 12.sp, color = IosLabelSecondary)
                                    }
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = IosSystemBlue, modifier = Modifier.size(18.dp))
                                    }
                                }
                                if (index < state.clientsWithDistance.size - 1) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(start = 14.dp).height(0.5.dp).background(IosBorderSeparator))
                                }
                            }
                        }
                    }

                    else -> {}
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. SECCIÓN: Estado de Atención (Grilla 2x2 Táctil)
                Text(
                    text = "ESTADO DE ATENCIÓN",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 0..1) {
                            val status = statusOptions[i]
                            val isSelected = selectedStatus == status
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) IosSystemBlue else IosCardSurface)
                                    .border(0.8.dp, if (isSelected) IosSystemBlue else IosBorderSeparator, RoundedCornerShape(12.dp))
                                    .clickable { if (!isSaving) selectedStatus = status },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = status.replace("_", " "),
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else IosLabelPrimary
                                    )
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 2..3) {
                            val status = statusOptions[i]
                            val isSelected = selectedStatus == status
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) IosSystemBlue else IosCardSurface)
                                    .border(0.8.dp, if (isSelected) IosSystemBlue else IosBorderSeparator, RoundedCornerShape(12.dp))
                                    .clickable { if (!isSaving) selectedStatus = status },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = status.replace("_", " "),
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else IosLabelPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. SECCIÓN: Evidencia Fotográfica
                val capturedPhotos by viewModel.capturedPhotos.collectAsState()

                Text(
                    text = "EVIDENCIA FOTOGRÁFICA (${capturedPhotos.size})",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(IosCardSurface)
                        .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    // Muestra primero las fotografías tomadas (ARRIBA)
                    if (capturedPhotos.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(capturedPhotos.size) { index ->
                                val photoFile = capturedPhotos[index]
                                val bitmap = remember(photoFile, photoFile.lastModified(), photoFile.length()) {
                                    if (photoFile.exists() && photoFile.length() > 0) {
                                        BitmapFactory.decodeFile(photoFile.absolutePath)?.asImageBitmap()
                                    } else null
                                }
                                if (bitmap != null) {
                                    Box(
                                        modifier = Modifier
                                            .width(150.dp)
                                            .height(170.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(0.5.dp, IosBorderSeparator, RoundedCornerShape(12.dp))
                                    ) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = "Foto ${index + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Badge con número de foto
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "FOTO #${index + 1}",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Botón eliminar (X)
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.65f))
                                                .clickable { viewModel.removeCapturedPhoto(photoFile) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Eliminar",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Botón para agregar fotografía (UBICADO DEBAJO DE LAS FOTOS)
                    OutlinedButton(
                        onClick = { dispatchCameraCapture() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, IosBorderSeparator)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = IosSystemBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (capturedPhotos.isEmpty()) "Tomar Fotografía (Obligatoria)" else "+ Agregar Otra Fotografía",
                                color = IosSystemBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. SECCIÓN: Observaciones
                Text(
                    text = "OBSERVACIONES (OPCIONAL)",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(IosCardSurface)
                        .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    if (comments.isEmpty()) {
                        Text("Detalles adicionales de la visita...", fontSize = 14.sp, color = IosLabelSecondary)
                    }
                    BasicTextField(
                        value = comments,
                        onValueChange = { comments = it },
                        cursorBrush = SolidColor(IosSystemBlue),
                        textStyle = TextStyle(fontSize = 14.sp, color = IosLabelPrimary),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 5. Botón de Confirmación Fijo Inferior (Solo para clientes del plan emparejados)
            val isFormValid = isOperationActive &&
                    capturedPhotos.isNotEmpty() &&
                    selectedStatus != null &&
                    selectedClient != null

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IosCardSurface)
                    .border(0.5.dp, IosBorderSeparator)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        if (!viewModel.isGpsEnabled()) {
                            Toast.makeText(appContext, "El GPS debe estar encendido para guardar la visita", Toast.LENGTH_LONG).show()
                            context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            return@Button
                        }
                        if (!isFormValid || isSaving) return@Button
                        isSaving = true
                        val distance = (proximityState as? ProximityUiState.SingleClientMatch)?.distance?.toDouble()
                        viewModel.saveVisita(
                            selectedClient = selectedClient,
                            isOpportunity = false,
                            opportunityClientName = null,
                            status = selectedStatus,
                            comments = comments,
                            distanceToClientMeters = distance,
                            onSuccess = {
                                Toast.makeText(appContext, "Visita guardada correctamente", Toast.LENGTH_SHORT).show()
                                onRegisteredSuccessfully()
                            }
                        )
                    },
                    enabled = isFormValid && !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IosSystemGreen,
                        disabledContainerColor = IosSystemGreen.copy(alpha = 0.4f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Text(
                            text = if (selectedClient == null) "Seleccione un cliente o registre Alta" else "Confirmar y Guardar Visita",
                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }
                }
            }
        }
    }

    if (showCameraPreview) {
        val targetFile = remember(showCameraPreview) { viewModel.createPrivatePhotoFile() }
        com.alvarosega.trackingventas.ui.camera.VisitaCameraPreview(
            targetFile = targetFile,
            onPhotoCaptured = {
                viewModel.processCapturedPhoto(sellerCode) {
                    photoCaptured = true
                    showCameraPreview = false
                }
            },
            onClose = {
                showCameraPreview = false
            }
        )
    }
}