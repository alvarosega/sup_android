package com.alvarosega.trackingventas.ui.tracking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alvarosega.trackingventas.data.local.entity.PlanRuteoEntity
import com.alvarosega.trackingventas.data.local.entity.requiereCorreccion
import com.alvarosega.trackingventas.ui.visita.VisitaViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

private const val PROXIMITY_RADIUS_METERS = 150.0f

// Tokens Oficiales Apple HIG / Apple Pro
private val IosBackground = Color(0xFFF2F2F7)
private val IosCardSurface = Color(0xFFFFFFFF)
private val IosBorderSeparator = Color(0xFFE5E5EA)
private val IosSystemBlue = Color(0xFF007AFF)
private val IosSystemGreen = Color(0xFF34C759)
private val IosSystemRed = Color(0xFFFF3B30)
private val IosSystemAmber = Color(0xFFB25E00)
private val IosSystemPurple = Color(0xFF5856D6)
private val IosLabelPrimary = Color(0xFF1D1D1F)
private val IosLabelSecondary = Color(0xFF6E6E73)
private val IosFillQuaternary = Color(0xFFF2F2F7)

enum class StoreMarkerStatus {
    VISITED,
    PENDING_REVIEW,
    REQUIRES_CORRECTION,
    DEFAULT
}

