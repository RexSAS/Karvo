package com.masterski.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.masterski.data.ThemeChoice

private val SkiBlue = Color(0xFF1565C0)

private val LightColors = lightColorScheme(primary = SkiBlue)
private val DarkColors = darkColorScheme(primary = SkiBlue)

/** Premium always-dark palette: navy night, glacier blue, gold for money and highlights. */
private val AlpineNightColors = darkColorScheme(
    background = Color(0xFF0B1220),
    surface = Color(0xFF0B1220),
    surfaceContainerLowest = Color(0xFF0E1626),
    surfaceContainerLow = Color(0xFF131C2E),
    surfaceContainer = Color(0xFF162034),
    surfaceContainerHigh = Color(0xFF1A253A),
    surfaceContainerHighest = Color(0xFF1C2840),
    surfaceVariant = Color(0xFF1C2840),
    primary = Color(0xFF7CC4FA),
    onPrimary = Color(0xFF04223A),
    primaryContainer = Color(0xFF1B3A5C),
    onPrimaryContainer = Color(0xFFCDE6FF),
    // Nav-bar indicator and chips use secondary; keep them in the glacier family, not Material purple.
    secondary = Color(0xFF9CC9F0),
    onSecondary = Color(0xFF04223A),
    secondaryContainer = Color(0xFF1B3A5C),
    onSecondaryContainer = Color(0xFFCDE6FF),
    tertiary = Color(0xFFE8B964),
    onTertiary = Color(0xFF3A2800),
    onBackground = Color(0xFFE6ECF5),
    onSurface = Color(0xFFE6ECF5),
    onSurfaceVariant = Color(0xFF9AA8BD),
    outline = Color(0xFF3A4A66),
    outlineVariant = Color(0xFF2A3852),
    error = Color(0xFFFF8A80),
)

private val BaseTypography = Typography()

/** Headline numbers are a touch bolder and tighter. */
private val MasterSkiTypography = BaseTypography.copy(
    displaySmall = BaseTypography.displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
)

@Composable
fun MasterSkiTheme(choice: ThemeChoice, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        choice == ThemeChoice.ALPINE_NIGHT -> AlpineNightColors
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, typography = MasterSkiTypography, content = content)
}
