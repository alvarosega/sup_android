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
import androidx.compose.animation.core.*
import androidx.compose.animation.animateContentSize
import com.alvarosega.trackingventas.data.local.entity.getSaneamientoAlertas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alvarosega.trackingventas.data.local.entity.PlanRuteoEntity
import com.alvarosega.trackingventas.data.local.entity.requiereCorreccion
import com.alvarosega.trackingventas.ui.visita.VisitaViewModel
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
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
private val IosSystemAmber = Color(0xFFFF9500)
private val IosSystemPurple = Color(0xFF5856D6)
private val IosLabelPrimary = Color(0xFF1D1D1F)
private val IosLabelSecondary = Color(0xFF6E6E73)
private val IosFillQuaternary = Color(0xFFF2F2F7)

enum class StoreMarkerStatus {
    VISITED_OK,
    VISITED_SANEAMIENTO,
    VISITED_REVIEW,
    PENDING_SANEAMIENTO,
    PENDING_REVIEW,
    PENDING_OK,
    REFERENCIAL
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
    val mapMode by viewModel.mapMode.collectAsState()
    val showUnvisited by viewModel.showUnvisited.collectAsState()
    val routeBoundary by viewModel.routeBoundary.collectAsState()
    val activeDay by viewModel.activeDay.collectAsState()
    val isOperationActive by viewModel.isOperationActive.collectAsState()
    val allClients by viewModel.allClients.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()
    val isRefreshingGps by viewModel.isRefreshingGps.collectAsState()

    val showOpportunities by viewModel.showOpportunities.collectAsState()
    val opportunities by viewModel.opportunities.collectAsState()
    val isLoadingOpportunities by viewModel.isLoadingOpportunities.collectAsState()

    val baseClientsForMap = remember(allClients, mapMode, showUnvisited) {
        if (mapMode == MapMode.VISITAS) {
            if (showUnvisited) {
                allClients
            } else {
                allClients.filter { !isReferencialClient(it) }
            }
        } else {
            allClients
        }
    }

    val filteredClients = remember(baseClientsForMap, selectedFilter) {
        when (selectedFilter) {
            MapFilterType.TODOS -> baseClientsForMap
            MapFilterType.PENDIENTES -> baseClientsForMap.filter { !it.isVisited }
            MapFilterType.VISITADOS -> baseClientsForMap.filter { it.isVisited }
        }
    }

