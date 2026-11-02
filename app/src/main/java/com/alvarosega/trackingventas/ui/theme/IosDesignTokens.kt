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

// PALETA OFICIAL APPLE PRO / iOS 18 GLASSMORPHISM
object IosColors {
    val BackgroundGrouped = Color(0xFFF2F2F7)
    val CardSurface = Color(0xFFFFFFFF)
    val CardSurfaceGlass = Color(0xF5FFFFFF)
    val BorderSeparator = Color(0xFFE5E5EA)

    // Accent System Colors
    val SystemBlue = Color(0xFF007AFF)
    val SystemBlueDark = Color(0xFF0051A8)
    val SystemBlueLight = Color(0xFFE8F1FF)

    val SystemGreen = Color(0xFF34C759)
    val SystemGreenDark = Color(0xFF248A3D)
    val SystemGreenLight = Color(0xFFE8F9ED)

    val SystemRed = Color(0xFFFF3B30)
    val SystemRedDark = Color(0xFFC72C23)
    val SystemRedLight = Color(0xFFFFEBEA)

    val SystemOrange = Color(0xFFFF9500)
    val SystemOrangeDark = Color(0xFFC77300)
    val SystemOrangeLight = Color(0xFFFFF3CD)

    val SystemPurple = Color(0xFF5856D6)
    val SystemPurpleDark = Color(0xFF3B38B3)
    val SystemPurpleLight = Color(0xFFF3F0FF)

    // Typography Labels
    val LabelPrimary = Color(0xFF1D1D1F)
    val LabelSecondary = Color(0xFF8E8E93)
    val LabelTertiary = Color(0xFFC7C7CC)
    val FillQuaternary = Color(0xFFE5E5EA)

    // Gradients
    val BlueGradient = Brush.horizontalGradient(listOf(SystemBlue, SystemBlueDark))
    val GreenGradient = Brush.horizontalGradient(listOf(SystemGreen, SystemGreenDark))
    val RedGradient = Brush.horizontalGradient(listOf(SystemRed, SystemRedDark))
    val OrangeGradient = Brush.horizontalGradient(listOf(SystemOrange, SystemOrangeDark))
    val PurpleGradient = Brush.horizontalGradient(listOf(SystemPurple, SystemPurpleDark))
    val GlassCardGradient = Brush.verticalGradient(listOf(Color(0xF5FFFFFF), Color(0xEDF8F9FD)))
}

object IosDimensions {
    val CardCornerRadius = 18.dp
    val DialogCornerRadius = 22.dp
    val ButtonCornerRadius = 12.dp
    val BadgeCornerRadius = 8.dp
}

// Extensión para Tarjeta Glassmorphic Premium
fun Modifier.glassCard(
    shape: Shape = RoundedCornerShape(18.dp),
    elevation: Dp = 3.dp,
    borderColor: Color = IosColors.BorderSeparator
): Modifier = this
    .shadow(elevation = elevation, shape = shape, ambientColor = Color(0x0C000000), spotColor = Color(0x08000000))
    .clip(shape)
    .background(IosColors.GlassCardGradient)
    .border(0.8.dp, borderColor, shape)
