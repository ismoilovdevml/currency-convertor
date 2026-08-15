package com.currencyconverter.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Exact hex tokens from the design spec (Currency Converter v2). */
data class AppColors(
    val bg: Color,
    val surface: Color,
    val fg: Color,
    val muted: Color,
    val line: Color,
    val key: Color,
    val sheet: Color,
    val accent: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val accentText: Color,
    val scrim: Color,
    /** Unstarred favourite-toggle glyph color (README: star-empty). */
    val starEmpty: Color,
)

val LightAppColors = AppColors(
    bg = Color(0xFFF0F4F1),
    surface = Color(0xFFFFFFFF),
    fg = Color(0xFF0F1F18),
    muted = Color(0xFF6C7C74),
    line = Color(0xFFE2E8E3),
    key = Color(0xFFFFFFFF),
    sheet = Color(0xFFFFFFFF),
    accent = Color(0xFF10A56B),
    accentInk = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFE4F3EC),
    accentText = Color(0xFF0E7A52),
    scrim = Color(0x80081408), // rgba(8,20,15,0.5)
    starEmpty = Color(0xFFB4C0BA),
)

val DarkAppColors = AppColors(
    bg = Color(0xFF141A18),
    surface = Color(0xFF1D2523),
    fg = Color(0xFFE4EBE7),
    muted = Color(0xFF8C9C95),
    line = Color(0xFF2A3330),
    key = Color(0xFF212A27),
    sheet = Color(0xFF1A2220),
    accent = Color(0xFF10A56B),
    accentInk = Color(0xFF07271B),
    accentSoft = Color(0xFF1E3A2E),
    accentText = Color(0xFF5FD3A2),
    scrim = Color(0x80081408),
    starEmpty = Color(0xFF57635E),
)
