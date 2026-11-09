package com.alvarosega.trackingventas.ui.visita

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.alvarosega.trackingventas.data.local.entity.PlanRuteoEntity
import com.alvarosega.trackingventas.data.remote.dto.ProductoInventarioDto
import com.alvarosega.trackingventas.ui.camera.VisitaCameraPreview
import com.alvarosega.trackingventas.ui.theme.IosColors
import com.alvarosega.trackingventas.ui.theme.glassCard
import java.util.Locale

private data class EventoTimelineData(
    val fecha: String,
    val tipo: String,
    val cantidad: Int
)

// Tokens Oficiales Apple Human Interface Guidelines
private val IosBackgroundGrouped = Color(0xFFF2F2F7)
private val IosCardSurface = Color(0xFFFFFFFF)
private val IosBorderSeparator = Color(0xFFE5E5EA)
private val IosSystemBlue = Color(0xFF007AFF)
private val IosSystemBlueLight = Color(0xFFE8F1FF)
private val IosSystemGreen = Color(0xFF34C759)
private val IosSystemGreenLight = Color(0xFFE8F9ED)
private val IosSystemRed = Color(0xFFFF3B30)
private val IosSystemAmber = Color(0xFFB25E00)
private val IosSystemPurple = Color(0xFF5856D6)
private val IosLabelPrimary = Color(0xFF000000)
private val IosLabelSecondary = Color(0xFF8E8E93)
private val IosFillQuaternary = Color(0xFFE5E5EA)

