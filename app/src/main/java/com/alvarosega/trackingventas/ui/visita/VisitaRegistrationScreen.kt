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

    var selectedClient by remember { mutableStateOf<PlanRuteoEntity?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var comments by remember { mutableStateOf("") }
    var photoCaptured by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val statusOptions = listOf("PREVENTA", "SIN_DINERO", "TIENDA_CERRADA", "AUSENTE")

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.processCapturedPhoto(sellerCode) {
                photoCaptured = true
            }
        } else {
            Toast.makeText(appContext, "Captura cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val uri = viewModel.generatePrivatePhotoUri()
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, uri)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                val pkgManager = context.packageManager
                val resolvedList = pkgManager.queryIntentActivities(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY)
                val systemCamera = resolvedList.firstOrNull {
                    (it.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                }
                if (systemCamera != null) {
                    cameraIntent.setPackage(systemCamera.activityInfo.packageName)
                }
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(appContext, "Error al preparar cámara: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(appContext, "Permiso de cámara obligatorio", Toast.LENGTH_SHORT).show()
        }
    }

    fun dispatchCameraCapture() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val uri = viewModel.generatePrivatePhotoUri()
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, uri)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                val pkgManager = context.packageManager
                val resolvedList = pkgManager.queryIntentActivities(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY)
                val systemCamera = resolvedList.firstOrNull {
                    (it.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                }
                if (systemCamera != null) {
                    cameraIntent.setPackage(systemCamera.activityInfo.packageName)
                }
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(appContext, "Error al iniciar cámara: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
                        .clickable { if (!isSaving) onNavigateBack() }
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
                                        tint = IosSystemGreen,
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
                                        .background(IosSystemGreenLight)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "A ${state.distance.toInt()}m",
                                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IosSystemGreen)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Código: #${state.client.clientId} | ${state.client.address ?: "Sin dirección registrada"}",
                                style = TextStyle(fontSize = 13.sp, color = IosLabelSecondary)
                            )

                            // Alerta de inconsistencia si faltan datos en el cliente
                            if (tieneInconsistencias) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFFFF3E0))
                                        .border(0.5.dp, Color(0xFFFFB74D), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Datos incompletos (teléfono, NIT o contacto). Se sugiere saneamiento.",
                                            fontSize = 12.sp,
                                            color = Color(0xFFE65100),
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Botón de acción para abrir Saneamiento en modo EDICIÓN
                            Button(
                                onClick = { onNavigateToSaneamiento(state.client.clientId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F7)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "Sugerir Corrección / Editar Datos",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IosSystemBlue
                                )
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
                Text(
                    text = "EVIDENCIA FOTOGRÁFICA",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (photoCaptured && viewModel.activePhotoFile != null) {
                    val bitmap = remember(viewModel.activePhotoFile) {
                        BitmapFactory.decodeFile(viewModel.activePhotoFile!!.absolutePath)?.asImageBitmap()
                    }
                    if (bitmap != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(IosCardSurface)
                                .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(16.dp))
                                .padding(10.dp)
                        ) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Foto capturada",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(
                                onClick = { dispatchCameraCapture() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Volver a Tomar Fotografía", color = IosSystemBlue, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(IosCardSurface)
                            .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(16.dp))
                            .clickable { dispatchCameraCapture() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(IosFillQuaternary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = IosSystemBlue, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tomar Fotografía (Obligatoria)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = IosSystemBlue)
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
                    photoCaptured &&
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
                        if (!isFormValid || isSaving) return@Button
                        isSaving = true
                        viewModel.saveVisita(
                            selectedClient = selectedClient,
                            isOpportunity = false,
                            opportunityClientName = null,
                            status = selectedStatus,
                            comments = comments,
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
}