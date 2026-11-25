package com.sierracode.norte.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7DD3FC),
    secondary = Color(0xFFA7F3D0),
    background = Color(0xFF0E1218),
    surface = Color(0xFF161C24),
    onBackground = Color(0xFFE6EAF0),
    onSurface = Color(0xFFE6EAF0)
)

private val LightColors = lightColorScheme()

@Composable
fun NorteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}