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
import androidx.compose.material.icons.filled.Snowboarding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.masterski.R
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

private fun DrawScope.ridge(points: List<Pair<Float, Float>>, brush: Brush) {
    val path = Path().apply {
        moveTo(0f, size.height)
        points.forEach { (x, y) -> lineTo(x * size.width, y * size.height) }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, brush)
}

/** Star positions as (x, y) fractions of the screen, with radius in dp and opacity; fixed so they never jump. */
private val Stars = listOf(
    Triple(.08f, .04f, .9f), Triple(.20f, .12f, .7f), Triple(.33f, .07f, .5f), Triple(.41f, .27f, .6f),
    Triple(.52f, .03f, .8f), Triple(.61f, .18f, .5f), Triple(.70f, .08f, .9f), Triple(.78f, .30f, .6f),
    Triple(.86f, .14f, 1f), Triple(.93f, .40f, .7f), Triple(.12f, .32f, .5f), Triple(.27f, .22f, .6f),
    Triple(.47f, .15f, .4f), Triple(.57f, .34f, .5f), Triple(.66f, .42f, .4f), Triple(.83f, .05f, .5f),
    Triple(.03f, .19f, .6f), Triple(.37f, .38f, .4f), Triple(.74f, .24f, .7f), Triple(.96f, .09f, .5f),
)

/** Alpine Night's sky: deep gradient, two soft glows and stars. */
private fun DrawScope.nightSky() {
    drawRect(Brush.verticalGradient(listOf(AlpineNight.SkyTop, AlpineNight.SkyMid, AlpineNight.SkyBottom)))
    drawRect(Brush.radialGradient(listOf(AlpineNight.GlowBlue, Color.Transparent), center = Offset(size.width * .85f, 0f), radius = size.width * 1.1f))
    drawRect(Brush.radialGradient(listOf(AlpineNight.GlowTeal.copy(alpha = .8f), Color.Transparent), center = Offset(0f, size.height * .08f), radius = size.width * .9f))
    Stars.forEachIndexed { i, (x, y, alpha) ->
        val r = if (i % 3 == 0) 1.6f else 1.1f
        drawCircle(Color.White.copy(alpha = alpha), radius = r * density, center = Offset(x * size.width, y * size.height))
    }
}

/** Snow-lit ridges for Alpine Night: a far range fading into the sky and a near range with lit summits. */
private fun DrawScope.nightRidges() {
    ridge(FarRidge, Brush.verticalGradient(listOf(Color(0x733D5AA8), Color.Transparent)))
    ridge(NearRidge, Brush.verticalGradient(0f to Color(0x47E9F0FF), .3f to Color(0x734F69B8), 1f to AlpineNight.SkyBottom))
}

/**
 * The scene behind a screen. Alpine Night: a starry night sky and snow-lit ridges. Classic: two
 * faint ridges in the theme colour. Both can add a faint winter-sports [figure]. Purely decorative (hidden from
 * accessibility); [content] is drawn on top and should use transparent containers to let it show.
 */
@Composable
fun AlpineBackdrop(figure: BackdropFigure, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val tint = MaterialTheme.colorScheme.primary
    val night = isAlpineNight()
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize().clearAndSetSemantics {}) {
            if (night) Canvas(Modifier.fillMaxSize()) { nightSky() }
            Canvas(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(if (night) 190.dp else 170.dp)) {
                if (night) {
                    nightRidges()
                } else {
                    ridge(FarRidge, SolidColor(tint.copy(alpha = 0.05f)))
                    ridge(NearRidge, SolidColor(tint.copy(alpha = 0.09f)))
                }
            }
            Figure(figure, if (night) AlpineNight.Glacier else tint)
        }
        content()
    }
}

@Composable
private fun BoxScope.Figure(figure: BackdropFigure, tint: Color) {
    when (figure) {
        BackdropFigure.Skier -> Silhouette(painterResource(R.drawable.art_carving_skier), tint, 150.dp, Alignment.BottomEnd, x = (-16).dp, y = (-112).dp)
        BackdropFigure.Snowboarder -> Silhouette(rememberVectorPainter(Icons.Filled.Snowboarding), tint, 120.dp, Alignment.BottomStart, x = 24.dp, y = (-110).dp)
        BackdropFigure.Snowflakes -> {
            Silhouette(rememberVectorPainter(Icons.Filled.AcUnit), tint, 72.dp, Alignment.BottomEnd, x = (-36).dp, y = (-190).dp)
            Silhouette(rememberVectorPainter(Icons.Filled.AcUnit), tint, 40.dp, Alignment.BottomEnd, x = (-130).dp, y = (-280).dp)
            Silhouette(rememberVectorPainter(Icons.Filled.AcUnit), tint, 28.dp, Alignment.BottomStart, x = 48.dp, y = (-230).dp)
        }
        BackdropFigure.None -> Unit
    }
}

@Composable
private fun BoxScope.Silhouette(art: Painter, tint: Color, size: Dp, alignment: Alignment, x: Dp, y: Dp) {
    Icon(
        art,
        contentDescription = null,
        tint = tint.copy(alpha = 0.10f),
        modifier = Modifier.align(alignment).offset(x, y).size(size),
    )
}
