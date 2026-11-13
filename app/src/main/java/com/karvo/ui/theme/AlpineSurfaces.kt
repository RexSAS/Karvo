package com.karvo.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.karvo.data.ThemeChoice

/** The theme in use, so components can switch between the Alpine Night look and plain Material (Classic). */
val LocalThemeChoice = staticCompositionLocalOf { ThemeChoice.ALPINE_NIGHT }

/** Height of the floating tab bar on the three main pages: their lists scroll on under it and end this far up. */
val LocalTabBarSpace = staticCompositionLocalOf { 0.dp }

/**
 * Behind a screen's header: the top of the sky (the page colour in Classic), solid behind the title and fading out a
 * little below it, so lists scroll away under the header instead of being cut by a straight line.
 */
@Composable
fun Modifier.headerFade(): Modifier {
    val sky = if (isAlpineNight()) AlpineNight.SkyTop else MaterialTheme.colorScheme.background
    return drawBehind {
        val fall = 20.dp.toPx()
        drawRect(Brush.verticalGradient(0f to sky, 0.7f to sky, 1f to sky.copy(alpha = 0f), startY = 0f, endY = size.height + fall),
            size = Size(size.width, size.height + fall))
    }
}

/**
 * Padding for a list that scrolls under its header and the bars below it: the Scaffold's [padding], plus [around] on
 * every side, [extra] more at the bottom and the tab bar's [bottomBar].
 */
fun underBars(padding: PaddingValues, extra: Dp = 0.dp, bottomBar: Dp = 0.dp, around: Dp = 0.dp) = PaddingValues(
    start = around, end = around,
    top = padding.calculateTopPadding() + around,
    bottom = padding.calculateBottomPadding() + bottomBar + around + extra,
)

@Composable
@ReadOnlyComposable
fun isAlpineNight(): Boolean = LocalThemeChoice.current == ThemeChoice.ALPINE_NIGHT

/** Alpine Night palette extras that Material's colour scheme has no slot for. */
object AlpineNight {
    // The sky, top to horizon: night, blue hour, then the first warm light.
    val SkyTop = Color(0xFF060918)
    val SkyMid = Color(0xFF0D1538)
    val SkyBottom = Color(0xFF1F2760)
    val SkyHorizon = Color(0xFF3A3672)
    val SkyDawn = Color(0xFF5A4478)
    /** Alpenglow: the warm light low in the sky and on the far summits. Art only, never text. */
    val Alpenglow = Color(0xFFF08F6E)
    // Smoked night glass: the cards. Only just see-through: a hint of the ridges behind, never enough to disturb the text.
    val TileTop = Color(0xF0222E62)
    val TileBottom = Color(0xF5111940)
    /** Solid night glass for list rows and text fields (nothing shows through). */
    val Tile = Color(0xFF151E40)
    val NavBar = Color(0xF00D1330)
    /** The foot of the near ridge: the bottom of every screen. */
    val Ground = Color(0xFF0A1030)
    val Gold = Color(0xFFF4C06A)
    val GoldDeep = Color(0xFFE5A23C)
    val Glacier = Color(0xFF8CCBFF)
}

/** Corner of a card (a chart, a group of rows). */
val CardShape = RoundedCornerShape(24.dp)

/** Corner of a small tile (one number). */
val TileShape = RoundedCornerShape(18.dp)

/** Shape of row [index] of [count] in a grouped list: the first and last rows round the group's corners. */
fun groupedShape(index: Int, count: Int): Shape {
    val r = 24.dp
    return RoundedCornerShape(
        topStart = if (index == 0) r else 0.dp,
        topEnd = if (index == 0) r else 0.dp,
        bottomStart = if (index == count - 1) r else 0.dp,
        bottomEnd = if (index == count - 1) r else 0.dp,
    )
}

/** Background for grouped list rows: solid tile in Alpine Night, Material's low container in Classic. */
@Composable
@ReadOnlyComposable
fun tileColor(): Color = if (isAlpineNight()) AlpineNight.Tile else MaterialTheme.colorScheme.surfaceContainerLow

/**
 * A card: smoked night glass with a fine light edge in Alpine Night (a hint of the ridges stays behind it);
 * an elevated card in Classic. Drop-in replacement for ElevatedCard.
 */
@Composable
fun AlpineTile(modifier: Modifier = Modifier, shape: Shape = CardShape, content: @Composable ColumnScope.() -> Unit) {
    if (!isAlpineNight()) {
        ElevatedCard(modifier, shape = shape, content = content)
        return
    }
    Column(
        modifier
            .clip(shape) // no shadow: a shadow would show through the glass
            .background(Brush.verticalGradient(listOf(AlpineNight.TileTop, AlpineNight.TileBottom)))
            .border(
                BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.06f)))),
                shape,
            ),
        content = content,
    )
}

/** The season total. Alpine Night: straight on the sky, no box. Classic: a card. */
@Composable
fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    if (isAlpineNight()) Column(modifier, content = content) else AlpineTile(modifier, content = content)
}

/**
 * The thin gold line under the season total: the horizon at first light. Alpine Night only.
 * [glint] from 0 to 1 sends a bright spot along it once (when the earnings come out); 1 is at rest.
 */
@Composable
fun HorizonLine(modifier: Modifier = Modifier, glint: () -> Float = { 1f }) {
    if (!isAlpineNight()) return
    Box(modifier.fillMaxWidth().height(2.dp).drawBehind {
        val y = size.height / 2
        drawLine(Brush.horizontalGradient(listOf(AlpineNight.Gold, AlpineNight.Gold.copy(alpha = 0f))), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        val g = glint()
        if (g > 0f && g < 1f) {
            val x = g * size.width
            val half = 40.dp.toPx()
            drawLine(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFFFF0CF), Color.Transparent), startX = x - half, endX = x + half),
                Offset(x - half, y), Offset(x + half, y), 2.dp.toPx(), alpha = 1f - g)
        }
    })
}

/**
 * Left-aligned screen title with an optional subtitle, something right after the title ([afterTitle], like the logo)
 * and a trailing control in the corner; replaces the centred top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, afterTitle: (@Composable () -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .headerFade()
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, style = MaterialTheme.typography.headlineLarge)
                afterTitle?.invoke()
            }
        }
        trailing?.invoke()
    }
}

/** Text-field colours: a solid tile fill in Alpine Night so the sky's stars never show inside a field. */
@Composable
fun alpineFieldColors(): TextFieldColors =
    if (isAlpineNight()) {
        OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AlpineNight.Tile,
            unfocusedContainerColor = AlpineNight.Tile,
            disabledContainerColor = AlpineNight.Tile,
            errorContainerColor = AlpineNight.Tile,
        )
    } else {
        OutlinedTextFieldDefaults.colors()
    }

/** The add button: a gold disc with a warm glow in Alpine Night, the standard FAB in Classic. */
@Composable
fun AddButton(onClick: () -> Unit, contentDescription: String) {
    if (!isAlpineNight()) {
        FloatingActionButton(onClick = onClick) { Icon(Icons.Filled.Add, contentDescription = contentDescription) }
        return
    }
    FloatingActionButton(
        onClick = onClick,
        containerColor = AlpineNight.Gold,
        contentColor = Color(0xFF2B1C00),
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
        modifier = Modifier.shadow(16.dp, CircleShape, ambientColor = AlpineNight.Gold, spotColor = AlpineNight.Gold),
    ) { Icon(Icons.Filled.Add, contentDescription = contentDescription, modifier = Modifier.size(28.dp)) }
}
