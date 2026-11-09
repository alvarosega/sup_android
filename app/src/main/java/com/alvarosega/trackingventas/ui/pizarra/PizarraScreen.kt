package com.alvarosega.trackingventas.ui.pizarra

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvarosega.trackingventas.ui.theme.IosColors
import com.alvarosega.trackingventas.ui.theme.glassCard
import java.util.Locale

private val IosCardSurface = Color(0xFFFFFFFF)
private val IosBorderSeparator = Color(0xFFE5E5EA)
private val IosSystemBlue = Color(0xFF007AFF)
private val IosSystemGreen = Color(0xFF34C759)
private val IosSystemRed = Color(0xFFFF3B30)
private val IosSystemAmber = Color(0xFFB25E00)
private val IosSystemPurple = Color(0xFF5856D6)
private val IosLabelPrimary = Color(0xFF000000)
private val IosLabelSecondary = Color(0xFF1E293B)
private val IosFillQuaternary = Color(0xFFE5E5EA)

@Composable
fun PizarraScreen(
    viewModel: PizarraViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext

    val pizarraState by viewModel.pizarraState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()

    LaunchedEffect(errorMsg) {
        errorMsg?.let {
            Toast.makeText(appContext, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IosColors.BackgroundMesh)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Header Bar
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
                    Text(
                        text = "Inicio",
                        style = TextStyle(fontSize = 15.sp, color = IosSystemBlue, fontWeight = FontWeight.Normal)
                    )
                }

                Text(
                    text = "Pizarra Modelo Unilever",
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary)
                )

                IconButton(onClick = { viewModel.fetchPizarraResumen() }) {
                    if (isLoading) {
                        CircularProgressIndicator(color = IosSystemBlue, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = IosSystemBlue)
                    }
                }
            }

            if (isLoading && pizarraState == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = IosSystemBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Cargando Pizarra de Métricas & Scoring...", fontSize = 14.sp, color = IosLabelSecondary)
                    }
                }
            } else if (pizarraState != null) {
                val data = pizarraState!!
                val miRuta = data.miRuta
                val kpis = miRuta?.kpis

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {

                    // HERO SCORE CARD (MI RUTA)
                    if (miRuta != null) {
                        val score = miRuta.scoreTotal ?: 0
                        val isAprobado = miRuta.aprobado == true

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard(shape = RoundedCornerShape(22.dp), elevation = 6.dp)
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "MI RUTA DE VENTAS",
                                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${miRuta.ruta ?: "Ruta"} • ${miRuta.vendedor ?: "Vendedor"}",
                                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${data.diaSemana ?: ""} ${data.fecha ?: ""} • Canal ${miRuta.canal ?: "DSD"}",
                                        style = TextStyle(fontSize = 12.sp, color = IosLabelSecondary)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isAprobado) Color(0xFFE8F9ED) else Color(0xFFFFEBEA))
                                        .border(1.dp, if (isAprobado) IosSystemGreen else IosSystemRed, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$score / 80",
                                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isAprobado) IosSystemGreen else IosSystemRed)
                                        )
                                        Text(
                                            text = if (isAprobado) "APROBADO" else "EN REVISIÓN",
                                            style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isAprobado) IosSystemGreen else IosSystemRed)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(IosFillQuaternary)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ranking General de Rutas",
                                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = IosLabelPrimary)
                                )
                                Text(
                                    text = "#${miRuta.rank ?: 0} de ${miRuta.totalRutas ?: 0} Rutas",
                                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IosSystemBlue)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    // BREAKDOWN 8 KPIS DETALLADOS
                    if (kpis != null) {
                        Text(
                            text = "EVALUACIÓN DE KPIS DE DESEMPEÑO",
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // KPI 1: Horario de Inicio
                            kpis.kpi1Horario?.let { k ->
                                val hWeb = k.horaWeb?.ifBlank { null } ?: "Sin marcar"
                                val hMovil = k.horaMovil?.ifBlank { null } ?: "Sin marcar"
                                val isLate = (k.horaWeb != null && k.horaWeb > "08:10:00") || (k.horaMovil != null && k.horaMovil > "08:10:00") || k.cumple == false
                                KpiCard(
                                    nombre = k.nombre ?: "1. Horario de Inicio",
                                    puntos = k.puntos ?: 0,
                                    puntosMax = k.puntosMax ?: 10,
                                    cumple = k.cumple == true,
                                    highlightRed = isLate,
                                    detalle = "Hora Web: $hWeb   •   Hora App: $hMovil"
                                )
                            }

                            // KPI 2: Cobertura de Visitas
                            kpis.kpi2Visitas?.let { k ->
                                val visitados = k.totalVisitados ?: k.visitadosActivos ?: 0
                                KpiCard(
                                    nombre = k.nombre ?: "2. Cobertura de Visitas",
                                    puntos = k.puntos ?: 0,
                                    puntosMax = k.puntosMax ?: 5,
                                    cumple = k.cumple == true,
                                    detalle = "$visitados / ${k.carteraPlan ?: 0} clientes visitados (${String.format(Locale.US, "%.1f", k.coberturaPct ?: 0.0)}%)"
                                )
                            }

                            // KPI 3: Objetivo Diario
                            kpis.kpi3Objetivo?.let { k ->
                                KpiCard(
                                    nombre = k.nombre ?: "3. Objetivo Diario",
                                    puntos = k.puntos ?: 0,
                                    puntosMax = k.puntosMax ?: 10,
                                    cumple = k.cumple == true,
                                    detalle = "Bs. ${String.format(Locale.US, "%.2f", k.ventaHoy ?: 0.0)} / Bs. ${String.format(Locale.US, "%.2f", k.cuotaDiaria ?: 0.0)} (${String.format(Locale.US, "%.1f", k.alcancePct ?: 0.0)}%)"
                                )
                            }

                            // KPI 4: Efectividad
                            kpis.kpi4Efectividad?.let { k ->
                                KpiCard(
                                    nombre = k.nombre ?: "4. Efectividad de Venta",
                                    puntos = k.puntos ?: 0,
                                    puntosMax = k.puntosMax ?: 30,
                                    cumple = k.cumple == true,
                                    detalle = "Compradores: ${k.compradores ?: 0} / ${k.carteraPlan ?: 0} (${String.format(Locale.US, "%.1f", k.efectividadPct ?: 0.0)}% vs Meta ${String.format(Locale.US, "%.0f", k.umbralPct ?: 80.0)}%)"
                                )
                            }

                            // KPI 5: Compra Mínima
                            kpis.kpi5CompraMinima?.let { k ->
                                val sobreUmbral = k.compradoresSobreUmbral ?: k.compradores ?: 0
                                KpiCard(
                                    nombre = k.nombre ?: "5. Compra Mínima (Drop Size)",
                                    puntos = k.puntos ?: 0,
                                    puntosMax = k.puntosMax ?: 5,
                                    cumple = k.cumple == true,
                                    detalle = "Compradores ≥ Bs. ${String.format(Locale.US, "%.0f", k.montoMinimoEstandar ?: 70.0)}: $sobreUmbral"
                                )
                            }

                            // KPI 6: Reposición
                            kpis.kpi6Reposicion?.let { k ->
                                KpiCard(
                                    nombre = k.nombre ?: "6. Reposición (Canasta Habitual)",
                                    puntos = k.puntos ?: 0,
                                    puntosMax = k.puntosMax ?: 15,
                                    cumple = k.cumple == true,
                                    detalle = "SKUs Match: ${k.skusMatch ?: 0} / ${k.skusEsperados ?: 0} (${String.format(Locale.US, "%.1f", k.tasaPct ?: 0.0)}%)"
                                )
                            }

                            // KPI 7: Ampliación (Pendiente de Desarrollo)
                            kpis.kpi7Ampliacion?.let { k ->
                                val isPendiente = k.pendienteDesarrollo == true || k.estado == "PENDIENTE_DESARROLLO" || k.evaluado == false || k.puntosMax == 20
                                KpiCard(
                                    nombre = k.nombre ?: "7. Ampliación de Portafolio",
                                    puntos = k.puntos ?: 0,
                                    puntosMax = k.puntosMax ?: 20,
                                    cumple = k.cumple == true,
                                    isPendiente = isPendiente,
                                    detalle = if (isPendiente) {
                                        "KPI pendiente de desarrollo backend (evaluación actual sobre 80 pts)."
                                    } else {
                                        "Tasa: ${String.format(Locale.US, "%.1f", k.tasaPct ?: 0.0)}% (Ampliación: ${k.skusAmpliacion ?: 0} / Total ${k.totalSkus ?: 0})"
                                    }
                                )
                            }

                            // KPI 8: Covers
                            kpis.kpi8Covers?.let { k ->
                                KpiCard(
                                    nombre = k.nombre ?: "8. Covers / Descuentos",
                                    puntos = k.puntos ?: 0,
                                    puntosMax = k.puntosMax ?: 5,
                                    cumple = k.cumple == true,
                                    detalle = "Cumplen: ${k.cumplen ?: 0} / ${k.totalCompradores ?: 0} (${String.format(Locale.US, "%.1f", k.tasaPct ?: 0.0)}%)"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    // LEADERBOARD / RANKING DE RUTAS
                    if (data.ranking.isNotEmpty()) {
                        Text(
                            text = "TABLA DE RANKING DE RUTAS",
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IosLabelSecondary, letterSpacing = 0.5.sp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard(shape = RoundedCornerShape(18.dp), elevation = 4.dp)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            data.ranking.forEach { r ->
                                val isMe = r.esMiRuta == true
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isMe) Color(0xFFEBF3FF) else Color.Transparent)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "#${r.rank ?: 0}",
                                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isMe) IosSystemBlue else IosLabelPrimary)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "${r.ruta ?: "Ruta"} • ${r.vendedor ?: "Vendedor"}",
                                                style = TextStyle(fontSize = 13.sp, fontWeight = if (isMe) FontWeight.Bold else FontWeight.SemiBold, color = IosLabelPrimary)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${r.scoreTotal ?: 0} pts",
                                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (r.aprobado == true) IosSystemGreen else IosSystemRed)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    nombre: String,
    puntos: Int,
    puntosMax: Int,
    cumple: Boolean,
    isPendiente: Boolean = false,
    highlightRed: Boolean = false,
    detalle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(shape = RoundedCornerShape(14.dp), elevation = 3.dp)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = nombre,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IosLabelPrimary),
                modifier = Modifier.weight(1f)
            )

            if (isPendiente) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF5E5))
                        .border(1.dp, Color(0xFFB25E00), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PENDIENTE",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB25E00))
                    )
                }
            } else {
                val badgeBg = if (cumple && !highlightRed) Color(0xFFE8F9ED) else Color(0xFFFFEBEA)
                val badgeColor = if (cumple && !highlightRed) IosSystemGreen else IosSystemRed
                val borderCol = if (cumple && !highlightRed) IosSystemGreen else IosSystemRed

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .border(0.8.dp, borderCol, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$puntos / $puntosMax pts",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val detailColor = when {
            isPendiente -> Color(0xFFB25E00)
            highlightRed -> IosSystemRed
            else -> IosLabelPrimary
        }

        Text(
            text = detalle,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = detailColor
            )
        )
    }
}
