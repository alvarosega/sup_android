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

// Tokens Oficiales Apple Pro
private val IosBackgroundGrouped = Color(0xFFF5F5F7)
private val IosCardSurface = Color(0xFFFFFFFF)
private val IosBorderSeparator = Color(0xFFE5E5EA)
private val IosSystemBlue = Color(0xFF007AFF)
private val IosSystemGreen = Color(0xFF248A3D)
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
    val isClienteValid = formState.cliente.isNotBlank() &&
            !Regex("[#$+%_=<>{}\\[\\]\\^]").containsMatchIn(formState.cliente)
    val isTipoNegocioValid = formState.tipoNegocio.isNotBlank()
    val isZonaValid = formState.zona.isNotBlank()
    val digitsCel = formState.celular.filter { it.isDigit() }
    val isCelularValid = digitsCel.length in 7..8
    val isNombreFacturaValid = formState.nombreFactura.isNotBlank()
    val digitsNit = formState.nit.filter { it.isDigit() }
    val isNitValid = formState.nit.isNotBlank() && digitsNit.isNotEmpty() && formState.nit.all { it.isDigit() }

    val isFormValid = if (formState.esEdicion) {
        isClienteValid && isTipoNegocioValid && isZonaValid && isCelularValid && isNombreFacturaValid && isNitValid
    } else {
        isClienteValid && isTipoNegocioValid && isZonaValid && isCelularValid && isNombreFacturaValid && isNitValid &&
                formState.photoCaptured && formState.gpsReady
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
                        label = "Nombre o Razón Social",
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

                    // Dirección (OPCIONAL)
                    AppleProFieldContainer(
                        label = "Dirección",
                        isMandatory = false,
                        isValid = true
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
                        label = "NIT / CI Facturación (0 si no tiene)",
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "EVIDENCIA FOTOGRÁFICA Y GPS",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                        )

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
                                            Text("Evidencia física obligatoria", fontSize = 11.sp, color = IosLabelSecondary)
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
        Modifier.border(1.dp, IosSystemAmber, RoundedCornerShape(8.dp))
    } else {
        Modifier.border(1.dp, IosBorderSeparator, RoundedCornerShape(8.dp))
    }

    val backgroundColor = if (needsAttention) IosAmberSurfaceTint else IosCardSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
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
                color = if (needsAttention) IosSystemAmber else IosLabelSecondary,
                fontWeight = FontWeight.SemiBold
            )

            if (isMandatory) {
                if (!isValid) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(IosAmberBackground)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "REQUERIDO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosSystemAmber
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

        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}