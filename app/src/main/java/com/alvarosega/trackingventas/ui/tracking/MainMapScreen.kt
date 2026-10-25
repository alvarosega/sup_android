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
private val IosSystemAmber = Color(0xFFB25E00)
private val IosSystemPurple = Color(0xFF5856D6)
private val IosLabelPrimary = Color(0xFF1D1D1F)
private val IosLabelSecondary = Color(0xFF6E6E73)
private val IosFillQuaternary = Color(0xFFF2F2F7)

enum class StoreMarkerStatus {
    VISITED_OK,
    VISITED_SANEAMIENTO,
    PENDING_SANEAMIENTO,
    PENDING_REVIEW,
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
    val isOperationActive by viewModel.isOperationActive.collectAsState()
    val allClients by viewModel.allClients.collectAsState()
    val filteredClients by viewModel.filteredClients.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()
    val isRefreshingGps by viewModel.isRefreshingGps.collectAsState()

    val showOpportunities by viewModel.showOpportunities.collectAsState()
    val opportunities by viewModel.opportunities.collectAsState()
    val isLoadingOpportunities by viewModel.isLoadingOpportunities.collectAsState()

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
                    mapView.controller.setZoom(18.0)
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
        opportunities
    ) {
        mapView.overlays.clear()

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

            // Jerarquía Bicolor de Estados
            val markerStatus = when {
                client.hasPendingReview -> StoreMarkerStatus.PENDING_REVIEW
                client.isVisited && client.requiereCorreccion() -> StoreMarkerStatus.VISITED_SANEAMIENTO
                client.isVisited -> StoreMarkerStatus.VISITED_OK
                client.requiereCorreccion() -> StoreMarkerStatus.PENDING_SANEAMIENTO
                else -> StoreMarkerStatus.DEFAULT
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
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(10f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
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
                        onClick = { viewModel.syncPlanRuteo() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sincronizar",
                            tint = IosSystemBlue,
                            modifier = Modifier.size(20.dp)
                        )
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

                // Leyenda de Colores Flotante en Mapa (Top-Left)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 70.dp, start = 16.dp)
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
                                Text(
                                    text = "LEYENDA PINES",
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

                            // Azul: Pendiente OK
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(IosSystemBlue, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pendiente OK", fontSize = 11.sp, color = IosLabelPrimary, fontWeight = FontWeight.Medium)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Bicolor Azul/Naranja: Pendiente + Falta Saneamiento
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(modifier = Modifier.size(10.dp).clip(CircleShape)) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IosSystemBlue))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IosSystemAmber))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pendiente + Saneamiento ⚠️", fontSize = 10.sp, color = IosLabelPrimary, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Verde: Visitado OK
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(IosSystemGreen, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Visitado y Saneado ✓", fontSize = 11.sp, color = IosLabelPrimary, fontWeight = FontWeight.Medium)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Bicolor Verde/Naranja: Visitado + Falta Saneamiento
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(modifier = Modifier.size(10.dp).clip(CircleShape)) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IosSystemGreen))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IosSystemAmber))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Visitado + Saneamiento ⚠️", fontSize = 10.sp, color = IosLabelPrimary, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Púrpura: En Revisión
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(IosSystemPurple, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("En Revisión", fontSize = 11.sp, color = IosLabelPrimary, fontWeight = FontWeight.Medium)
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

                // Carrusel Deslizable Horizontal (HorizontalPager 90% Ancho / 10% Siguiente)
                if (clientsSortedByDistance.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { clientsSortedByDistance.size })

                    // Sincronizar el cliente seleccionado al deslizar en el pager
                    LaunchedEffect(pagerState.currentPage, clientsSortedByDistance) {
                        if (clientsSortedByDistance.isNotEmpty() && pagerState.currentPage in clientsSortedByDistance.indices) {
                            val activeClient = clientsSortedByDistance[pagerState.currentPage]
                            selectedClient = activeClient
                        }
                    }

                    // Sincronizar el pager cuando el usuario toca un pin en el mapa
                    LaunchedEffect(selectedClient) {
                        selectedClient?.let { sc ->
                            val targetIndex = clientsSortedByDistance.indexOf(sc)
                            if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
                                pagerState.animateScrollToPage(targetIndex)
                            }
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        pageSpacing = 12.dp,
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

                        val cardBorderColor = when {
                            client.isVisited -> IosSystemGreen
                            client.hasPendingReview -> IosSystemPurple
                            necesitaCorreccion || client.requiereSaneamiento -> IosSystemAmber
                            else -> IosSystemBlue
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(6.dp, RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp))
                                .background(IosCardSurface)
                                .border(1.8.dp, cardBorderColor, RoundedCornerShape(18.dp))
                                .padding(14.dp)
                                .animateContentSize()
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

                                    // Botón Lápiz de Edición con Parpadeo Rojo
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

                            // Caja de Alerta de Saneamiento si requiere corrección
                            val isSaneamientoRequerido = necesitaCorreccion || client.requiereSaneamiento || client.saneamientoObligatorio
                            if (isSaneamientoRequerido) {
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

        val isBicolor = (status == StoreMarkerStatus.PENDING_SANEAMIENTO || status == StoreMarkerStatus.VISITED_SANEAMIENTO)

        val leftColor = when (status) {
            StoreMarkerStatus.VISITED_OK, StoreMarkerStatus.VISITED_SANEAMIENTO -> 0xFF34C759.toInt() // Verde
            StoreMarkerStatus.PENDING_REVIEW -> 0xFF5856D6.toInt() // Púrpura
            else -> 0xFF007AFF.toInt() // Azul
        }

        val rightColor = when (status) {
            StoreMarkerStatus.PENDING_SANEAMIENTO, StoreMarkerStatus.VISITED_SANEAMIENTO -> 0xFFFF9500.toInt() // Naranja Saneamiento
            else -> leftColor
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