    var selectedClient by remember { mutableStateOf<PlanRuteoEntity?>(null) }
    var selectedClientsGroup by remember { mutableStateOf<List<PlanRuteoEntity>>(emptyList()) }
    var selectedClientIndex by remember { mutableIntStateOf(0) }
    var selectedClientDistance by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var hasCenteredOnInitialLocation by remember { mutableStateOf(false) }
    var showLegend by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulsePencil")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

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
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    viewModel.forceRefreshGps()
                }
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()
        viewModel.forceRefreshGps()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
        }
    }

    val clientsSortedByDistance = remember(filteredClients, latestLocation) {
        val loc = latestLocation
        if (loc != null) {
            filteredClients.sortedBy { client ->
                val res = FloatArray(1)
                Location.distanceBetween(loc.latitude, loc.longitude, client.latitude, client.longitude, res)
                res[0]
            }
        } else {
            filteredClients
        }
    }

    // Auto-seleccionar el cliente más cercano por defecto para que el carrusel esté SIEMPRE VISIBLE
    LaunchedEffect(clientsSortedByDistance) {
        if (selectedClient == null && clientsSortedByDistance.isNotEmpty()) {
            selectedClient = clientsSortedByDistance.first()
            selectedClientsGroup = clientsSortedByDistance
            selectedClientIndex = 0
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

    var bestGpsAccuracy by remember { mutableFloatStateOf(Float.MAX_VALUE) }

    LaunchedEffect(filteredClients) {
        if (!hasCenteredOnInitialLocation && filteredClients.isNotEmpty()) {
            val validClients = filteredClients.filter { it.latitude != 0.0 && it.longitude != 0.0 }
            if (validClients.isNotEmpty()) {
                val avgLat = validClients.map { it.latitude }.average()
                val avgLon = validClients.map { it.longitude }.average()
                mapView.controller.setCenter(GeoPoint(avgLat, avgLon))
                mapView.controller.setZoom(16.0)
            }
        }
    }

    LaunchedEffect(latestLocation) {
        latestLocation?.let { loc ->
            if (loc.latitude != 0.0 && loc.longitude != 0.0) {
                if (!hasCenteredOnInitialLocation || loc.accuracy < bestGpsAccuracy) {
                    bestGpsAccuracy = loc.accuracy
                    mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
                    mapView.controller.setZoom(16.0)
                    hasCenteredOnInitialLocation = true
                }
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
        opportunities,
        mapMode,
        routeBoundary
    ) {
        mapView.overlays.clear()

        // Dibujo de Geocerca / Polígono de Frontera de Ruta (Server o Cálculo Automático de Envolvente)
        val serverPoints = routeBoundary?.puntos?.filter { it.latitud != 0.0 && it.longitud != 0.0 }?.map { GeoPoint(it.latitud, it.longitud) } ?: emptyList()
        val polygonPoints = if (serverPoints.size >= 3) {
            serverPoints
        } else {
            val clientPoints = filteredClients.filter { it.latitude != 0.0 && it.longitude != 0.0 }.map { GeoPoint(it.latitude, it.longitude) }
            if (clientPoints.size >= 3) {
                calculateConvexHull(clientPoints)
            } else {
                emptyList()
            }
        }

        if (polygonPoints.size >= 3) {
            val boundaryPolygon = Polygon(mapView).apply {
                points = polygonPoints
                outlinePaint.color = 0xDC0071E3.toInt() // Borde azul brillante estilo Apple Pro
                outlinePaint.strokeWidth = 5f
                fillPaint.color = 0x1E0071E3.toInt() // Relleno azul traslúcido suave
                infoWindow = null
                setOnClickListener { _, _, _ -> false }
            }
            mapView.overlays.add(boundaryPolygon)
        }

        // Receptor de toque en fondo del mapa con amplio área táctil de 60m para tocar pines fácilmente
        val mapTouchOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p == null) return false
                val nearest = filteredClients.minByOrNull { client ->
                    val res = FloatArray(1)
                    Location.distanceBetween(p.latitude, p.longitude, client.latitude, client.longitude, res)
                    res[0]
                }
                if (nearest != null) {
                    val res = FloatArray(1)
                    Location.distanceBetween(p.latitude, p.longitude, nearest.latitude, nearest.longitude, res)
                    if (res[0] <= 60.0f) { // Amplia área táctil de 60m alrededor del pin
                        selectedClient = nearest
                        val idx = clientsSortedByDistance.indexOf(nearest)
                        if (idx >= 0) {
                            selectedClientIndex = idx
                            selectedClientsGroup = clientsSortedByDistance
                        }
                        mapView.controller.animateTo(GeoPoint(nearest.latitude, nearest.longitude))
                        return true
                    }
                }
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        })
        mapView.overlays.add(mapTouchOverlay)

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
                infoWindow = null
                setOnClickListener { _, _, _ -> false }
            }
            mapView.overlays.add(perimeter)
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

            // Jerarquía Bicolor de Estados (2 Dimensiones: Visita x Calidad de Datos)
            val isCensoMode = mapMode == MapMode.CENSO
            val isReferencial = isReferencialClient(client)

            val markerStatus = if (!isCensoMode) {
                if (isReferencial) {
                    StoreMarkerStatus.REFERENCIAL
                } else if (client.isVisited) {
                    StoreMarkerStatus.VISITED_OK
                } else {
                    StoreMarkerStatus.PENDING_OK
                }
            } else {
                when {
                    client.isVisited && client.hasPendingReview -> StoreMarkerStatus.VISITED_REVIEW
                    client.isVisited && client.requiereCorreccion() -> StoreMarkerStatus.VISITED_SANEAMIENTO
                    client.isVisited -> StoreMarkerStatus.VISITED_OK
                    client.hasPendingReview -> StoreMarkerStatus.PENDING_REVIEW
                    client.requiereCorreccion() -> StoreMarkerStatus.PENDING_SANEAMIENTO
                    else -> StoreMarkerStatus.PENDING_OK
                }
            }

            val marker = Marker(mapView).apply {
                position = clientPoint
                title = null
                snippet = null
                infoWindow = null
                icon = getStoreMarkerDrawable(
                    context,
                    status = markerStatus,
                    inRange = dist <= PROXIMITY_RADIUS_METERS
                )
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ ->
                    if (!viewModel.isGpsEnabled()) {
                        Toast.makeText(context, "El GPS está desactivado. Actívalo para interactuar con las visitas.", Toast.LENGTH_SHORT).show()
                        return@setOnMarkerClickListener true
                    }
                    if (!isOperationActive) {
                        Toast.makeText(context, "Debe iniciar jornada laboral para registrar visitas.", Toast.LENGTH_SHORT).show()
                        return@setOnMarkerClickListener true
                    }
                    val nearby = filteredClients.filter { other ->
                        val res = FloatArray(1)
                        Location.distanceBetween(client.latitude, client.longitude, other.latitude, other.longitude, res)
                        res[0] <= 30.0f
                    }
                    if (nearby.size > 1) {
                        selectedClientsGroup = nearby
                        selectedClientIndex = nearby.indexOf(client).coerceAtLeast(0)
                        selectedClient = nearby[selectedClientIndex]
                    } else {
                        selectedClientsGroup = listOf(client)
                        selectedClientIndex = 0
                        selectedClient = client
                    }
                    true
                }
            }
            mapView.overlays.add(marker)
        }

        // Marcador del Vendedor (DIBUJADO AL FINAL PARA ESTAR SIEMPRE EN LA CAPA SUPERIOR)
        latestLocation?.let { loc ->
            val userPoint = GeoPoint(loc.latitude, loc.longitude)
            val userMarker = Marker(mapView).apply {
                position = userPoint
                title = null
                snippet = null
                infoWindow = null
                icon = getUserMarkerDrawable(context)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setOnMarkerClickListener { _, _ -> true }
            }
            mapView.overlays.add(userMarker)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IosBackground)
    ) {
        // BARRA SUPERIOR SÓLIDA CON ELEVACIÓN Y Z-INDEX 10F SOBRE EL MAPA Y STATUS BAR
        Surface(
            color = IosCardSurface,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(10f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onBack() }
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = IosSystemBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Panel", style = TextStyle(fontSize = 16.sp, color = IosSystemBlue, fontWeight = FontWeight.Normal))
                    }

                    Text(
                        text = "Mapa de Visitas",
                        style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IosLabelPrimary)
                    )

                    IconButton(
                        onClick = {
                            viewModel.syncPlanRuteo()
                            viewModel.refreshAppModules()
                            viewModel.checkWorkdayStatus()
                            viewModel.forceRefreshGps()
                            Toast.makeText(context, "Sincronizando plan de ruta y permisos...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (isRefreshingGps) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = IosSystemBlue
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sincronizar",
                                tint = IosSystemBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Switch de Modo: Visitas vs Censo / Saneamiento
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(IosFillQuaternary)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val isVisitasMode = mapMode == MapMode.VISITAS

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .shadow(if (isVisitasMode) 1.dp else 0.dp, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isVisitasMode) IosCardSurface else Color.Transparent)
                            .clickable { viewModel.setMapMode(MapMode.VISITAS) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Modo Visitas",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = if (isVisitasMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (isVisitasMode) IosSystemBlue else IosLabelSecondary
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .shadow(if (!isVisitasMode) 1.dp else 0.dp, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isVisitasMode) IosCardSurface else Color.Transparent)
                            .clickable { viewModel.setMapMode(MapMode.CENSO) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Modo Censo / Saneamiento",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = if (!isVisitasMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isVisitasMode) IosSystemPurple else IosLabelSecondary
                            )
                        )
                    }
                }

                // Segmented Control de Filtro Integrado en Cabecera
                val isVisitasMode = mapMode == MapMode.VISITAS
                val baseClientsForCounts = if (isVisitasMode) {
                    allClients.filter { !isReferencialClient(it) }
                } else {
                    allClients
                }

                val totalCount = baseClientsForCounts.size
                val pendingCount = baseClientsForCounts.count { !it.isVisited }
                val visitedCount = baseClientsForCounts.count { it.isVisited }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(IosFillQuaternary)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val filters = listOf(
                        Triple(MapFilterType.TODOS, "Todos", totalCount),
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
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) IosLabelPrimary else IosLabelSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        // ÁREA DEL MAPA (RESTRINGIDA EN CAPA Z-INDEX 0F)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .zIndex(0f)
                .navigationBarsPadding()
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )

            // Alerta GPS Desactivado Banner
            if (!viewModel.isGpsEnabled()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IosSystemAmber)
                        .clickable { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "GPS Desactivado. Toca aquí para encender la ubicación.",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

                // Leyenda de Colores Flotante en Mapa (Top-Left)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 10.dp, start = 12.dp)
                ) {
                    if (showLegend) {
                        Column(
                            modifier = Modifier
                                .width(170.dp)
                                .shadow(3.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(IosCardSurface.copy(alpha = 0.95f))
                                .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isCensoMode = mapMode == MapMode.CENSO
                                Text(
                                    text = if (isCensoMode) "LEYENDA CENSO" else "LEYENDA VISITAS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IosLabelSecondary,
                                    letterSpacing = 0.5.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Ocultar Leyenda",
                                    tint = IosLabelSecondary,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { showLegend = false }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val isCensoMode = mapMode == MapMode.CENSO

                            if (!isCensoMode) {
                                // LEYENDA SIMPLIFICADA PARA MODO VISITAS
                                // 1. Azul: no visitado
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).background(IosSystemBlue, CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("no visitado", fontSize = 11.sp, color = IosLabelPrimary, fontWeight = FontWeight.Medium)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // 2. Verde: visitado
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).background(IosSystemGreen, CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("visitado", fontSize = 11.sp, color = IosLabelPrimary, fontWeight = FontWeight.Medium)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // 3. Gris: referencial (otro día)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).background(IosLabelSecondary, CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("referencial (otro día)", fontSize = 11.sp, color = IosLabelPrimary, fontWeight = FontWeight.Medium)
                                }
                            } else {
                                // LEYENDA COMPLETA BICOLOR PARA MODO CENSO / SANEAMIENTO
                                // 1. Azul: no visitado
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).background(IosSystemBlue, CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("no visitado", fontSize = 11.sp, color = IosLabelPrimary, fontWeight = FontWeight.Medium)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // 2. Bicolor Azul/Naranja: no visitado, datos incorrectos
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Row(modifier = Modifier.size(10.dp).clip(CircleShape)) {
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IosSystemBlue))
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IosSystemAmber))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("no visitado, datos incorrectos", fontSize = 10.sp, color = IosLabelPrimary, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // 3. Bicolor Azul/Morado: no visitado, validación de datos
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Row(modifier = Modifier.size(10.dp).clip(CircleShape)) {
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IosSystemBlue))
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IosSystemPurple))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("no visitado, validación de datos", fontSize = 10.sp, color = IosLabelPrimary, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // 4. Bicolor Verde/Naranja: visitado, datos incorrectos
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Row(modifier = Modifier.size(10.dp).clip(CircleShape)) {
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IosSystemGreen))
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IosSystemAmber))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("visitado, datos incorrectos", fontSize = 10.sp, color = IosLabelPrimary, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // 5. Bicolor Verde/Morado: visitado, validación de datos
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Row(modifier = Modifier.size(10.dp).clip(CircleShape)) {
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IosSystemGreen))
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IosSystemPurple))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("visitado, validación de datos", fontSize = 10.sp, color = IosLabelPrimary, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // 6. Verde: visitado, datos correctos
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).background(IosSystemGreen, CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("visitado, datos correctos", fontSize = 11.sp, color = IosLabelPrimary, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .shadow(2.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(IosCardSurface)
                                .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(20.dp))
                                .clickable { showLegend = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎨 Leyenda", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IosSystemBlue)
                        }
                    }
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
                                val currentLoc = latestLocation
                                if (currentLoc != null) {
                                    mapView.controller.animateTo(GeoPoint(currentLoc.latitude, currentLoc.longitude))
                                    mapView.controller.setZoom(18.5)
                                }
                                viewModel.forceRefreshGps { lat, lon ->
                                    mapView.controller.animateTo(GeoPoint(lat, lon))
                                    mapView.controller.setZoom(18.5)
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

                    // Toggle Clientes Referenciales (Unvisited)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(if (showUnvisited) IosSystemPurple else IosCardSurface)
                            .border(1.dp, if (showUnvisited) IosSystemPurple else IosBorderSeparator, CircleShape)
                            .clickable { viewModel.toggleShowUnvisited() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Ver / Ocultar Referenciales",
                            tint = if (showUnvisited) Color.White else IosLabelSecondary,
                            modifier = Modifier.size(20.dp)
                        )
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

                // Carrusel Deslizable Horizontal (HorizontalPager 90% Ancho / 10% Siguiente)
                if (clientsSortedByDistance.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { clientsSortedByDistance.size })

                    // Sincronización limpia cuando el pager se ASienta en una página (settledPage) para evitar brincos
                    LaunchedEffect(pagerState.settledPage, clientsSortedByDistance) {
                        if (clientsSortedByDistance.isNotEmpty() && pagerState.settledPage in clientsSortedByDistance.indices) {
                            val activeClient = clientsSortedByDistance[pagerState.settledPage]
                            if (selectedClient?.clientId != activeClient.clientId) {
                                selectedClient = activeClient
                                mapView.controller.animateTo(GeoPoint(activeClient.latitude, activeClient.longitude))
                            }
                        }
                    }

                    // Sincronizar el pager de forma instantánea al tocar un pin en el mapa
                    LaunchedEffect(selectedClient) {
                        selectedClient?.let { sc ->
                            val targetIndex = clientsSortedByDistance.indexOf(sc)
                            if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
                                pagerState.scrollToPage(targetIndex)
                            }
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(start = 10.dp, end = 36.dp),
                        pageSpacing = 10.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) { page ->
                        val client = clientsSortedByDistance[page]

                        val distToClient = if (latestLocation != null) {
                            val res = FloatArray(1)
                            Location.distanceBetween(
                                latestLocation!!.latitude,
                                latestLocation!!.longitude,
                                client.latitude,
                                client.longitude,
                                res
                            )
                            res[0].toDouble()
                        } else null

                        val isInRange = (distToClient ?: Float.MAX_VALUE.toDouble()) <= PROXIMITY_RADIUS_METERS.toDouble()
                        val necesitaCorreccion = client.requiereCorreccion()
                        val isCensoMode = mapMode == MapMode.CENSO

                        val cardBorderColor = if (!isCensoMode) {
                            if (client.isVisited) IosSystemGreen else IosSystemBlue
                        } else {
                            when {
                                client.isVisited -> IosSystemGreen
                                client.hasPendingReview -> IosSystemPurple
                                necesitaCorreccion || client.requiereSaneamiento -> IosSystemAmber
                                else -> IosSystemBlue
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(6.dp, RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp))
                                .background(IosCardSurface)
                                .border(1.8.dp, cardBorderColor, RoundedCornerShape(18.dp))
                                .padding(14.dp)
                        ) {
                            // Encabezado de Posición en el Carrusel Ordenado por Distancia
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CLIENTE ${page + 1} DE ${clientsSortedByDistance.size} • ${if (distToClient != null) "A ${distToClient.toInt()}m" else "CALCULANDO"}",
                                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = cardBorderColor, letterSpacing = 0.5.sp)
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isSaneamientoRequerido = necesitaCorreccion || client.requiereSaneamiento || client.saneamientoObligatorio
                                    val buttonBg = if (isSaneamientoRequerido) Color(0xFFFFEBEA).copy(alpha = pulseAlpha) else IosFillQuaternary
                                    val iconTint = if (isSaneamientoRequerido) IosSystemRed else IosSystemBlue

                                    if (isCensoMode) {
                                        // Botón Lápiz de Edición solo visible en Modo Censo
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(buttonBg)
                                                .border(if (isSaneamientoRequerido) 1.5.dp else 0.dp, IosSystemRed.copy(alpha = pulseAlpha), CircleShape)
                                            .clickable { onNavigateToEditarCliente(client) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = iconTint, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Nombre del Cliente y Dirección
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = client.clientName,
                                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "#${client.clientId} • ${client.tipoNegocio ?: "General"} • ${client.zona ?: "Sin Zona"}",
                                    style = TextStyle(fontSize = 12.sp, color = IosLabelSecondary)
                                )
                                Text(
                                    text = client.address ?: "Sin dirección registrada",
                                    style = TextStyle(fontSize = 12.sp, color = IosLabelSecondary)
                                )
                            }

                            // Caja de Alerta de Saneamiento solo en Modo Censo
                            val isSaneamientoRequerido = necesitaCorreccion || client.requiereSaneamiento || client.saneamientoObligatorio
                            if (isCensoMode && isSaneamientoRequerido) {
                                val alertas = client.getSaneamientoAlertas()
                                val alertaTexto = if (alertas.isNotEmpty()) alertas.joinToString(" • ") else "Datos incompletos o pendientes de corrección."
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFF5E5))
                                        .border(0.8.dp, Color(0xFFFFB74D), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = IosSystemAmber, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = alertaTexto,
                                            fontSize = 11.sp,
                                            color = Color(0xFFB25E00),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Botón de Acción Principal "Registrar Visita"
                            Button(
                                onClick = {
                                    if (!viewModel.isGpsEnabled()) {
                                        Toast.makeText(context, "El GPS está apagado. Debe activar la ubicación para registrar una visita.", Toast.LENGTH_LONG).show()
                                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                        return@Button
                                    }
                                    val loc = latestLocation
                                    if (loc == null) {
                                        Toast.makeText(context, "Esperando señal GPS...", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    visitaViewModel.startVisitaForClient(
                                        client = client,
                                        distanceMeters = (distToClient ?: Float.MAX_VALUE.toDouble()).toFloat(),
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
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
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
            }
        }
    }

private object MarkerDrawableCache {
    private var userBitmap: Bitmap? = null
    private var opportunityBitmap: Bitmap? = null
    private val storeBitmaps = HashMap<String, Bitmap>()

    fun getUserMarker(context: Context): Drawable {
        val bitmap = userBitmap ?: run {
            val size = 72
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            // Resplandor externo cian translúcido
            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x55007AFF
                style = Paint.Style.FILL
            }
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, haloPaint)

            // Anillo blanco
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(size / 2f, size / 2f, 22f, borderPaint)

            // Núcleo azul brillante de alta visibilidad
            val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF007AFF.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawCircle(size / 2f, size / 2f, 16f, corePaint)

            // Punto blanco central de referencia
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(size / 2f, size / 2f, 5f, dotPaint)

            userBitmap = bmp
            bmp
        }
        return BitmapDrawable(context.resources, bitmap)
    }

    fun getStoreMarker(context: Context, status: StoreMarkerStatus, inRange: Boolean): Drawable {
        val key = "$status-$inRange"
        val bitmap = storeBitmaps[key] ?: run {
            if (status == StoreMarkerStatus.REFERENCIAL) {
                val width = 42
                val height = 48
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)

                val grayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF8E8E93.toInt()
                    style = Paint.Style.FILL
                }
                val darkGrayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF6E6E73.toInt()
                    style = Paint.Style.FILL
                }
                val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    style = Paint.Style.FILL
                }

                // Techo / Toldo de Tienda Comercial Referencial
                val roofPath = android.graphics.Path().apply {
                    moveTo(4f, 16f)
                    lineTo(width / 2f, 4f)
                    lineTo(width - 4f, 16f)
                    close()
                }
                canvas.drawPath(roofPath, darkGrayPaint)

                // Fachada Principal de la Tienda
                canvas.drawRect(8f, 16f, width - 8f, height - 8f, grayPaint)

                // Puerta y Ventana en Blanco
                canvas.drawRect(width / 2f - 4f, height - 18f, width / 2f + 4f, height - 8f, whitePaint)
                canvas.drawRect(12f, 20f, 18f, 26f, whitePaint)

                storeBitmaps[key] = bmp
                bmp
            } else {
                val width = 48
                val height = 60
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)

                val isBicolor = (status == StoreMarkerStatus.PENDING_SANEAMIENTO ||
                        status == StoreMarkerStatus.VISITED_SANEAMIENTO ||
                        status == StoreMarkerStatus.PENDING_REVIEW ||
                        status == StoreMarkerStatus.VISITED_REVIEW)

                val leftColor = when (status) {
                    StoreMarkerStatus.VISITED_OK, StoreMarkerStatus.VISITED_SANEAMIENTO, StoreMarkerStatus.VISITED_REVIEW -> 0xFF34C759.toInt() // Verde (Visitado)
                    else -> 0xFF007AFF.toInt() // Azul (No Visitado)
                }

                val rightColor = when (status) {
                    StoreMarkerStatus.PENDING_SANEAMIENTO, StoreMarkerStatus.VISITED_SANEAMIENTO -> 0xFFFF9500.toInt() // Naranja (Datos Incorrectos)
                    StoreMarkerStatus.PENDING_REVIEW, StoreMarkerStatus.VISITED_REVIEW -> 0xFF5856D6.toInt() // Morado (Validación de Datos)
                    else -> leftColor // Unicolor
                }

                val path = android.graphics.Path().apply {
                    moveTo(width / 2f - 18f, 24f)
                    lineTo(width / 2f + 18f, 24f)
                    lineTo(width / 2f, height.toFloat())
                    close()
                }

                if (!isBicolor) {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = leftColor
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(width / 2f, 22f, 20f, paint)
                    canvas.drawPath(path, paint)
                } else {
                    val paintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = leftColor
                        style = Paint.Style.FILL
                    }
                    val paintRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = rightColor
                        style = Paint.Style.FILL
                    }

                    // Mitad Izquierda (Estado de Visita)
                    canvas.save()
                    canvas.clipRect(0f, 0f, width / 2f, height.toFloat())
                    canvas.drawCircle(width / 2f, 22f, 20f, paintLeft)
                    canvas.drawPath(path, paintLeft)
                    canvas.restore()

                    // Mitad Derecha (Naranja Saneamiento)
                    canvas.save()
                    canvas.clipRect(width / 2f, 0f, width.toFloat(), height.toFloat())
                    canvas.drawCircle(width / 2f, 22f, 20f, paintRight)
                    canvas.drawPath(path, paintRight)
                    canvas.restore()
                }

                // Círculo Central Blanco
                val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(width / 2f, 22f, 8f, centerPaint)

                storeBitmaps[key] = bmp
                bmp
            }
        }

        return BitmapDrawable(context.resources, bitmap)
    }

    fun getOpportunityMarker(context: Context): Drawable {
        val bitmap = opportunityBitmap ?: run {
            val width = 48
            val height = 60
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

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

            opportunityBitmap = bmp
            bmp
        }

        return BitmapDrawable(context.resources, bitmap)
    }
}

