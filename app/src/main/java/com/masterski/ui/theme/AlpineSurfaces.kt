package com.masterski.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.masterski.data.ThemeChoice

/** The theme in use, so components can switch between the Alpine Night look and plain Material (Classic). */
val LocalThemeChoice = staticCompositionLocalOf { ThemeChoice.ALPINE_NIGHT }

@Composable
@ReadOnlyComposable
fun isAlpineNight(): Boolean = LocalThemeChoice.current == ThemeChoice.ALPINE_NIGHT

/** Alpine Night palette extras that Material's colour scheme has no slot for. */
object AlpineNight {
    val SkyTop = Color(0xFF060A17)
    val SkyMid = Color(0xFF0A1229)
    val SkyBottom = Color(0xFF0C1530)
    val GlowBlue = Color(0xFF24367C)
    val GlowTeal = Color(0xFF0E4F5E)
    val TileTop = Color(0xFF16213F)
    val TileBottom = Color(0xFF111A33)
    /** Flat tile colour for list rows (the middle of the tile gradient). */
    val Tile = Color(0xFF141E3A)
    val NavBar = Color(0xFF080D1F)
    val Gold = Color(0xFFF2C879)
    val GoldDeep = Color(0xFFE8A73C)
    val Glacier = Color(0xFF8FD3FF)
}

private val TileShape = RoundedCornerShape(18.dp)

/** Shape of row [index] of [count] in a grouped list: the first and last rows round the group's corners. */
fun groupedShape(index: Int, count: Int): Shape {
    val r = 18.dp
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
 * A solid card: deep-navy gradient with a fine light top edge in Alpine Night; an elevated card in Classic.
 * Drop-in replacement for ElevatedCard.
 */
@Composable
fun AlpineTile(modifier: Modifier = Modifier, shape: Shape = TileShape, content: @Composable ColumnScope.() -> Unit) {
    if (!isAlpineNight()) {
        ElevatedCard(modifier, shape = shape, content = content)
        return
    }
    Column(
        modifier
            .shadow(10.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(AlpineNight.TileTop, AlpineNight.TileBottom)))
            .border(
                BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.05f)))),
                shape,
            ),
        content = content,
    )
}

/** The one frosted-glass panel (the season total). Falls back to a tile in Classic. */
@Composable
fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    if (!isAlpineNight()) {
        AlpineTile(modifier, shape, content)
        return
    }
    Column(
        modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape),
        content = content,
    )
}

/** Left-aligned screen title with an optional subtitle and trailing decoration; replaces the centred top bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(title, style = MaterialTheme.typography.headlineMedium)
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

/** The add button: solid gold in Alpine Night, the standard FAB in Classic. */
@Composable
fun AddButton(onClick: () -> Unit, contentDescription: String) {
    if (!isAlpineNight()) {
        FloatingActionButton(onClick = onClick) { Icon(Icons.Filled.Add, contentDescription = contentDescription) }
        return
    }
    FloatingActionButton(
        onClick = onClick,
        containerColor = AlpineNight.Gold,
        contentColor = Color(0xFF2A1D00),
        shape = RoundedCornerShape(18.dp),
    ) { Icon(Icons.Filled.Add, contentDescription = contentDescription, modifier = Modifier.size(28.dp)) }
}
