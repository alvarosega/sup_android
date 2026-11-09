package com.alvarosega.trackingventas.ui.saneamiento

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.alvarosega.trackingventas.util.CatalogoTiposNegocio

import com.alvarosega.trackingventas.ui.theme.glassCard

// Tokens Oficiales Apple Pro
private val IosBackgroundGrouped = Color(0xFFF5F5F7)
private val IosCardSurface = Color(0xFFFFFFFF)
private val IosBorderSeparator = Color(0xFFE5E5EA)
private val IosSystemBlue = Color(0xFF007AFF)
private val IosSystemGreen = Color(0xFF248A3D)
private val IosSystemRed = Color(0xFFFF3B30)
private val IosSystemAmber = Color(0xFFB25E00)
private val IosAmberBackground = Color(0xFFFFF5E5)
private val IosAmberSurfaceTint = Color(0xFFFFFDF9)
private val IosGreenBackground = Color(0xFFEBF9EF)
private val IosSystemPurple = Color(0xFF5856D6)
private val IosLabelPrimary = Color(0xFF1D1D1F)
private val IosLabelSecondary = Color(0xFF6E6E73)
private val IosFillQuaternary = Color(0xFFEFEFF4)

@Composable
fun SaneamientoFormScreen(
    viewModel: SaneamientoViewModel,
    clientId: Long?,
    sellerCode: String,
    onNavigateBack: () -> Unit,
    onSavedSuccessfully: () -> Unit
) {
    val context = LocalContext.current
    val formState by viewModel.uiState.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()

    var dropdownTipoNegocioExpanded by remember { mutableStateOf(false) }
    var dropdownZonaExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(clientId) {
        viewModel.initForm(clientId)
    }

    LaunchedEffect(errorMsg) {
        errorMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    var showCameraPreview by remember { mutableStateOf(false) }
    var showFullscreenMapDialog by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCameraPreview = true
        } else {
            Toast.makeText(context, "Permiso de cámara obligatorio", Toast.LENGTH_SHORT).show()
        }
    }



    fun dispatchCameraCapture() {
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

    // Estados de validación individual de campos obligatorios
    val clienteWords = formState.cliente.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val isClienteOnlyLetters = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$").matches(formState.cliente.trim())
    val isClienteValid = clienteWords.size >= 2 && isClienteOnlyLetters

    val isTipoNegocioValid = formState.tipoNegocio.isNotBlank()
    val isZonaValid = formState.zona.isNotBlank()

    val digitsCel = formState.celular.filter { it.isDigit() }
    val isCelularValid = digitsCel.length in 7..8

    val direccionWords = formState.direccion.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val isDireccionValid = if (formState.esEdicion) {
        formState.direccion.isNotBlank()
    } else {
        direccionWords.size >= 2
    }

    val isNombreFacturaValid = formState.nombreFactura.isNotBlank() || formState.cliente.isNotBlank()

    val digitsNit = formState.nit.filter { it.isDigit() }
    val isNitValid = formState.nit.isNotBlank() && digitsNit.isNotEmpty() && formState.nit.all { it.isDigit() } &&
            (digitsNit == "0" || digitsNit.length >= 5)

    val isFormValid = if (formState.esEdicion) {
        isClienteValid && isTipoNegocioValid && isZonaValid && isCelularValid && isDireccionValid && isNombreFacturaValid && isNitValid
    } else {
        isClienteValid && isTipoNegocioValid && isZonaValid && isCelularValid && isDireccionValid && isNombreFacturaValid && isNitValid &&
                formState.photoCaptured && formState.gpsReady
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IosBackgroundGrouped)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // TopBar estilo Apple Pro
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(IosCardSurface)
                    .border(1.dp, IosBorderSeparator)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { if (!formState.isSaving) onNavigateBack() }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = IosSystemBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancelar", style = TextStyle(fontSize = 15.sp, color = IosSystemBlue))
                }

                Text(
                    text = if (formState.esEdicion) "Editar Cliente" else "Alta Cliente",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = IosLabelPrimary)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (formState.esEdicion) IosAmberBackground else IosGreenBackground)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (formState.esEdicion) "EDITAR #${formState.clienteId}" else "ALTA",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (formState.esEdicion) IosSystemAmber else IosSystemGreen
                        )
                    )
                }
            }

            // Formulario scrolleable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // SECCIÓN 1: DATOS COMERCIALES
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "DATOS COMERCIALES",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                    )

                    // Nombre o Razón Social (OBLIGATORIO)
                    AppleProFieldContainer(
                        label = "Nombre y Apellido Paterno (ej. Juan Perez)",
                        isMandatory = true,
                        isValid = isClienteValid
                    ) {
                        BasicTextField(
                            value = formState.cliente,
                            onValueChange = { viewModel.updateField { copy(cliente = it) } },
                            singleLine = true,
                            cursorBrush = SolidColor(IosSystemBlue),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = IosLabelPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Tipo de Negocio (OBLIGATORIO)
                    AppleProFieldContainer(
                        label = "Tipo de Negocio",
                        isMandatory = true,
                        isValid = isTipoNegocioValid,
                        modifier = Modifier.clickable { dropdownTipoNegocioExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formState.tipoNegocio.ifEmpty { "Seleccionar catálogo..." },
                                fontSize = 15.sp,
                                color = if (formState.tipoNegocio.isEmpty()) IosLabelSecondary else IosLabelPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = IosLabelSecondary)
                        }

                        DropdownMenu(
                            expanded = dropdownTipoNegocioExpanded,
                            onDismissRequest = { dropdownTipoNegocioExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            CatalogoTiposNegocio.TIPOS.forEach { tipo ->
                                DropdownMenuItem(
                                    text = { Text(tipo, fontSize = 14.sp) },
                                    onClick = {
                                        viewModel.updateField { copy(tipoNegocio = tipo) }
                                        dropdownTipoNegocioExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Zona Comercial (OBLIGATORIO)
                    AppleProFieldContainer(
                        label = "Zona Comercial",
                        isMandatory = true,
                        isValid = isZonaValid,
                        modifier = Modifier.clickable { dropdownZonaExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formState.zona.ifEmpty { "Seleccionar zona comercial..." },
                                fontSize = 15.sp,
                                color = if (formState.zona.isEmpty()) IosLabelSecondary else IosLabelPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = IosLabelSecondary)
                        }

                        DropdownMenu(
                            expanded = dropdownZonaExpanded,
                            onDismissRequest = { dropdownZonaExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            CatalogoTiposNegocio.ZONAS.forEach { z ->
                                DropdownMenuItem(
                                    text = { Text(z, fontSize = 14.sp) },
                                    onClick = {
                                        viewModel.updateField { copy(zona = z) }
                                        dropdownZonaExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Contacto (OPCIONAL)
                    AppleProFieldContainer(
                        label = "Nombre de Contacto",
                        isMandatory = false,
                        isValid = true
                    ) {
                        BasicTextField(
                            value = formState.contacto,
                            onValueChange = { viewModel.updateField { copy(contacto = it) } },
                            singleLine = true,
                            cursorBrush = SolidColor(IosSystemBlue),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = IosLabelPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // SECCIÓN 2: COMUNICACIÓN Y UBICACIÓN
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "COMUNICACIÓN Y UBICACIÓN",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                    )

                    // Celular (OBLIGATORIO)
                    AppleProFieldContainer(
                        label = "Celular (7-8 dígitos)",
                        isMandatory = true,
                        isValid = isCelularValid
                    ) {
                        BasicTextField(
                            value = formState.celular,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }
                                viewModel.updateField { copy(celular = filtered) }
                            },
                            singleLine = true,
                            cursorBrush = SolidColor(IosSystemBlue),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = IosLabelPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Teléfono Fijo (OPCIONAL)
                    AppleProFieldContainer(
                        label = "Teléfono Fijo",
                        isMandatory = false,
                        isValid = true
                    ) {
                        BasicTextField(
                            value = formState.telefono,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }
                                viewModel.updateField { copy(telefono = filtered) }
                            },
                            singleLine = true,
                            cursorBrush = SolidColor(IosSystemBlue),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = IosLabelPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Dirección (OBLIGATORIA)
                    AppleProFieldContainer(
                        label = "Dirección (Mínimo 3 palabras)",
                        isMandatory = true,
                        isValid = isDireccionValid
                    ) {
                        BasicTextField(
                            value = formState.direccion,
                            onValueChange = { viewModel.updateField { copy(direccion = it) } },
                            singleLine = true,
                            cursorBrush = SolidColor(IosSystemBlue),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = IosLabelPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Referencia (OPCIONAL)
                    AppleProFieldContainer(
                        label = "Referencia visual del local",
                        isMandatory = false,
                        isValid = true
                    ) {
                        BasicTextField(
                            value = formState.referencia,
                            onValueChange = { viewModel.updateField { copy(referencia = it) } },
                            singleLine = true,
                            cursorBrush = SolidColor(IosSystemBlue),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = IosLabelPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // SECCIÓN 3: DATOS DE FACTURACIÓN (OBLIGATORIOS)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "DATOS DE FACTURACIÓN",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                    )

                    // Nombre Factura (OBLIGATORIO)
                    AppleProFieldContainer(
                        label = "Razón Social / Nombre Factura",
                        isMandatory = true,
                        isValid = isNombreFacturaValid
                    ) {
                        BasicTextField(
                            value = formState.nombreFactura,
                            onValueChange = { viewModel.updateField { copy(nombreFactura = it) } },
                            singleLine = true,
                            cursorBrush = SolidColor(IosSystemBlue),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = IosLabelPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // NIT / CI (OBLIGATORIO)
                    AppleProFieldContainer(
                        label = "NIT / CI (Mínimo 5 dígitos, 0 si no tiene)",
                        isMandatory = true,
                        isValid = isNitValid
                    ) {
                        BasicTextField(
                            value = formState.nit,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }
                                viewModel.updateField { copy(nit = filtered) }
                            },
                            singleLine = true,
                            cursorBrush = SolidColor(IosSystemBlue),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = IosLabelPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // SECCIÓN 4: EVIDENCIA Y GPS (SOLO ALTA)
                if (!formState.esEdicion) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "EVIDENCIA FOTOGRÁFICA Y GPS",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                        )

                        // Tarjeta de Mini Mapa Visual Embebido
                        val loc by viewModel.capturedLocation.collectAsState()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(IosCardSurface)
                                .border(1.dp, if (formState.gpsReady && loc != null) IosBorderSeparator else IosSystemAmber, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("UBICACIÓN EN EL MAPA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (formState.gpsReady && loc != null) IosGreenBackground else IosAmberBackground)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (formState.gpsReady && loc != null) "UBICACIÓN FIJADA" else "BUSCANDO GPS",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (formState.gpsReady && loc != null) IosSystemGreen else IosSystemAmber
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Toque o mueva el mapa para situar la entrada del comercio:", fontSize = 12.sp, color = IosLabelSecondary)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Mini Mapa OSM Embebido en el Formulario
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(10.dp))
                                ) {
                                    if (loc != null) {
                                        androidx.compose.ui.viewinterop.AndroidView(
                                            factory = { ctx ->
                                                org.osmdroid.views.MapView(ctx).apply {
                                                    setMultiTouchControls(true)
                                                    controller.setZoom(18.5)
                                                    val startPoint = org.osmdroid.util.GeoPoint(loc!!.latitude, loc!!.longitude)
                                                    controller.setCenter(startPoint)

                                                    val marker = org.osmdroid.views.overlay.Marker(this).apply {
                                                        position = startPoint
                                                        title = null
                                                        snippet = null
                                                        infoWindow = null
                                                        icon = getBluePinMarkerDrawable(ctx)
                                                        setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                                                    }
                                                    overlays.add(marker)

                                                    val mapEventsReceiver = object : org.osmdroid.events.MapEventsReceiver {
                                                        override fun singleTapConfirmedHelper(p: org.osmdroid.util.GeoPoint?): Boolean {
                                                            if (p != null) {
                                                                marker.position = p
                                                                viewModel.setManualLocation(p.latitude, p.longitude)
                                                                invalidate()
                                                            }
                                                            return true
                                                        }

                                                        override fun longPressHelper(p: org.osmdroid.util.GeoPoint?): Boolean {
                                                            if (p != null) {
                                                                marker.position = p
                                                                viewModel.setManualLocation(p.latitude, p.longitude)
                                                                invalidate()
                                                            }
                                                            return true
                                                        }
                                                    }
                                                    overlays.add(org.osmdroid.views.overlay.MapEventsOverlay(mapEventsReceiver))
                                                }
                                            },
                                            update = { mapView ->
                                                loc?.let { l ->
                                                    val p = org.osmdroid.util.GeoPoint(l.latitude, l.longitude)
                                                    mapView.controller.setCenter(p)
                                                    mapView.overlays.filterIsInstance<org.osmdroid.views.overlay.Marker>().firstOrNull()?.position = p
                                                    mapView.invalidate()
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(color = IosSystemAmber, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Cargando señal GPS...", fontSize = 12.sp, color = IosSystemAmber)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showFullscreenMapDialog = true },
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, IosSystemBlue)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Place, contentDescription = null, tint = IosSystemBlue, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ver Mapa Completo", fontSize = 12.sp, color = IosSystemBlue, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.capturarGpsParaAlta() },
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Recalcular GPS", fontSize = 12.sp, color = IosLabelPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        val isPhotoValid = formState.photoCaptured && viewModel.activePhotoFile != null

                        val borderModifier = if (isPhotoValid) {
                            Modifier.border(1.dp, IosBorderSeparator, RoundedCornerShape(8.dp))
                        } else {
                            Modifier.border(1.dp, IosSystemAmber, RoundedCornerShape(8.dp))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPhotoValid) IosCardSurface else IosAmberSurfaceTint)
                                .then(borderModifier)
                                .padding(12.dp)
                        ) {
                            if (isPhotoValid) {
                                val activeFile = viewModel.activePhotoFile
                                val bitmap = remember(activeFile, activeFile?.lastModified(), activeFile?.length()) {
                                    if (activeFile != null && activeFile.exists() && activeFile.length() > 0) {
                                        BitmapFactory.decodeFile(activeFile.absolutePath)?.asImageBitmap()
                                    } else null
                                }
                                if (bitmap != null) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Fotografía de Fachada", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = IosLabelSecondary)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(IosGreenBackground)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("VÁLIDO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = IosSystemGreen)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = "Fachada",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        TextButton(
                                            onClick = { dispatchCameraCapture() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Repetir Fotografía", color = IosSystemBlue, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { dispatchCameraCapture() }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(IosAmberBackground),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = IosSystemAmber, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Tomar Foto de Fachada", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = IosLabelPrimary)
                                            Text("Fotografía de la fachada comercial", fontSize = 11.sp, color = IosLabelSecondary)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(IosAmberBackground)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("REQUERIDO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = IosSystemAmber)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Barra Fija Inferior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IosCardSurface)
                    .border(0.5.dp, IosBorderSeparator)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        if (!isFormValid || formState.isSaving) return@Button
                        viewModel.guardarRegistro {
                            Toast.makeText(context, "Registro procesado con éxito", Toast.LENGTH_SHORT).show()
                            onSavedSuccessfully()
                        }
                    },
                    enabled = isFormValid && !formState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (formState.esEdicion) IosSystemPurple else IosSystemGreen,
                        disabledContainerColor = Color(0xFFE5E5EA)
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    if (formState.isSaving) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            text = if (formState.esEdicion) "Guardar Sugerencia de Edición" else "Registrar Alta Cliente",
                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isFormValid) Color.White else IosLabelSecondary)
                        )
                    }
                }
            }
        }
    }

    if (showCameraPreview) {
        val targetFile = remember(showCameraPreview) { viewModel.getOrCreatePrivatePhotoFile() }
        com.alvarosega.trackingventas.ui.camera.VisitaCameraPreview(
            targetFile = targetFile,
            onPhotoCaptured = {
                viewModel.processCapturedPhoto(sellerCode) {
                    showCameraPreview = false
                }
            },
            onClose = {
                showCameraPreview = false
            }
        )
    }

    if (showFullscreenMapDialog) {
        val locState by viewModel.capturedLocation.collectAsState()
        val loc = locState
        if (loc != null) {
            Dialog(
                onDismissRequest = { showFullscreenMapDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = IosBackgroundGrouped
                ) {
                    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                        // Header de Diálogo Completo
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(IosCardSurface)
                                .border(0.5.dp, IosBorderSeparator)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = IosSystemBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Validar Ubicación Exacta del Local",
                                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary)
                                )
                            }

                            IconButton(onClick = { showFullscreenMapDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = IosLabelSecondary)
                            }
                        }

                        // MapView de Pantalla Completa
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    org.osmdroid.views.MapView(ctx).apply {
                                        setMultiTouchControls(true)
                                        controller.setZoom(19.0)
                                        val point = org.osmdroid.util.GeoPoint(loc.latitude, loc.longitude)
                                        controller.setCenter(point)

                                        val marker = org.osmdroid.views.overlay.Marker(this).apply {
                                            position = point
                                            title = null
                                            snippet = null
                                            infoWindow = null
                                            icon = getBluePinMarkerDrawable(ctx)
                                            setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                                        }
                                        overlays.add(marker)

                                        val mapEventsReceiver = object : org.osmdroid.events.MapEventsReceiver {
                                            override fun singleTapConfirmedHelper(p: org.osmdroid.util.GeoPoint?): Boolean {
                                                if (p != null) {
                                                    marker.position = p
                                                    viewModel.setManualLocation(p.latitude, p.longitude)
                                                    invalidate()
                                                }
                                                return true
                                            }

                                            override fun longPressHelper(p: org.osmdroid.util.GeoPoint?): Boolean {
                                                if (p != null) {
                                                    marker.position = p
                                                    viewModel.setManualLocation(p.latitude, p.longitude)
                                                    invalidate()
                                                }
                                                return true
                                            }
                                        }
                                        overlays.add(org.osmdroid.views.overlay.MapEventsOverlay(mapEventsReceiver))
                                    }
                                },
                                update = { mapView ->
                                    val p = org.osmdroid.util.GeoPoint(loc.latitude, loc.longitude)
                                    mapView.overlays.filterIsInstance<org.osmdroid.views.overlay.Marker>().firstOrNull()?.position = p
                                    mapView.invalidate()
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Coordenadas flotantes en la parte superior del mapa
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(IosCardSurface.copy(alpha = 0.92f))
                                    .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Toque el mapa para situar la entrada • Lat ${loc.latitude.toString().take(8)}, Lon ${loc.longitude.toString().take(8)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IosSystemBlue
                                )
                            }
                        }

                        // Botón de Confirmación
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(IosCardSurface)
                                .border(0.5.dp, IosBorderSeparator)
                                .padding(16.dp)
                        ) {
                            Button(
                                onClick = { showFullscreenMapDialog = false },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IosSystemGreen)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Confirmar Ubicación Exacta", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Contenedor estilizado estilo Apple Pro.
 * Enmarca en Ámbar (#B25E00) con fondo de advertencia sutil (#FFFDF9) si el campo es obligatorio y está incompleto.
 * Si es válido o es opcional, mantiene un borde neutro (#E5E5EA) con fondo blanco puro.
 */
@Composable
private fun AppleProFieldContainer(
    label: String,
    isMandatory: Boolean,
    isValid: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val needsAttention = isMandatory && !isValid

    val borderModifier = if (needsAttention) {
        Modifier.border(1.2.dp, IosSystemRed, RoundedCornerShape(10.dp))
    } else {
        Modifier.border(0.8.dp, IosBorderSeparator, RoundedCornerShape(10.dp))
    }

    val backgroundColor = if (needsAttention) Color(0xFFFFF5F5) else IosCardSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .then(borderModifier)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isMandatory) label else "$label (Opcional)",
                fontSize = 11.sp,
                color = if (needsAttention) IosSystemRed else IosLabelSecondary,
                fontWeight = FontWeight.SemiBold
            )

            if (isMandatory) {
                if (!isValid) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFEBEA))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "REQUERIDO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosSystemRed
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(IosGreenBackground)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "VÁLIDO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosSystemGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}

private fun getBluePinMarkerDrawable(context: android.content.Context): android.graphics.drawable.Drawable {
    val width = 48
    val height = 60
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val pinColor = 0xFF007AFF.toInt()
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = pinColor
        style = android.graphics.Paint.Style.FILL
    }

    val path = android.graphics.Path().apply {
        moveTo(width / 2f - 18f, 24f)
        lineTo(width / 2f + 18f, 24f)
        lineTo(width / 2f, height.toFloat())
        close()
    }

    canvas.drawCircle(width / 2f, 22f, 20f, paint)
    canvas.drawPath(path, paint)

    val centerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(width / 2f, 22f, 8f, centerPaint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}