package com.masterski.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.DownhillSkiing
import androidx.compose.material.icons.filled.Snowboarding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The faint figure drawn above the ridges on a screen. */
enum class BackdropFigure { Skier, Snowboarder, Snowflakes, None }

// Ridge outlines as (x, y) fractions of the backdrop band: far range first, near range second.
private val FarRidge = listOf(
    0f to .55f, .12f to .35f, .22f to .5f, .38f to .12f, .52f to .45f, .64f to .28f, .78f to .55f, .9f to .22f, 1f to .4f,
)
private val NearRidge = listOf(
    0f to .82f, .15f to .6f, .28f to .76f, .45f to .5f, .6f to .78f, .75f to .62f, .88f to .8f, 1f to .7f,
)

private fun DrawScope.ridge(points: List<Pair<Float, Float>>, color: Color) {
    val path = Path().apply {
        moveTo(0f, size.height)
        points.forEach { (x, y) -> lineTo(x * size.width, y * size.height) }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, color)
}

/**
 * A subtle alpine scene behind a screen: two mountain ridges along the bottom and an optional winter-sports
 * silhouette, tinted with the theme's primary colour at very low opacity. Purely decorative (hidden from
 * accessibility); [content] is drawn on top and should use transparent containers to let it show.
 */
@Composable
fun AlpineBackdrop(figure: BackdropFigure, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val tint = MaterialTheme.colorScheme.primary
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize().clearAndSetSemantics {}) {
            Canvas(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(170.dp)) {
                ridge(FarRidge, tint.copy(alpha = 0.05f))
                ridge(NearRidge, tint.copy(alpha = 0.09f))
            }
            Figure(figure, tint)
        }
        content()
    }
}

@Composable
private fun BoxScope.Figure(figure: BackdropFigure, tint: Color) {
    when (figure) {
        BackdropFigure.Skier -> Silhouette(Icons.Filled.DownhillSkiing, tint, 120.dp, Alignment.BottomEnd, x = (-28).dp, y = (-118).dp)
        BackdropFigure.Snowboarder -> Silhouette(Icons.Filled.Snowboarding, tint, 120.dp, Alignment.BottomStart, x = 24.dp, y = (-110).dp)
        BackdropFigure.Snowflakes -> {
            Silhouette(Icons.Filled.AcUnit, tint, 72.dp, Alignment.BottomEnd, x = (-36).dp, y = (-190).dp)
            Silhouette(Icons.Filled.AcUnit, tint, 40.dp, Alignment.BottomEnd, x = (-130).dp, y = (-280).dp)
            Silhouette(Icons.Filled.AcUnit, tint, 28.dp, Alignment.BottomStart, x = 48.dp, y = (-230).dp)
        }
        BackdropFigure.None -> Unit
    }
}

@Composable
private fun BoxScope.Silhouette(icon: ImageVector, tint: Color, size: Dp, alignment: Alignment, x: Dp, y: Dp) {
    Icon(
        icon,
        contentDescription = null,
        tint = tint.copy(alpha = 0.10f),
        modifier = Modifier.align(alignment).offset(x, y).size(size),
    )
}
