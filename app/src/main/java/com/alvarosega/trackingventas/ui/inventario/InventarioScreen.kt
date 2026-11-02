package com.alvarosega.trackingventas.ui.inventario

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Tokens de diseño estilo Apple iOS HIG
private val IosBackground = Color(0xFFF2F2F7)
private val IosCardSurface = Color(0xFFFFFFFF)
private val IosBorderSeparator = Color(0xFFC6C6C8)
private val IosSystemBlue = Color(0xFF007AFF)
private val IosSystemGreen = Color(0xFF34C759)
private val IosLabelPrimary = Color(0xFF000000)
private val IosLabelSecondary = Color(0xFF8E8E93)
private val IosFillQuaternary = Color(0xFFE5E5EA)

@Composable
fun InventarioScreen(
    viewModel: InventarioViewModel,
    clientId: Long,
    onNavigateBack: () -> Unit,
    onSavedSuccessfully: () -> Unit
) {
    val context = LocalContext.current

    val selectedClient by viewModel.selectedClient.collectAsState()
    val productosHistorial by viewModel.productosHistorial.collectAsState()
    val stockMap by viewModel.stockMap.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val isLoadingProductos by viewModel.isLoadingProductos.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()

    LaunchedEffect(clientId) {
        viewModel.initForClient(clientId)
    }

    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IosBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
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
                Text("Visita", style = TextStyle(fontSize = 16.sp, color = IosSystemBlue))
            }

            Text(
                text = "Inventario en PDV",
                style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IosLabelPrimary)
            )

            Box(modifier = Modifier.width(60.dp))
        }

        // CARGO O HEADER DE CLIENTE
        selectedClient?.let { client ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IosCardSurface)
                    .border(0.5.dp, IosBorderSeparator)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = IosSystemBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = client.clientName,
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "#${client.clientId} • ${client.address ?: "Sin dirección"}",
                    style = TextStyle(fontSize = 12.sp, color = IosLabelSecondary)
                )
            }
        }

        if (isLoadingProductos) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = IosSystemBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Cargando historial de compras del cliente...", fontSize = 14.sp, color = IosLabelSecondary)
                }
            }
        } else if (productosHistorial.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay productos registrados en el historial de compras de este cliente.",
                    style = TextStyle(fontSize = 14.sp, color = IosLabelSecondary),
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                itemsIndexed(productosHistorial) { idx, prod ->
                    val prodKey = viewModel.getProdKey(prod, idx)
                    val currentStockStr = stockMap[prodKey] ?: ""

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(IosCardSurface)
                            .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val fechaUltima = prod.ultimaCompra?.fecha?.ifBlank { null }
                                    ?: prod.historial?.firstOrNull()?.fecha?.ifBlank { null }
                                    ?: "Sin compra reciente"

                                Text(
                                    text = prod.producto ?: "Producto",
                                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Última compra: $fechaUltima",
                                    style = TextStyle(fontSize = 12.sp, color = IosLabelSecondary, fontWeight = FontWeight.Medium)
                                )
                            }

                            // Stepper de Conteo de Stock Físico Independiente
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.adjustStockFisico(prodKey, -1) },
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(IosFillQuaternary)
                                ) {
                                    Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = IosLabelPrimary)
                                }

                                OutlinedTextField(
                                    value = currentStockStr,
                                    onValueChange = { input ->
                                        viewModel.updateStockInput(prodKey, input)
                                    },
                                    singleLine = true,
                                    placeholder = { Text("Cant", fontSize = 12.sp, color = IosLabelSecondary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IosSystemGreen),
                                    modifier = Modifier.width(60.dp),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                IconButton(
                                    onClick = { viewModel.adjustStockFisico(prodKey, 1) },
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(IosFillQuaternary)
                                ) {
                                    Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = IosLabelPrimary)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "OBSERVACIONES (OPCIONAL)",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = comments,
                        onValueChange = { viewModel.setComments(it) },
                        label = { Text("Comentarios sobre el estado del estante...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = IosCardSurface,
                            unfocusedContainerColor = IosCardSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // BOTÓN DE CONFIRMACIÓN FIJO INFERIOR
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
                            Toast.makeText(context, "El GPS debe estar encendido para guardar el inventario", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        viewModel.guardarInventario {
                            Toast.makeText(context, "Inventario de PDV registrado con éxito", Toast.LENGTH_SHORT).show()
                            onSavedSuccessfully()
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IosSystemGreen)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Confirmar y Guardar Inventario en PDV", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