@Composable
fun VisitaRegistrationScreen(
    viewModel: VisitaViewModel,
    sellerCode: String,
    isInventarioOnlyMode: Boolean = false,
    onNavigateBack: () -> Unit,
    onRegisteredSuccessfully: () -> Unit,
    onNavigateToSaneamiento: (Long?) -> Unit,
    onNavigateToInventario: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val haptic = LocalHapticFeedback.current

    val proximityState by viewModel.proximityState.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()
    val capturedPhotos by viewModel.capturedPhotos.collectAsState()
    val activeRoute by viewModel.activeRoute.collectAsState()
    val activeModules by viewModel.activeModules.collectAsState()
    val isOperationActive by viewModel.isOperationActive.collectAsState()

    val productosHistorial by viewModel.productosHistorial.collectAsState()
    val stockMap by viewModel.stockMap.collectAsState()
    val isLoadingProductos by viewModel.isLoadingProductos.collectAsState()

    val sugeridoState by viewModel.sugeridoState.collectAsState()
    val isLoadingSugerido by viewModel.isLoadingSugerido.collectAsState()

    val isInventarioActive = activeModules["inventario_cliente"] ?: true

    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var selectedClient by remember { mutableStateOf<PlanRuteoEntity?>(null) }
    var comments by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var showCameraPreview by remember { mutableStateOf(false) }

    val activeMatchedClient = remember(proximityState) {
        when (val state = proximityState) {
            is ProximityUiState.SingleClientMatch -> state.client
            else -> null
        }
    }
    val effectiveClient = selectedClient ?: activeMatchedClient

    // Evaluación automática de proximidad si la pantalla inicia en estado Idle
    LaunchedEffect(Unit) {
        if (viewModel.proximityState.value is ProximityUiState.Idle) {
            viewModel.evaluateProximity("Lunes")
        }
    }

    var lastClientId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(effectiveClient?.clientId) {
        val newId = effectiveClient?.clientId
        if (newId != null) {
            if (newId != lastClientId) {
                lastClientId = newId
                selectedStatus = null
                comments = ""
                isSaving = false
            }
            if (productosHistorial.isEmpty()) {
                viewModel.fetchHistorialProductos(newId)
            }
        }
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

    BackHandler {
        if (!isSaving) {
            viewModel.resetForm()
            onNavigateBack()
        }
    }

    if (showCameraPreview) {
        val targetFile = remember(showCameraPreview) { viewModel.createPrivatePhotoFile() }
        VisitaCameraPreview(
            targetFile = targetFile,
            onPhotoCaptured = {
                viewModel.onPhotoTakenSuccessfully()
                showCameraPreview = false
            },
            onClose = { showCameraPreview = false }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IosColors.BackgroundMesh)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Bar estilo iOS con datos del cliente seleccionado y tipo de negocio en ROJO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
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
                        style = TextStyle(fontSize = 15.sp, color = IosSystemBlue, fontWeight = FontWeight.Normal)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = effectiveClient?.clientName ?: if (isInventarioOnlyMode) "Toma de Stock" else "Registrar Visita",
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        effectiveClient?.clientId?.let {
                            Text(
                                text = "#$it",
                                style = TextStyle(fontSize = 11.sp, color = IosLabelSecondary, fontWeight = FontWeight.Medium)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        val phoneNum = effectiveClient?.telefono?.ifBlank { null } ?: effectiveClient?.celular?.ifBlank { null }
                        if (!phoneNum.isNullOrBlank()) {
                            Text(
                                text = "Tel: $phoneNum",
                                style = TextStyle(fontSize = 11.sp, color = IosSystemBlue, fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        val tipo = effectiveClient?.tipoNegocio
                        if (!tipo.isNullOrBlank()) {
                            Text(
                                text = tipo,
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IosSystemRed)
                            )
                        }
                    }
                }
            }

            // CONTENIDO UNIFICADO DE PASO ÚNICO SEGÚN EL MODO
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (!isInventarioOnlyMode) {
                    // MODO REGISTRO DE VISITA: 1. Estado de Atención
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
                                        .glassCard(
                                            shape = RoundedCornerShape(14.dp),
                                            elevation = if (isSelected) 6.dp else 2.dp,
                                            borderGradient = if (isSelected) IosColors.BlueGradient else IosColors.GlassBorderGradient
                                        )
                                        .background(if (isSelected) IosSystemBlue else Color.Transparent)
                                        .clickable {
                                            if (!isSaving) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                selectedStatus = status
                                            }
                                        },
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
                                        .glassCard(
                                            shape = RoundedCornerShape(14.dp),
                                            elevation = if (isSelected) 6.dp else 2.dp,
                                            borderGradient = if (isSelected) IosColors.BlueGradient else IosColors.GlassBorderGradient
                                        )
                                        .background(if (isSelected) IosSystemBlue else Color.Transparent)
                                        .clickable {
                                            if (!isSaving) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                selectedStatus = status
                                            }
                                        },
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

                    Spacer(modifier = Modifier.height(18.dp))

                    // MODO REGISTRO DE VISITA: 2. OBJETIVO VENTA DEL CLIENTE (LISTADO SIMPLIFICADO: NOMBRE + CANTIDAD SUGERIDA + ESTADO)
                    if (isLoadingSugerido) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard(shape = RoundedCornerShape(16.dp), elevation = 3.dp)
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = IosSystemBlue, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cargando objetivo de venta del cliente...", fontSize = 12.sp, color = IosLabelSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    } else if (sugeridoState != null && sugeridoState!!.sugeridos.isNotEmpty()) {
                        val sug = sugeridoState!!

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard(shape = RoundedCornerShape(18.dp), elevation = 4.dp)
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "OBJETIVO VENTA DEL CLIENTE",
                                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                                    )
                                    Text(
                                        text = sug.cliente ?: effectiveClient?.clientName ?: "Cliente",
                                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEBF3FF))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "${sug.totalSkusSugeridos ?: sug.sugeridos.size} SKUs • Bs. ${String.format(Locale.US, "%.2f", sug.totalSugerido ?: 0.0)}",
                                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosSystemBlue)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // AGRUPAMIENTO VISUAL DIRECTO POR LA COLUMNA JERARQUÍA 1 (SIN SELECTORES)
                            val groupedSugeridos = remember(sug.sugeridos) {
                                sug.sugeridos.groupBy { prod ->
                                    (prod.jerarquia1?.ifBlank { null } ?: "GENERAL").uppercase(Locale.US)
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                groupedSugeridos.forEach { (catName, itemsList) ->
                                    val isHogar = catName.contains("HOGAR") || catName.contains("HOME")
                                    val isPersonal = catName.contains("PERSONAL") || catName.contains("CARE")
                                    val headerColor = when {
                                        isHogar -> IosSystemGreen
                                        isPersonal -> IosSystemBlue
                                        else -> IosSystemPurple
                                    }
                                    val headerBg = when {
                                        isHogar -> Color(0xFFE8F9ED)
                                        isPersonal -> Color(0xFFEBF3FF)
                                        else -> Color(0xFFF3F0FF)
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(headerBg.copy(alpha = 0.4f))
                                            .border(0.8.dp, headerColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = catName,
                                                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = headerColor, letterSpacing = 0.5.sp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(headerBg)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${itemsList.size} SKUs",
                                                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = headerColor)
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            itemsList.forEach { prod ->
                                                val hasBought = prod.comproMesActual || prod.cantidadCompradaMesActual > 0
                                                val badgeBg = if (hasBought) Color(0xFFE8F9ED) else Color(0xFFFFF5E5)
                                                val badgeText = if (hasBought) {
                                                    "Compró ${prod.cantidadCompradaMesActual} un."
                                                } else {
                                                    "Pendiente (${prod.cantSugerida} un.)"
                                                }
                                                val badgeColor = if (hasBought) IosSystemGreen else Color(0xFFB25E00)

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(IosCardSurface)
                                                        .border(0.5.dp, IosBorderSeparator, RoundedCornerShape(10.dp))
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = prod.producto,
                                                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary)
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "Objetivo: ${prod.cantSugerida} un.",
                                                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = headerColor)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(badgeBg)
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = badgeText,
                                                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // MODO REGISTRO DE VISITA: 3. Evidencia Fotográfica
                    Text(
                        text = "EVIDENCIA FOTOGRÁFICA (${capturedPhotos.size})",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(shape = RoundedCornerShape(22.dp), elevation = 6.dp)
                            .padding(12.dp)
                    ) {
                        if (capturedPhotos.isNotEmpty()) {
                            LazyRow(
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
                                    text = if (capturedPhotos.isEmpty()) "Tomar Fotografía" else "+ Agregar Otra Fotografía",
                                    color = IosSystemBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // MODO REGISTRO DE VISITA: 4. Observaciones
                    Text(
                        text = "OBSERVACIONES (OPCIONAL)",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .glassCard(shape = RoundedCornerShape(18.dp), elevation = 4.dp)
                            .padding(14.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        if (comments.isEmpty()) {
                            Text("Detalles adicionales...", fontSize = 14.sp, color = IosLabelSecondary)
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
                } else {
                    // MODO TOMA DE STOCK EN PDV: EXCLUSIVAMENTE Censo de Inventario con Historial de Movimientos
                    if (isLoadingProductos) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = IosSystemBlue)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Cargando historial de productos del cliente...", fontSize = 14.sp, color = IosLabelSecondary)
                            }
                        }
                    } else if (productosHistorial.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard(shape = RoundedCornerShape(18.dp), elevation = 4.dp)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No se encontraron productos en el historial de este cliente.",
                                    style = TextStyle(fontSize = 14.sp, color = IosLabelSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        effectiveClient?.clientId?.let { id -> viewModel.fetchHistorialProductos(id) }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = IosSystemBlue)
                                ) {
                                    Text("Cargar Catálogo / Reintentar", color = Color.White)
                                }
                            }
                        }
                    } else if (isInventarioActive) {
                        Text(
                            text = "CENSO DE STOCK EN PDV E HISTORIAL DE MOVIMIENTOS",
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosSystemBlue, letterSpacing = 0.5.sp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Chips de Filtrado y Agrupamiento Jerárquico Multicapa (J1 -> J2 -> J3)
                        var selectedJ1 by remember { mutableStateOf("Todos") }
                        var selectedJ2 by remember { mutableStateOf("Todos") }
                        var selectedJ3 by remember { mutableStateOf("Todos") }

                        val j1List = remember(productosHistorial) {
                            val list = mutableListOf("Todos")
                            productosHistorial.mapNotNull { it.jerarquia1 ?: it.categoria }.distinct().forEach {
                                if (!list.contains(it) && it.isNotBlank()) list.add(it)
                            }
                            list
                        }

                        val j2List = remember(productosHistorial, selectedJ1) {
                            val base = if (selectedJ1 == "Todos") productosHistorial else productosHistorial.filter { (it.jerarquia1 ?: it.categoria) == selectedJ1 }
                            val list = mutableListOf("Todos")
                            base.mapNotNull { it.jerarquia2 }.distinct().forEach {
                                if (!list.contains(it) && it.isNotBlank()) list.add(it)
                            }
                            list
                        }

                        val j3List = remember(productosHistorial, selectedJ1, selectedJ2) {
                            var base = if (selectedJ1 == "Todos") productosHistorial else productosHistorial.filter { (it.jerarquia1 ?: it.categoria) == selectedJ1 }
                            if (selectedJ2 != "Todos") base = base.filter { it.jerarquia2 == selectedJ2 }
                            val list = mutableListOf("Todos")
                            base.mapNotNull { it.jerarquia3 }.distinct().forEach {
                                if (!list.contains(it) && it.isNotBlank()) list.add(it)
                            }
                            list
                        }

                        val filteredProductsList = remember(productosHistorial, selectedJ1, selectedJ2, selectedJ3) {
                            var list = productosHistorial
                            if (selectedJ1 != "Todos") list = list.filter { (it.jerarquia1 ?: it.categoria) == selectedJ1 }
                            if (selectedJ2 != "Todos") list = list.filter { it.jerarquia2 == selectedJ2 }
                            if (selectedJ3 != "Todos") list = list.filter { it.jerarquia3 == selectedJ3 }
                            list
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Fila 1: Jerarquía 1 / Categoría Principal
                            if (j1List.size > 2) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(j1List, key = { "j1_$it" }) { jer ->
                                        val isSel = selectedJ1 == jer
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) IosSystemBlue else IosFillQuaternary)
                                                .clickable {
                                                    selectedJ1 = jer
                                                    selectedJ2 = "Todos"
                                                    selectedJ3 = "Todos"
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = jer,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) Color.White else IosLabelSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            // Fila 2: Jerarquía 2 / Subcategoría
                            if (j2List.size > 2) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(j2List, key = { "j2_$it" }) { jer ->
                                        val isSel = selectedJ2 == jer
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) IosSystemPurple else IosFillQuaternary)
                                                .clickable {
                                                    selectedJ2 = jer
                                                    selectedJ3 = "Todos"
                                                }
                                                .padding(horizontal = 9.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = jer,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) Color.White else IosLabelSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            // Fila 3: Jerarquía 3 / Marca
                            if (j3List.size > 2) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(j3List, key = { "j3_$it" }) { jer ->
                                        val isSel = selectedJ3 == jer
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) IosSystemGreen else IosFillQuaternary)
                                                .clickable { selectedJ3 = jer }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = jer,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) Color.White else IosLabelSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                filteredProductsList.forEachIndexed { idx, prod ->
                                    val prodKey = viewModel.getProdKey(prod, idx)
                                    val currentStockStr = stockMap[prodKey] ?: ""
                                    var showHistoryDetail by remember { mutableStateOf(false) }

                                    val timelineList = remember(prod) {
                                        val comprasList = (prod.historialCompras ?: emptyList()).map {
                                            EventoTimelineData(fecha = it.fecha ?: "-", tipo = "COMPRA", cantidad = it.cantidad ?: 0)
                                        }
                                        val stocksList = (prod.historicoStocks ?: emptyList()).map {
                                            EventoTimelineData(fecha = it.fecha, tipo = "STOCK", cantidad = it.stockFisico)
                                        }
                                        (comprasList + stocksList).sortedByDescending { it.fecha }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .glassCard(shape = RoundedCornerShape(18.dp), elevation = 4.dp)
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { showHistoryDetail = !showHistoryDetail }
                                            ) {
                                                Text(
                                                    text = prod.producto ?: "Producto",
                                                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary)
                                                )

                                                if (timelineList.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = if (showHistoryDetail) "Ocultar historial" else "Ver historial completo (${timelineList.size})",
                                                        style = TextStyle(fontSize = 12.sp, color = IosSystemBlue, fontWeight = FontWeight.Bold)
                                                    )
                                                }
                                            }

                                            // Stepper de Conteo de Stock Físico Independiente
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                // Botón Menos
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(IosFillQuaternary)
                                                        .border(1.dp, IosBorderSeparator, CircleShape)
                                                        .clickable { viewModel.adjustStockFisico(prodKey, -1) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = IosLabelPrimary)
                                                }

                                                // Campo Numérico
                                                OutlinedTextField(
                                                    value = currentStockStr,
                                                    onValueChange = { input ->
                                                        viewModel.updateStockInput(prodKey, input)
                                                    },
                                                    singleLine = true,
                                                    placeholder = { Text("Cant", fontSize = 12.sp, color = IosLabelSecondary) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IosSystemGreen, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                                                    modifier = Modifier.width(58.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedContainerColor = IosCardSurface,
                                                        unfocusedContainerColor = IosCardSurface,
                                                        focusedBorderColor = IosSystemGreen,
                                                        unfocusedBorderColor = if (currentStockStr.isBlank()) IosSystemRed else IosBorderSeparator
                                                    )
                                                )

                                                // Botón Más
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(IosSystemBlue)
                                                        .clickable { viewModel.adjustStockFisico(prodKey, 1) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                                                }
                                            }
                                        }

                                        // Despliegue Cronológico Unificado de Compras y Censos de Stock Físico
                                        if (showHistoryDetail && timelineList.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(IosFillQuaternary)
                                                    .padding(10.dp)
                                            ) {
                                                Text(
                                                    text = "HISTORIAL CRONOLÓGICO DE MOVIMIENTOS",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = IosLabelSecondary,
                                                    letterSpacing = 0.5.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                timelineList.forEach { ev ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (ev.tipo == "COMPRA") {
                                                            Text("Compra: +${ev.cantidad} un.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IosSystemBlue)
                                                        } else {
                                                            Text("Stock en PDV: ${ev.cantidad} un.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IosSystemGreen)
                                                        }
                                                        Text(ev.fecha, fontSize = 11.sp, color = IosLabelSecondary)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

            // Botón de Confirmación Fijo Inferior
            val isFormValid = if (isInventarioOnlyMode) {
                isOperationActive && effectiveClient != null
            } else {
                isOperationActive && capturedPhotos.isNotEmpty() && selectedStatus != null && effectiveClient != null
            }

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
                            Toast.makeText(appContext, "El GPS debe estar encendido para guardar", Toast.LENGTH_LONG).show()
                            context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            return@Button
                        }
                        if (!isFormValid || isSaving) return@Button
                        isSaving = true
                        val distance = (proximityState as? ProximityUiState.SingleClientMatch)?.distance?.toDouble()
                        viewModel.saveVisita(
                            selectedClient = effectiveClient,
                            isOpportunity = false,
                            opportunityClientName = null,
                            status = if (isInventarioOnlyMode) "ATENDIDO" else selectedStatus,
                            comments = comments,
                            distanceToClientMeters = distance,
                            onSuccess = {
                                val msg = if (isInventarioOnlyMode) "Toma de Stock guardada correctamente" else "Visita registrada correctamente"
                                Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
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
                        val btnText = if (effectiveClient == null) {
                            "Seleccione un cliente"
                        } else if (isInventarioOnlyMode) {
                            "Confirmar y Guardar Toma de Stock"
                        } else {
                            "Confirmar y Guardar Visita"
                        }
                        Text(
                            text = btnText,
                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }
                }
            }
        }
    }
}
}
}