private fun calculateConvexHull(points: List<GeoPoint>): List<GeoPoint> {
    if (points.size < 3) return points
    val sorted = points.distinctBy { Pair(it.latitude, it.longitude) }.sortedWith(compareBy({ it.latitude }, { it.longitude }))
    if (sorted.size < 3) return sorted

    fun crossProduct(o: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
        return (a.longitude - o.longitude) * (b.latitude - o.latitude) - (a.latitude - o.latitude) * (b.longitude - o.longitude)
    }

    val lower = mutableListOf<GeoPoint>()
    for (p in sorted) {
        while (lower.size >= 2 && crossProduct(lower[lower.size - 2], lower.last(), p) <= 0) {
            lower.removeAt(lower.size - 1)
        }
        lower.add(p)
    }

    val upper = mutableListOf<GeoPoint>()
    for (p in sorted.reversed()) {
        while (upper.size >= 2 && crossProduct(upper[upper.size - 2], upper.last(), p) <= 0) {
            upper.removeAt(upper.size - 1)
        }
        upper.add(p)
    }

    lower.removeAt(lower.size - 1)
    upper.removeAt(upper.size - 1)
    return lower + upper
}

private fun isReferencialClient(client: PlanRuteoEntity): Boolean {
    return client.status.equals("unvisited", ignoreCase = true) ||
            client.status.equals("referencial", ignoreCase = true)
}

private fun getUserMarkerDrawable(context: Context): Drawable =
    MarkerDrawableCache.getUserMarker(context)

private fun getStoreMarkerDrawable(context: Context, status: StoreMarkerStatus, inRange: Boolean): Drawable =
    MarkerDrawableCache.getStoreMarker(context, status, inRange)

private fun getOpportunityMarkerDrawable(context: Context): Drawable =
    MarkerDrawableCache.getOpportunityMarker(context)