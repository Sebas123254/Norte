package com.sierracode.norte.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Marca - Azul Grafito Sobrio
val GraphiteBluePrimaryLight = Color(0xFF2C3E50)
val GraphiteBlueOnPrimaryLight = Color(0xFFFFFFFF)
val GraphiteBlueContainerLight = Color(0xFFD6E4F0)
val GraphiteBlueOnContainerLight = Color(0xFF162533)

val GraphiteBluePrimaryDark = Color(0xFF90B4CE)
val GraphiteBlueOnPrimaryDark = Color(0xFF102A43)
val GraphiteBlueContainerDark = Color(0xFF243B53)
val GraphiteBlueOnContainerDark = Color(0xFFD9E2EC)

// Éxito - Verde sobrio
val SuccessGreenLight = Color(0xFF2E7D32)
val SuccessGreenOnLight = Color(0xFFFFFFFF)
val SuccessGreenContainerLight = Color(0xFFC8E6C9)
val SuccessGreenOnContainerLight = Color(0xFF0A3912)

val SuccessGreenDark = Color(0xFF81C784)
val SuccessGreenOnDark = Color(0xFF00390E)
val SuccessGreenContainerDark = Color(0xFF1B5E20)
val SuccessGreenOnContainerDark = Color(0xFFA5D6A7)

// Error - Rojo sobrio
val ErrorRedLight = Color(0xFFBA1A1A)
val ErrorRedOnLight = Color(0xFFFFFFFF)
val ErrorRedContainerLight = Color(0xFFFFDAD6)
val ErrorRedOnContainerLight = Color(0xFF410002)

val ErrorRedDark = Color(0xFFFFB4AB)
val ErrorRedOnDark = Color(0xFF690005)
val ErrorRedContainerDark = Color(0xFF93000A)
val ErrorRedOnContainerDark = Color(0xFFFFDAD6)

// Advertencia - Ámbar sobrio
val WarningAmberLight = Color(0xFFB45309)
val WarningAmberOnLight = Color(0xFFFFFFFF)
val WarningAmberContainerLight = Color(0xFFFEF3C7)
val WarningAmberOnContainerLight = Color(0xFF78350F)

val WarningAmberDark = Color(0xFFFBBF24)
val WarningAmberOnDark = Color(0xFF451A03)
val WarningAmberContainerDark = Color(0xFF78350F)
val WarningAmberOnContainerDark = Color(0xFFFEF3C7)

// Superficies y Fondos Claro
val BackgroundLight = Color(0xFFF8FAFC)
val OnBackgroundLight = Color(0xFF0F172A)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF0F172A)
val SurfaceVariantLight = Color(0xFFF1F5F9)
val OnSurfaceVariantLight = Color(0xFF475569)
val OutlineLight = Color(0xFF94A3B8)

// Superficies y Fondos Oscuro
val BackgroundDark = Color(0xFF0E1218)
val OnBackgroundDark = Color(0xFFE2E8F0)
val SurfaceDark = Color(0xFF161C24)
val OnSurfaceDark = Color(0xFFE2E8F0)
val SurfaceVariantDark = Color(0xFF1E293B)
val OnSurfaceVariantDark = Color(0xFF94A3B8)
val OutlineDark = Color(0xFF64748B)

@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        success = Color.Unspecified,
        onSuccess = Color.Unspecified,
        successContainer = Color.Unspecified,
        onSuccessContainer = Color.Unspecified,
        warning = Color.Unspecified,
        onWarning = Color.Unspecified,
        warningContainer = Color.Unspecified,
        onWarningContainer = Color.Unspecified
    )
}

