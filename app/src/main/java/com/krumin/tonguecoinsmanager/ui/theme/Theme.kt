package com.krumin.tonguecoinsmanager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    secondary = GoldSecondary,
    tertiary = GoldTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = GoldDark,
    onSecondary = GoldLight,
    onTertiary = GoldLight,
    onBackground = GoldLight,
    onSurface = GoldLight
)

private val LightColorScheme = lightColorScheme(
    primary = GoldSecondary,
    secondary = GoldTertiary,
    tertiary = GoldPrimary,
    background = GoldLight,
    surface = androidx.compose.ui.graphics.Color.White,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onTertiary = GoldDark,
    onBackground = GoldDark,
    onSurface = GoldDark
)

@Composable
fun TongueCoinsManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set to false for a consistent golden brand look
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}