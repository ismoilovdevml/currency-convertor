package com.currencyconverter.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalAppColors = compositionLocalOf { LightAppColors }

object CurrencyConverterTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
}

@Composable
fun CurrencyConverterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = appColors.accent,
            onPrimary = appColors.accentInk,
            background = appColors.bg,
            surface = appColors.surface,
            onBackground = appColors.fg,
            onSurface = appColors.fg,
        )
    } else {
        lightColorScheme(
            primary = appColors.accent,
            onPrimary = appColors.accentInk,
            background = appColors.bg,
            surface = appColors.surface,
            onBackground = appColors.fg,
            onSurface = appColors.fg,
        )
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
            content = content,
        )
    }
}
