package com.karvo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.karvo.R
import com.karvo.data.ThemeChoice

private val SkiBlue = Color(0xFF1565C0)

private val LightColors = lightColorScheme(primary = SkiBlue)
private val DarkColors = darkColorScheme(primary = SkiBlue)

/**
 * Alpine Night, "First Light": the night sky just before sunrise, glacier blue for actions, and gold as the first
 * light (money and highlights). Always dark. Text on every surface here is WCAG AA or better.
 */
private val AlpineNightColors = darkColorScheme(
    background = Color(0xFF080C1C), // Night
    surface = Color(0xFF080C1C),
    surfaceContainerLowest = Color(0xFF0A0F24),
    surfaceContainerLow = Color(0xFF0F1530),
    surfaceContainer = Color(0xFF121A3A),
    surfaceContainerHigh = Color(0xFF151E40), // Night glass: dialogs and sheets
    surfaceContainerHighest = Color(0xFF1A2450),
    surfaceVariant = Color(0xFF1A2450),
    primary = Color(0xFF8CCBFF), // Glacier
    onPrimary = Color(0xFF06203A),
    primaryContainer = Color(0xFF1B3566),
    onPrimaryContainer = Color(0xFFD3E9FF),
    // Chips and switches use secondary; keep them in the blue-hour family, not Material purple.
    secondary = Color(0xFFA8C7F0),
    onSecondary = Color(0xFF06203A),
    secondaryContainer = Color(0xFF22306A), // Blue hour
    onSecondaryContainer = Color(0xFFDCE8FF),
    tertiary = Color(0xFFF4C06A), // Summit gold
    onTertiary = Color(0xFF2B1C00),
    tertiaryContainer = Color(0xFF4A3512),
    onTertiaryContainer = Color(0xFFFFE3B3),
    onBackground = Color(0xFFF2F5FB), // Snow
    onSurface = Color(0xFFF2F5FB),
    onSurfaceVariant = Color(0xFFA9B4CC), // Mist
    outline = Color(0xFF5C6A9A),
    outlineVariant = Color(0xFF2A3462),
    error = Color(0xFFFF8A80),
    // Snackbars (e.g. Undo) use the inverse roles; keep them light-on-night with a glacier action.
    inverseSurface = Color(0xFFF2F5FB),
    inverseOnSurface = Color(0xFF080C1C),
    inversePrimary = Color(0xFF1E6CB3),
)

/**
 * Jost (OFL; one variable file, res/font/jost.ttf; licence in assets/licenses): titles, large numbers and the name
 * Karvo. Body text and labels stay in the system font. The web app uses the same font for the same things.
 */
@OptIn(ExperimentalTextApi::class) // choosing the weight of a variable font
private fun jost(weight: FontWeight) =
    Font(R.font.jost, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

val Jost = FontFamily(jost(FontWeight.Light), jost(FontWeight.Normal), jost(FontWeight.Medium), jost(FontWeight.SemiBold))

private val BaseTypography = Typography()

/** Display: large numbers, light. Headline large/small and title large: titles, medium. Headline medium: numbers, regular. */
private val KarvoTypography = BaseTypography.copy(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = Jost, fontWeight = FontWeight.Light, fontSize = 64.sp, lineHeight = 68.sp, letterSpacing = (-2).sp),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = Jost, fontWeight = FontWeight.Light, letterSpacing = (-1.2).sp),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = Jost, fontWeight = FontWeight.Light, letterSpacing = (-1).sp),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = Jost, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = Jost, fontWeight = FontWeight.Normal, letterSpacing = (-0.4).sp),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = Jost, fontWeight = FontWeight.Medium),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = Jost, fontWeight = FontWeight.Medium),
)

/** A small number in the display font, next to body text: a lesson's length, a month's hours. */
val Typography.rowNumber: TextStyle
    get() = titleMedium.copy(fontFamily = Jost, fontWeight = FontWeight.Medium, fontSize = 17.sp, letterSpacing = 0.sp)

/** A duration for large display ("142h 30m"): the digits full size, the units half size and in [unitColor]. */
fun bigDuration(text: String, unitColor: Color): AnnotatedString = buildAnnotatedString {
    text.forEach { c -> if (c.isLetter()) withStyle(SpanStyle(fontSize = .5.em, color = unitColor)) { append(c) } else append(c) }
}

@Composable
fun KarvoTheme(choice: ThemeChoice, content: @Composable () -> Unit) {
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
    CompositionLocalProvider(LocalThemeChoice provides choice) {
        MaterialTheme(colorScheme = colors, typography = KarvoTypography, content = content)
    }
}
