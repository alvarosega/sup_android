package com.alvarosega.trackingventas.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// PALETA OFICIAL APPLE PRO / iOS 18 LIQUID GLASSMORPHISM LUXURY
object IosColors {
    val BackgroundGrouped = Color(0xFFF1F5F9)
    val BackgroundMesh = Brush.verticalGradient(
        listOf(
            Color(0xFFE2E8F0),
            Color(0xFFEDF2F7),
            Color(0xFFE2E8F0)
        )
    )

    val CardSurface = Color(0xFFFFFFFF)
    val CardSurfaceGlass = Color(0xD9FFFFFF) // 85% Frosted Glass
    val BorderSeparator = Color(0xFFE2E8F0)
    val SpecularHighlight = Color(0x99FFFFFF)

    // Accent System Colors (Vibrant Metallic)
    val SystemBlue = Color(0xFF007AFF)
    val SystemBlueDark = Color(0xFF0051A8)
    val SystemBlueLight = Color(0xFFEBF3FF)

    val SystemGreen = Color(0xFF34C759)
    val SystemGreenDark = Color(0xFF248A3D)
    val SystemGreenLight = Color(0xFFEBFBF0)

    val SystemRed = Color(0xFFFF3B30)
    val SystemRedDark = Color(0xFFC72C23)
    val SystemRedLight = Color(0xFFFFF0F0)

    val SystemOrange = Color(0xFFFF9500)
    val SystemOrangeDark = Color(0xFFC77300)
    val SystemOrangeLight = Color(0xFFFFF7E6)

    val SystemPurple = Color(0xFF5856D6)
    val SystemPurpleDark = Color(0xFF3B38B3)
    val SystemPurpleLight = Color(0xFFF4F3FF)

    // Typography Labels
    val LabelPrimary = Color(0xFF0F172A)
    val LabelSecondary = Color(0xFF64748B)
    val LabelTertiary = Color(0xFF94A3B8)
    val FillQuaternary = Color(0xFFE2E8F0)

    // Glossy Glass Gradients
    val BlueGradient = Brush.verticalGradient(listOf(Color(0xFF0084FF), Color(0xFF0056B3)))
    val GreenGradient = Brush.verticalGradient(listOf(Color(0xFF38E067), Color(0xFF229E43)))
    val RedGradient = Brush.verticalGradient(listOf(Color(0xFFFF453A), Color(0xFFC72C23)))
    val OrangeGradient = Brush.verticalGradient(listOf(Color(0xFFFF9F0A), Color(0xFFC77300)))
    val PurpleGradient = Brush.verticalGradient(listOf(Color(0xFF6462E0), Color(0xFF3B38B3)))

    val GlassCardGradient = Brush.verticalGradient(
        listOf(
            Color(0xF0FFFFFF),
            Color(0xD8F8FAFC)
        )
    )

    val GlassBorderGradient = Brush.verticalGradient(
        listOf(
            Color(0x99FFFFFF),
            Color(0x33007AFF)
        )
    )
}

object IosDimensions {
    val CardCornerRadius = 22.dp
    val DialogCornerRadius = 26.dp
    val ButtonCornerRadius = 14.dp
    val BadgeCornerRadius = 10.dp
}

// Extensión para Tarjeta Liquid Glassmorphic Ultra-Premium
fun Modifier.glassCard(
    shape: Shape = RoundedCornerShape(22.dp),
    elevation: Dp = 6.dp,
    borderGradient: Brush = IosColors.GlassBorderGradient
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color(0x1A007AFF),
        spotColor = Color(0x12000000)
    )
    .clip(shape)
    .background(IosColors.GlassCardGradient)
    .border(1.2.dp, borderGradient, shape)
