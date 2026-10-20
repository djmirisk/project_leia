package com.solaresenergy.sisfv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = SolarPrimary,
    secondary = SolarSecondary,
    tertiary = SolarAccent,
    background = SolarBackground,
    surface = SolarSurface,
    error = SolarError,
    onPrimary = SolarSurface,
    onSecondary = SolarSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun SolaresSISFVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