@Composable
fun MainMapScreen(
    viewModel: MainMapViewModel = hiltViewModel(),
    visitaViewModel: VisitaViewModel,
    onBack: () -> Unit,
    onNavigateToVisita: () -> Unit,
    onNavigateToAltaCliente: () -> Unit,
    onNavigateToEditarCliente: (PlanRuteoEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val latestLocation by viewModel.latestLocation.collectAsState()
    val allClients by viewModel.allClients.collectAsState()
    val filteredClients by viewModel.filteredClients.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()
    val isRefreshingGps by viewModel.isRefreshingGps.collectAsState()

    val showOpportunities by viewModel.showOpportunities.collectAsState()
    val opportunities by viewModel.opportunities.collectAsState()
    val isLoadingOpportunities by viewModel.isLoadingOpportunities.collectAsState()

    var selectedClient by remember { mutableStateOf<PlanRuteoEntity?>(null) }
    var selectedClientDistance by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var hasCenteredOnInitialLocation by remember { mutableStateOf(false) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setCenter(GeoPoint(-16.5000, -68.1500))
            controller.setZoom(16.0)
        }
    }

    BackHandler {
        if (selectedClient != null) {
            selectedClient = null
        } else {
            onBack()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(latestLocation, selectedClient) {
        val client = selectedClient
        val loc = latestLocation
        if (client != null && loc != null) {
            val results = FloatArray(1)
            Location.distanceBetween(loc.latitude, loc.longitude, client.latitude, client.longitude, results)
            selectedClientDistance = results[0]
        } else {
            selectedClientDistance = Float.MAX_VALUE
        }
    }

    LaunchedEffect(latestLocation) {
        latestLocation?.let { loc ->
            if (!hasCenteredOnInitialLocation) {
                mapView.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
                mapView.controller.setZoom(18.0)
                hasCenteredOnInitialLocation = true
            }
        }
    }

    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(
        latestLocation,
        filteredClients,
        selectedClient,
        showOpportunities,
        opportunities
    ) {
        mapView.overlays.clear()

        // Perímetro del cliente seleccionado
        selectedClient?.let { client ->
            val circlePoints = Polygon.pointsAsCircle(
                GeoPoint(client.latitude, client.longitude),
                PROXIMITY_RADIUS_METERS.toDouble()
            )
            val perimeter = Polygon(mapView).apply {
                points = circlePoints
                fillPaint.color = 0x22007AFF
                fillPaint.style = Paint.Style.FILL
                outlinePaint.color = 0xFF007AFF.toInt()
                outlinePaint.strokeWidth = 3f
            }
            mapView.overlays.add(perimeter)
        }

        // Marcador del Vendedor
        latestLocation?.let { loc ->
            val userPoint = GeoPoint(loc.latitude, loc.longitude)
            val userMarker = Marker(mapView).apply {
                position = userPoint
                title = null
                icon = getUserMarkerDrawable(context)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setOnMarkerClickListener { _, _ -> true }
            }
            mapView.overlays.add(userMarker)
        }

        // Marcadores de Clientes con jerarquía de 4 estados
        filteredClients.forEach { client ->
            val clientPoint = GeoPoint(client.latitude, client.longitude)
            val dist = if (latestLocation != null) {
                val res = FloatArray(1)
                Location.distanceBetween(
                    latestLocation!!.latitude,
                    latestLocation!!.longitude,
                    client.latitude,
                    client.longitude,
                    res
                )
                res[0]
            } else Float.MAX_VALUE

            // Jerarquía: Visitado -> Edición Pendiente -> Requiere Corrección -> Normal
            val markerStatus = when {
                client.isVisited -> StoreMarkerStatus.VISITED
                client.hasPendingReview -> StoreMarkerStatus.PENDING_REVIEW
                client.requiereCorreccion() -> StoreMarkerStatus.REQUIRES_CORRECTION
                else -> StoreMarkerStatus.DEFAULT
            }

            val marker = Marker(mapView).apply {
                position = clientPoint
                title = client.clientName
                icon = getStoreMarkerDrawable(
                    context,
                    status = markerStatus,
                    inRange = dist <= PROXIMITY_RADIUS_METERS
                )
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ ->
                    selectedClient = client
                    true
                }
            }
            mapView.overlays.add(marker)
        }

        // Oportunidades
        if (showOpportunities) {
            val oppOverlay = FolderOverlay()
            opportunities.forEach { opp ->
                val oppMarker = Marker(mapView).apply {
                    position = GeoPoint(opp.latitude, opp.longitude)
                    title = null
                    snippet = null
                    icon = getOpportunityMarkerDrawable(context)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ -> true }
                }
                oppOverlay.add(oppMarker)
            }
            mapView.overlays.add(oppOverlay)
        }

        mapView.invalidate()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ÁREA DEL MAPA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                )

                // Botón Volver
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 16.dp, start = 16.dp)
                        .size(44.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(IosCardSurface)
                        .border(1.dp, IosBorderSeparator, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = IosLabelPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Botones Superiores Derechos
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Refrescar GPS
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(IosCardSurface)
                            .border(1.dp, IosBorderSeparator, CircleShape)
                            .clickable {
                                viewModel.forceRefreshGps { lat, lon ->
                                    mapView.controller.animateTo(GeoPoint(lat, lon))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRefreshingGps) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = IosSystemBlue)
                        } else {
                            Icon(imageVector = Icons.Default.Place, contentDescription = "Actualizar GPS", tint = IosSystemBlue, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Toggle Oportunidades
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(if (showOpportunities) IosSystemPurple else IosCardSurface)
                            .border(1.dp, if (showOpportunities) IosSystemPurple else IosBorderSeparator, CircleShape)
                            .clickable { viewModel.toggleOpportunities() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingOpportunities) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Oportunidades",
                                tint = if (showOpportunities) Color.White else IosSystemPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Botón Alta Cliente
                if (selectedClient == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .shadow(3.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(IosSystemBlue)
                            .clickable { onNavigateToAltaCliente() }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Alta Cliente", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
                        }
                    }
                }

                // Tarjeta Inset de Cliente Seleccionado
                selectedClient?.let { client ->
                    val isInRange = selectedClientDistance <= PROXIMITY_RADIUS_METERS
                    val necesitaCorreccion = client.requiereCorreccion()

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(14.dp)
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .background(IosCardSurface)
                            .border(1.dp, IosBorderSeparator, RoundedCornerShape(10.dp))
                            .padding(16.dp)
                            .animateContentSize()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = client.clientName,
                                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "#${client.clientId} • ${client.tipoNegocio ?: "Sin tipo"} • ${client.zona ?: "Sin zona"}",
                                    style = TextStyle(fontSize = 12.sp, color = IosLabelSecondary)
                                )
                                Text(
                                    text = client.address ?: "Sin dirección",
                                    style = TextStyle(fontSize = 12.sp, color = IosLabelSecondary)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Botón Editar Cliente
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(IosFillQuaternary)
                                        .clickable { onNavigateToEditarCliente(client) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = IosSystemBlue, modifier = Modifier.size(16.dp))
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(IosFillQuaternary)
                                        .clickable { selectedClient = null },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = IosLabelSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Fila de Badges de Estado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rango de distancia
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (isInRange) IosSystemGreen else IosSystemRed, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedClientDistance == Float.MAX_VALUE) "Calculando..."
                                    else if (isInRange) "${selectedClientDistance.toInt()}m (En rango)"
                                    else "${selectedClientDistance.toInt()}m (Fuera de rango)",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isInRange) IosSystemGreen else IosSystemRed
                                    )
                                )
                            }

                            // Badges funcionales
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (client.hasPendingReview) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFF3F0FF))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("EN REVISIÓN", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IosSystemPurple))
                                    }
                                } else if (necesitaCorreccion) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFFF5E5))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("DATOS INCOMPLETOS", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IosSystemAmber))
                                    }
                                }

                                if (client.isVisited) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFEBF9EF))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("VISITADO", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IosSystemGreen))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val loc = latestLocation
                                if (loc == null || !viewModel.isGpsEnabled()) {
                                    Toast.makeText(context, "Esperando señal GPS...", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                visitaViewModel.startVisitaForClient(
                                    client = client,
                                    distanceMeters = selectedClientDistance,
                                    lat = loc.latitude,
                                    lon = loc.longitude,
                                    accuracy = loc.accuracy
                                )
                                onNavigateToVisita()
                            },
                            enabled = isInRange,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IosSystemGreen,
                                disabledContainerColor = IosFillQuaternary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text(
                                text = if (isInRange) "Registrar Visita" else "Acércate a menos de ${PROXIMITY_RADIUS_METERS.toInt()}m",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isInRange) Color.White else IosLabelSecondary
                                )
                            )
                        }
                    }
                }
            }

            // BARRA INFERIOR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IosCardSurface)
                    .border(0.5.dp, IosBorderSeparator)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FILTRAR CLIENTES",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showOpportunities) {
                            Text(
                                text = "Oportunidades: ${opportunities.size}",
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = IosSystemPurple)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sincronizar",
                            tint = IosSystemBlue,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { viewModel.syncPlanRuteo() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val pendingCount = allClients.count { !it.isVisited }
                val visitedCount = allClients.count { it.isVisited }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(IosFillQuaternary)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val filters = listOf(
                        Triple(MapFilterType.TODOS, "Todos", allClients.size),
                        Triple(MapFilterType.PENDIENTES, "Pendientes", pendingCount),
                        Triple(MapFilterType.VISITADOS, "Hechos", visitedCount)
                    )

                    filters.forEach { (type, label, count) ->
                        val isSelected = selectedFilter == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .shadow(if (isSelected) 1.dp else 0.dp, RoundedCornerShape(6.dp))
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) IosCardSurface else Color.Transparent)
                                .clickable { viewModel.setFilter(type) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$label ($count)",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) IosLabelPrimary else IosLabelSecondary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private object MarkerDrawableCache {
    private var userMarker: Drawable? = null
    private var opportunityMarker: Drawable? = null
    private val storeMarkers = HashMap<String, Drawable>()

    fun getUserMarker(context: Context): Drawable {
        userMarker?.let { return it }

        val resId = context.resources.getIdentifier("ic_marker_user", "drawable", context.packageName)
        if (resId != 0) {
            ContextCompat.getDrawable(context, resId)?.let {
                userMarker = it
                return it
            }
        }

        val size = 56
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33007AFF
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, haloPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, 16f, borderPaint)

        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF007AFF.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, 12f, corePaint)

        val drawable = BitmapDrawable(context.resources, bitmap)
        userMarker = drawable
        return drawable
    }

    fun getStoreMarker(context: Context, status: StoreMarkerStatus, inRange: Boolean): Drawable {
        val key = "$status-$inRange"
        storeMarkers[key]?.let { return it }

        val width = 48
        val height = 60
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Jerarquía de color exacta
        val pinColor = when (status) {
            StoreMarkerStatus.VISITED -> 0xFF007AFF.toInt()            // Azul
            StoreMarkerStatus.PENDING_REVIEW -> 0xFF5856D6.toInt()     // Púrpura
            StoreMarkerStatus.REQUIRES_CORRECTION -> 0xFFB25E00.toInt()// Ámbar
            StoreMarkerStatus.DEFAULT -> {
                if (inRange) 0xFFFF9500.toInt() else 0xFFFF3B30.toInt() // Naranja en rango / Rojo
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinColor
            style = Paint.Style.FILL
        }

        canvas.drawCircle(width / 2f, 22f, 20f, paint)

        val path = android.graphics.Path().apply {
            moveTo(width / 2f - 18f, 24f)
            lineTo(width / 2f + 18f, 24f)
            lineTo(width / 2f, height.toFloat())
            close()
        }
        canvas.drawPath(path, paint)

        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(width / 2f, 22f, 8f, centerPaint)

        val drawable = BitmapDrawable(context.resources, bitmap)
        storeMarkers[key] = drawable
        return drawable
    }

    fun getOpportunityMarker(context: Context): Drawable {
        opportunityMarker?.let { return it }

        val resId = context.resources.getIdentifier("ic_marker_opportunity", "drawable", context.packageName)
        if (resId != 0) {
            ContextCompat.getDrawable(context, resId)?.let {
                opportunityMarker = it
                return it
            }
        }

        val width = 48
        val height = 60
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val pinColor = 0xFFFFCC00.toInt()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinColor
            style = Paint.Style.FILL
        }

        canvas.drawCircle(width / 2f, 22f, 20f, paint)

        val path = android.graphics.Path().apply {
            moveTo(width / 2f - 18f, 24f)
            lineTo(width / 2f + 18f, 24f)
            lineTo(width / 2f, height.toFloat())
            close()
        }
        canvas.drawPath(path, paint)

        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(width / 2f, 22f, 8f, centerPaint)

        val drawable = BitmapDrawable(context.resources, bitmap)
        opportunityMarker = drawable
        return drawable
    }
}

private fun getUserMarkerDrawable(context: Context): Drawable =
    MarkerDrawableCache.getUserMarker(context)

private fun getStoreMarkerDrawable(context: Context, status: StoreMarkerStatus, inRange: Boolean): Drawable =
    MarkerDrawableCache.getStoreMarker(context, status, inRange)

private fun getOpportunityMarkerDrawable(context: Context): Drawable =
    MarkerDrawableCache.getOpportunityMarker(context)