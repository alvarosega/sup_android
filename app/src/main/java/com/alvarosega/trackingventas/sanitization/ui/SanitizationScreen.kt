package com.alvarosega.trackingventas.sanitization.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvarosega.trackingventas.sanitization.data.local.entity.ReferenceClientEntity
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanitizationScreen(
    viewModel: SanitizationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsState()
    val selectedClient by viewModel.selectedClient.collectAsState()
    val capturedPhotos by viewModel.capturedPhotoUris.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()

    var currentTempUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.onPhotoTakenSuccess()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = viewModel.createTempPictureUri()
            currentTempUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCameraFlow() {
        if (capturedPhotos.size >= 3) {
            Toast.makeText(context, "Límite máximo de 3 fotos alcanzado", Toast.LENGTH_SHORT).show()
            return
        }
        val hasCamPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCamPermission) {
            val uri = viewModel.createTempPictureUri()
            currentTempUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (selectedClient == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auditoría de Puntos",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { viewModel.syncDownMasterClients() }
                ) {
                    Text("Descargar")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (clients.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay clientes cargados. Presiona Descargar.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clients) { client ->
                        ClientItemRow(client = client, onClick = { viewModel.selectClient(client) })
                    }
                }
            }
        } else {
            AuditForm(
                client = selectedClient!!,
                photos = capturedPhotos,
                onAddPhoto = { launchCameraFlow() },
                onCancel = { viewModel.clearSelection() },
                onSave = { status, comments ->
                    viewModel.saveAudit(status, comments) {
                    }
                }
            )
        }
    }
}

@Composable
private fun ClientItemRow(
    client: ReferenceClientEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (client.isAudited) Color(0xFFF1F8E9) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cliente #${client.clientId}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = client.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
                Text(
                    text = "Ruta: ${client.route} | Día: ${client.day}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Text(
                text = if (client.isAudited) "AUDITADO" else "PENDIENTE",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (client.isAudited) Color(0xFF2E7D32) else Color(0xFFE65100)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuditForm(
    client: ReferenceClientEntity,
    photos: List<Uri>,
    onAddPhoto: () -> Unit,
    onCancel: () -> Unit,
    onSave: (status: String, comments: String) -> Unit
) {
    val statusOptions = listOf(
        "VALIDATED" to "Existe y Opera",
        "NOT_FOUND" to "No Existe / No Encontrado",
        "CLOSED_PERMANENT" to "Cerrado Definitivamente",
        "DUPLICATE" to "Punto Duplicado"
    )

    var expanded by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf(statusOptions[0]) }
    var comments by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Auditar: Cliente #${client.clientId}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = client.address,
            fontSize = 14.sp,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedStatus.second,
                onValueChange = {},
                readOnly = true,
                label = { Text("Estado del Punto") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                statusOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.second) },
                        onClick = {
                            selectedStatus = option
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = comments,
            onValueChange = { comments = it },
            label = { Text("Comentarios u Observaciones") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fotos Fachada (${photos.size}/3)",
                fontWeight = FontWeight.SemiBold
            )
            if (photos.size < 3) {
                Button(
                    onClick = onAddPhoto,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                ) {
                    Text("Tomar Foto")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(100.dp)
        ) {
            items(photos) { uri ->
                val file = uri.path?.let { File(it) }
                val bitmap = remember(uri) {
                    if (file != null && file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                    } else null
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Foto fachada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancelar")
            }
            Button(
                onClick = { onSave(selectedStatus.first, comments) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Guardar")
            }
        }
    }
}