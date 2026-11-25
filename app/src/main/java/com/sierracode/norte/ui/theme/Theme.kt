package com.sierracode.norte.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val DarkColors = darkColorScheme(
    primary = GraphiteBluePrimaryDark,
    onPrimary = GraphiteBlueOnPrimaryDark,
    primaryContainer = GraphiteBlueContainerDark,
    onPrimaryContainer = GraphiteBlueOnContainerDark,
    secondary = SuccessGreenDark,
    onSecondary = SuccessGreenOnDark,
    secondaryContainer = SuccessGreenContainerDark,
    onSecondaryContainer = SuccessGreenOnContainerDark,
    tertiary = WarningAmberDark,
    onTertiary = WarningAmberOnDark,
    tertiaryContainer = WarningAmberContainerDark,
    onTertiaryContainer = WarningAmberOnContainerDark,
    error = ErrorRedDark,
    onError = ErrorRedOnDark,
    errorContainer = ErrorRedContainerDark,
    onErrorContainer = ErrorRedOnContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

private val LightColors = lightColorScheme(
    primary = GraphiteBluePrimaryLight,
    onPrimary = GraphiteBlueOnPrimaryLight,
    primaryContainer = GraphiteBlueContainerLight,
    onPrimaryContainer = GraphiteBlueOnContainerLight,
    secondary = SuccessGreenLight,
    onSecondary = SuccessGreenOnLight,
    secondaryContainer = SuccessGreenContainerLight,
    onSecondaryContainer = SuccessGreenOnContainerLight,
    tertiary = WarningAmberLight,
    onTertiary = WarningAmberOnLight,
    tertiaryContainer = WarningAmberContainerLight,
    onTertiaryContainer = WarningAmberOnContainerLight,
    error = ErrorRedLight,
    onError = ErrorRedOnLight,
    errorContainer = ErrorRedContainerLight,
    onErrorContainer = ErrorRedOnContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val LightExtendedColors = ExtendedColors(
    success = SuccessGreenLight,
    onSuccess = SuccessGreenOnLight,
    successContainer = SuccessGreenContainerLight,
    onSuccessContainer = SuccessGreenOnContainerLight,
    warning = WarningAmberLight,
    onWarning = WarningAmberOnLight,
    warningContainer = WarningAmberContainerLight,
    onWarningContainer = WarningAmberOnContainerLight
)

private val DarkExtendedColors = ExtendedColors(
    success = SuccessGreenDark,
    onSuccess = SuccessGreenOnDark,
    successContainer = SuccessGreenContainerDark,
    onSuccessContainer = SuccessGreenOnContainerDark,
    warning = WarningAmberDark,
    onWarning = WarningAmberOnDark,
    warningContainer = WarningAmberContainerDark,
    onWarningContainer = WarningAmberOnContainerDark
)

val ExtendedTheme: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

@Composable
fun NorteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}