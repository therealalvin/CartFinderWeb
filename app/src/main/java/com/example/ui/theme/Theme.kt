package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF52B788),
    onPrimary = Color(0xFF081C15),
    primaryContainer = GolfGreenLight,
    onPrimaryContainer = Color(0xFFD8F3DC),
    secondary = BunkerGold,
    onSecondary = Color(0xFF332005),
    secondaryContainer = Color(0xFF533B1E),
    onSecondaryContainer = BunkerGoldLight,
    tertiary = FlagRed,
    onTertiary = Color.White,
    background = FairwayDarkBg,
    onBackground = Color(0xFFE8F1EC),
    surface = FairwayDarkSurface,
    onSurface = Color(0xFFE8F1EC),
    surfaceVariant = FairwayDarkCard,
    onSurfaceVariant = Color(0xFFBACBC2)
)

private val LightColorScheme = lightColorScheme(
    primary = GolfGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GolfGreenContainer,
    onPrimaryContainer = GolfOnGreenContainer,
    secondary = Color(0xFF995E2B),
    onSecondary = Color.White,
    secondaryContainer = BunkerGoldLight,
    onSecondaryContainer = Color(0xFF3B2207),
    tertiary = FlagRed,
    onTertiary = Color.White,
    background = FairwayLightBg,
    onBackground = Color(0xFF14241D),
    surface = FairwayLightSurface,
    onSurface = Color(0xFF14241D),
    surfaceVariant = FairwayLightCard,
    onSurfaceVariant = Color(0xFF405349)
)

@Composable
fun CartFinderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep branded golf look
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
