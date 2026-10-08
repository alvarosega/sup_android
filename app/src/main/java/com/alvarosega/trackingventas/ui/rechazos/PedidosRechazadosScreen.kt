package com.alvarosega.trackingventas.ui.rechazos

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.alvarosega.trackingventas.data.local.entity.PedidoRechazadoEntity
import com.alvarosega.trackingventas.data.remote.dto.MotivoRechazoDto
import com.alvarosega.trackingventas.data.remote.dto.PreventaPendienteDto
import com.alvarosega.trackingventas.ui.camera.VisitaCameraPreview
import java.io.File

// Tokens de diseño estilo Apple iOS HIG
private val IosBackground = Color(0xFFF2F2F7)
private val IosCardSurface = Color(0xFFFFFFFF)
private val IosBorderSeparator = Color(0xFFC6C6C8)
private val IosSystemBlue = Color(0xFF007AFF)
private val IosSystemGreen = Color(0xFF34C759)
private val IosSystemRed = Color(0xFFFF3B30)
private val IosSystemOrange = Color(0xFFFF9500)
private val IosLabelPrimary = Color(0xFF000000)
private val IosLabelSecondary = Color(0xFF8E8E93)
private val IosFillQuaternary = Color(0xFFE5E5EA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosRechazadosScreen(
    viewModel: PedidosRechazadosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val appContext = remember { context.applicationContext }

    val statusMsg by viewModel.statusMessage.collectAsState()
    val localRechazos by viewModel.localRechazos.collectAsState()
    val preventasPendientes by viewModel.preventasPendientes.collectAsState()
    val selectedPreventa by viewModel.selectedPreventa.collectAsState()
    val tipoRechazo by viewModel.tipoRechazo.collectAsState()
    val motivosList by viewModel.motivosList.collectAsState()
    val selectedMotivo by viewModel.selectedMotivo.collectAsState()
    val comentarios by viewModel.comentarios.collectAsState()
    val itemsState by viewModel.itemsState.collectAsState()
    val capturedPhotoFile by viewModel.capturedPhotoFile.collectAsState()
    val isLoadingPreventas by viewModel.isLoadingPreventas.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Preventas Pendientes, 1: Historial Local
    var pendingFilter by remember { mutableStateOf("TODOS") } // "TODOS", "PARCIAL", "TOTAL"
    var showCameraPreview by remember { mutableStateOf(false) }
    var showJustifySheet by remember { mutableStateOf(false) }
    var showMotivoDropdown by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCameraPreview = true
        } else {
            Toast.makeText(appContext, "Permiso de cámara obligatorio", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchPreventasPendientes()
    }

    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    if (showCameraPreview) {
        val photoFile = remember { viewModel.createPrivatePhotoFile() }
        VisitaCameraPreview(
            targetFile = photoFile,
            onPhotoCaptured = {
                showCameraPreview = false
                Toast.makeText(context, "Fotografía de evidencia capturada", Toast.LENGTH_SHORT).show()
            },
            onClose = {
                showCameraPreview = false
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IosBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // TOP BAR ESTILO APPLE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
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
                    .clickable { onNavigateBack() }
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Atrás",
                    tint = IosSystemBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Inicio", style = TextStyle(fontSize = 16.sp, color = IosSystemBlue))
            }

            Text(
                text = "Pedidos Rechazados",
                style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IosLabelPrimary)
            )

            Box(modifier = Modifier.width(60.dp))
        }

        // SEGMENTED CONTROL TABS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IosFillQuaternary)
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedTab == 0) IosCardSurface else Color.Transparent)
                    .clickable { selectedTab = 0 },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Pendientes (${preventasPendientes.size})",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (preventasPendientes.isNotEmpty()) IosSystemRed else IosLabelPrimary
                    )
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedTab == 1) IosCardSurface else Color.Transparent)
                    .clickable { selectedTab = 1 },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Historial (${localRechazos.size})",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                        color = IosLabelPrimary
                    )
                )
            }
        }

        if (selectedTab == 0) {
            // TAB 0: PREVENTAS RECHAZADAS PENDIENTES DE JUSTIFICAR
            if (isLoadingPreventas) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = IosSystemBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Consultando preventas rechazadas...", color = IosLabelSecondary, fontSize = 14.sp)
                    }
                }
            } else if (preventasPendientes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = IosSystemGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("¡Excelente!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = IosLabelPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("No tienes preventas rechazadas pendientes de justificar.", color = IosLabelSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Filtros por Tipo de Rechazo (Todos, Parciales, Totales)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("TODOS", "PARCIAL", "TOTAL").forEach { filter ->
                            val isSel = pendingFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) IosSystemBlue else IosFillQuaternary)
                                    .clickable { pendingFilter = filter }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = when (filter) {
                                        "PARCIAL" -> "Parciales"
                                        "TOTAL" -> "Totales"
                                        else -> "Todos (${preventasPendientes.size})"
                                    },
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else IosLabelPrimary
                                    )
                                )
                            }
                        }
                    }

                    val filteredList = remember(preventasPendientes, pendingFilter) {
                        when (pendingFilter) {
                            "PARCIAL" -> preventasPendientes.filter { (it.tipoSugerido ?: "PARCIAL") == "PARCIAL" }
                            "TOTAL" -> preventasPendientes.filter { (it.tipoSugerido ?: "PARCIAL") == "TOTAL" }
                            else -> preventasPendientes
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        items(filteredList) { preventa ->
                        var showDetails by remember { mutableStateOf(false) }
                        val rejectedItems = remember(preventa) {
                            preventa.itemsRechazados.ifEmpty { preventa.items.filter { (it.cantidadRechazada ?: 0) > 0 } }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(IosCardSurface)
                                .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(14.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preventa.clienteNombre ?: "Cliente #${preventa.clienteId}",
                                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Preventa #${preventa.nroPreventa} • Ruta: ${preventa.ruta ?: "General"}",
                                        style = TextStyle(fontSize = 13.sp, color = IosSystemBlue, fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Fecha Preventa: ${preventa.fechaPreventa}",
                                        style = TextStyle(fontSize = 12.sp, color = IosLabelSecondary)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (preventa.tipoSugerido == "TOTAL") Color(0xFFFFEBEA) else Color(0xFFFFF3CD))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = preventa.tipoSugerido ?: "RECHAZO",
                                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (preventa.tipoSugerido == "TOTAL") IosSystemRed else IosSystemOrange)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Preventa: Bs. ${preventa.montoTotalPreventa ?: 0.0} • Facturado: Bs. ${preventa.montoTotalFacturado ?: 0.0}",
                                    style = TextStyle(fontSize = 13.sp, color = IosLabelSecondary)
                                )
                                Text(
                                    text = "Monto Rechazado: Bs. ${preventa.montoTotalRechazado ?: 0.0} (${preventa.totalItemsRechazados ?: preventa.items.size} ítems)",
                                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IosSystemRed)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Detalle desplegable de productos rechazados
                            if (rejectedItems.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showDetails = !showDetails }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (showDetails) "Ocultar detalle de productos (${rejectedItems.size})" else "Ver detalle de productos rechazados (${rejectedItems.size})",
                                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosSystemBlue)
                                    )
                                    Icon(
                                        imageVector = if (showDetails) Icons.Default.ArrowDropDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = IosSystemBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                if (showDetails) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    rejectedItems.forEach { item ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(IosFillQuaternary)
                                                .padding(8.dp)
                                        ) {
                                            Text(item.productoNombre, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IosLabelPrimary)
                                            Text(
                                                text = "Pedidas: ${item.cantidadPreventa} • Facturadas: ${item.cantidadFacturada ?: 0} • Rechazadas: ${item.cantidadRechazada ?: 0}",
                                                fontSize = 11.sp,
                                                color = IosSystemRed,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if ((item.montoRechazado ?: 0.0) > 0.0) {
                                                Text("Monto Rechazado: Bs. ${item.montoRechazado}", fontSize = 11.sp, color = IosLabelSecondary)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.selectPreventa(preventa)
                                        showJustifySheet = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = IosSystemRed),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Justificar Rechazo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    } else {
            // TAB 1: HISTORIAL LOCAL DE JUSTIFICACIONES
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (localRechazos.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Sin justificaciones de rechazo registradas localmente", color = IosLabelSecondary, fontSize = 14.sp)
                        }
                    }
                } else {
                    items(localRechazos) { rechazo ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(IosCardSurface)
                                .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(14.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = rechazo.clienteNombre ?: "Cliente #${rechazo.clienteId}",
                                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary)
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (rechazo.isSynced) Color(0xFFE8F9ED) else Color(0xFFFFF3CD))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (rechazo.isSynced) "Sincronizado" else "Pendiente Sync",
                                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (rechazo.isSynced) IosSystemGreen else IosSystemOrange)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Motivo: ${rechazo.motivo} • Tipo: ${rechazo.tipoRechazo}", fontSize = 13.sp, color = IosSystemRed, fontWeight = FontWeight.SemiBold)
                            Text("Fecha: ${rechazo.fechaRechazo}", fontSize = 12.sp, color = IosLabelSecondary)
                            if (!rechazo.nroPreventa.isNullOrBlank()) {
                                Text("Preventa: #${rechazo.nroPreventa}", fontSize = 12.sp, color = IosSystemBlue)
                            }
                            if (!rechazo.comentarios.isNullOrBlank()) {
                                Text("Obs: ${rechazo.comentarios}", fontSize = 12.sp, color = IosLabelSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }

    // MODAL SHEET / DIÁLOGO DE JUSTIFICACIÓN DE RECHAZO
    if (showJustifySheet && selectedPreventa != null) {
        val preventa = selectedPreventa!!

        ModalBottomSheet(
            onDismissRequest = { showJustifySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = IosBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "JUSTIFICAR RECHAZO",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IosSystemRed)
                    )

                    IconButton(onClick = { showJustifySheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = IosLabelSecondary)
                    }
                }

                Text(
                    text = "${preventa.clienteNombre} • Preventa #${preventa.nroPreventa}",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = IosLabelPrimary)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. TIPO DE RECHAZO (DETERMINADO POR EL SISTEMA)
                Text(
                    text = "TIPO DE RECHAZO DENEGADO",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (tipoRechazo == "TOTAL") Color(0xFFFFEBEA) else Color(0xFFFFF3CD))
                        .border(1.dp, if (tipoRechazo == "TOTAL") IosSystemRed else IosSystemOrange, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (tipoRechazo == "TOTAL") "RECHAZO TOTAL REGISTRADO POR EL SISTEMA" else "RECHAZO PARCIAL REGISTRADO POR EL SISTEMA",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (tipoRechazo == "TOTAL") IosSystemRed else IosSystemOrange)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. MOTIVO PRINCIPAL
                Text(
                    text = "MOTIVO DE RECHAZO",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedMotivo?.nombre ?: "Seleccionar motivo",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Motivo principal") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMotivoDropdown = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = IosCardSurface,
                            unfocusedContainerColor = IosCardSurface
                        )
                    )

                    DropdownMenu(
                        expanded = showMotivoDropdown,
                        onDismissRequest = { showMotivoDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        motivosList.forEach { motivo ->
                            DropdownMenuItem(
                                text = { Text(motivo.nombre, fontSize = 14.sp) },
                                onClick = {
                                    viewModel.setSelectedMotivo(motivo)
                                    showMotivoDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. PRODUCTOS DE LA PREVENTA
                Text(
                    text = "PRODUCTOS A JUSTIFICAR (${itemsState.size})",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                itemsState.forEachIndexed { index, editable ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(IosCardSurface)
                            .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(editable.item.productoNombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Cód: ${editable.item.codigoProducto} • Precio: Bs. ${editable.item.precioUnitario}", fontSize = 12.sp, color = IosLabelSecondary)
                                Text("Pedida: ${editable.item.cantidadPreventa} • Facturada: ${editable.item.cantidadFacturada ?: 0}", fontSize = 12.sp, color = IosSystemBlue, fontWeight = FontWeight.SemiBold)
                            }

                            if (tipoRechazo == "PARCIAL") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updateItemCantidadRechazada(index, editable.cantidadRechazada - 1) },
                                        modifier = Modifier.size(32.dp).clip(CircleShape).background(IosFillQuaternary)
                                    ) {
                                        Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }

                                    Text(
                                        text = "${editable.cantidadRechazada}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = IosSystemRed
                                    )

                                    IconButton(
                                        onClick = { viewModel.updateItemCantidadRechazada(index, editable.cantidadRechazada + 1) },
                                        modifier = Modifier.size(32.dp).clip(CircleShape).background(IosFillQuaternary)
                                    ) {
                                        Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            } else {
                                Text("Rechazo: ${editable.cantidadRechazada}", fontWeight = FontWeight.Bold, color = IosSystemRed, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. COMENTARIOS Y EVIDENCIA
                Text(
                    text = "OBSERVACIONES Y FOTO DE EVIDENCIA",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = comentarios,
                    onValueChange = { viewModel.setComentarios(it) },
                    label = { Text("Comentarios u observaciones adicionales") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = IosCardSurface,
                        unfocusedContainerColor = IosCardSurface
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (capturedPhotoFile != null && capturedPhotoFile!!.exists()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(IosCardSurface)
                            .padding(8.dp)
                    ) {
                        val photoPath = capturedPhotoFile?.absolutePath ?: ""
                        val bitmap = remember(photoPath) {
                            if (photoPath.isNotBlank()) BitmapFactory.decodeFile(photoPath) else null
                        }

                        if (bitmap != null) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Foto evidencia",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .clickable { viewModel.removeCapturedPhoto() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Fotografía de evidencia guardada", fontSize = 13.sp, color = IosSystemGreen, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                showCameraPreview = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IosFillQuaternary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = IosSystemBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tomar Fotografía de Evidencia (Opcional)", color = IosSystemBlue, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. BOTÓN CONFIRMAR Y ENVIAR JUSTIFICACIÓN
                Button(
                    onClick = {
                        if (!viewModel.isGpsEnabled()) {
                            Toast.makeText(context, "El GPS debe estar encendido para justificar el rechazo", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        viewModel.guardarRechazo {
                            showJustifySheet = false
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IosSystemRed)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Confirmar y Enviar Justificación", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
