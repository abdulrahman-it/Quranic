package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPine,
    secondary = SecondaryBronze,
    tertiary = AccentEmerald,
    background = BgIvory,
    surface = SurfaceWhite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextObsidian,
    onSurface = TextObsidian
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),      // Soft spiritual green accent
    secondary = Color(0xFFE6C15C),    // Shimmering gold prestige accent
    tertiary = Color(0xFF34D399),     // Bright spiritual emerald
    background = Color(0xFF0F1512),   // Majestic deep forest-black background
    surface = Color(0xFF161F1A),      // Deep pine-gray container surface
    onPrimary = Color(0xFF0F1512),
    onSecondary = Color(0xFF0F1512),
    onBackground = Color(0xFFECE7DC), // Highly-readable warm cream text
    onSurface = Color(0xFFECE7DC)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
