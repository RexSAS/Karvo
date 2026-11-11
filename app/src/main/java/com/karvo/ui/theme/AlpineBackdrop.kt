package com.karvo.ui.theme

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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The faint figure drawn above the ridges on a screen. */
enum class BackdropFigure { Snowboarder, Snowflakes, None }

// Ridge outlines as (x as a fraction of the width, height above the bottom edge in dp): far, middle and near range.
private val FarRidge = listOf(
    0f to 195f, .078f to 220f, .144f to 202f, .244f to 252f, .311f to 224f, .389f to 268f, .489f to 214f,
    .567f to 240f, .656f to 208f, .75f to 260f, .833f to 224f, .917f to 246f, 1f to 220f,
)
private val MidRidge = listOf(
    0f to 148f, .1f to 182f, .194f to 152f, .306f to 198f, .417f to 146f, .517f to 176f, .628f to 130f, .728f to 170f, .861f to 126f, 1f to 168f,
)
private val NearRidge = listOf(
    0f to 90f, .122f to 118f, .267f to 80f, .417f to 130f, .572f to 74f, .728f to 108f, .867f to 68f, 1f to 96f,
)
/** The sunlit side of four far summits: summit, down the left slope, back in. */
private val Alpenglow = listOf(
    listOf(.244f to 252f, .183f to 224f, .233f to 232f),
    listOf(.389f to 268f, .328f to 235f, .378f to 245f),
    listOf(.75f to 260f, .689f to 228f, .739f to 238f),
    listOf(.078f to 220f, .033f to 202f, .069f to 208f),
)
/** Height of the band the ridges are drawn in. */
private val RidgeBand = 270.dp

private fun DrawScope.at(point: Pair<Float, Float>) = Offset(point.first * size.width, size.height - point.second.dp.toPx())

/** One range, moved [shift] pixels to the left. It is drawn a fifth wider on each side, so moving it never opens a gap. */
private fun DrawScope.ridge(points: List<Pair<Float, Float>>, brush: Brush, shift: Float = 0f) {
    val path = Path().apply {
        moveTo(-size.width * .2f, size.height)
        lineTo(-size.width * .2f, at(points.first()).y)
        points.forEach { at(it).let { p -> lineTo(p.x, p.y) } }
        lineTo(size.width * 1.2f, at(points.last()).y)
        lineTo(size.width * 1.2f, size.height)
        close()
    }
    translate(left = -shift) { drawPath(path, brush) }
}

/** A ridge's own light: [top] at its highest summit, [bottom] at the bottom edge. */
private fun DrawScope.ridgeLight(points: List<Pair<Float, Float>>, top: Color, bottom: Color) =
    Brush.verticalGradient(listOf(top, bottom), startY = size.height - points.maxOf { it.second }.dp.toPx(), endY = size.height)

/** Star positions as (x, y) fractions of the screen, with opacity; fixed so they never jump. All in the dark upper sky. */
private val Stars = listOf(
    Triple(.08f, .04f, .9f), Triple(.20f, .12f, .7f), Triple(.33f, .07f, .5f), Triple(.41f, .27f, .6f),
    Triple(.52f, .03f, .8f), Triple(.61f, .18f, .5f), Triple(.70f, .08f, .9f), Triple(.78f, .30f, .6f),
    Triple(.86f, .14f, 1f), Triple(.93f, .40f, .7f), Triple(.12f, .32f, .5f), Triple(.27f, .22f, .6f),
    Triple(.47f, .15f, .4f), Triple(.57f, .34f, .5f), Triple(.66f, .42f, .4f), Triple(.83f, .05f, .5f),
    Triple(.03f, .19f, .6f), Triple(.37f, .38f, .4f), Triple(.74f, .24f, .7f), Triple(.96f, .09f, .5f),
)

/** Alpine Night's sky, "first light": night at the top, blue hour, then the first warm light low on the left; stars above. */
private fun DrawScope.nightSky() {
    drawRect(Brush.verticalGradient(0f to AlpineNight.SkyTop, .42f to AlpineNight.SkyMid, .74f to AlpineNight.SkyBottom,
        .9f to AlpineNight.SkyHorizon, 1f to AlpineNight.SkyDawn))
    drawRect(Brush.radialGradient(listOf(AlpineNight.Alpenglow.copy(alpha = .34f), Color.Transparent),
        center = Offset(size.width * .25f, size.height * .8f), radius = size.height * .42f))
    Stars.forEachIndexed { i, (x, y, alpha) ->
        val r = if (i % 3 == 0) 1.6f else 1.1f
        drawCircle(Color.White.copy(alpha = alpha), radius = r * density, center = Offset(x * size.width, y * size.height))
    }
}

/** How far each range moves per page swiped, in dp: the nearer, the more (parallax). */
private const val FarDrift = 6f
private const val MidDrift = 12f
private const val NearDrift = 20f

/** Three ranges for Alpine Night, lighter the farther away; the far summits catch the alpenglow. [drift] is in pages. */
private fun DrawScope.nightRidges(drift: Float) {
    val far = (drift * FarDrift).dp.toPx()
    ridge(FarRidge, ridgeLight(FarRidge, Color(0xFF5D62AC), Color(0xFF2C3478)), far)
    translate(left = -far) {
        Alpenglow.forEach { glint ->
            drawPath(Path().apply { at(glint[0]).let { moveTo(it.x, it.y) }; glint.drop(1).forEach { p -> at(p).let { lineTo(it.x, it.y) } }; close() },
                Color(0xFFF6B58E), alpha = .62f)
        }
    }
    ridge(MidRidge, ridgeLight(MidRidge, Color(0xFF2B3579), Color(0xFF161E52)), (drift * MidDrift).dp.toPx())
    ridge(NearRidge, ridgeLight(NearRidge, Color(0xFF151D4C), AlpineNight.Ground), (drift * NearDrift).dp.toPx())
}

/**
 * The scene behind a screen. Alpine Night: the sky at first light over three ranges. Classic: two
 * faint ridges in the theme colour. Both can add a faint winter-sports [figure]. Purely decorative (hidden from
 * accessibility); [content] is drawn on top and should use transparent containers to let it show.
 * [drift] is how many pages the content has been swiped: the ranges slide a little with it, the near one most.
 */
@Composable
fun AlpineBackdrop(figure: BackdropFigure, modifier: Modifier = Modifier, drift: () -> Float = { 0f }, content: @Composable () -> Unit) {
    val tint = MaterialTheme.colorScheme.primary
    val night = isAlpineNight()
    val still = reducedMotion()
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize().clearAndSetSemantics {}) {
            if (night) Canvas(Modifier.fillMaxSize()) { nightSky() }
            Canvas(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(RidgeBand)) {
                val pages = if (still) 0f else drift()
                if (night) {
                    nightRidges(pages)
                } else {
                    ridge(MidRidge, SolidColor(tint.copy(alpha = 0.05f)), (pages * MidDrift).dp.toPx())
                    ridge(NearRidge, SolidColor(tint.copy(alpha = 0.09f)), (pages * NearDrift).dp.toPx())
                }
            }
            Figure(figure, if (night) AlpineNight.Glacier else tint)
        }
        content()
    }
}

/** Just the [figure], for content that slides over a shared backdrop (e.g. a pager page). */
@Composable
fun BackdropFigureLayer(figure: BackdropFigure) {
    val tint = if (isAlpineNight()) AlpineNight.Glacier else MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxSize().clearAndSetSemantics {}) { Figure(figure, tint) }
}

@Composable
private fun BoxScope.Figure(figure: BackdropFigure, tint: Color) {
    when (figure) {
        BackdropFigure.Snowboarder -> Silhouette(rememberVectorPainter(Icons.Filled.Snowboarding), tint, 120.dp, Alignment.BottomStart, x = 24.dp, y = (-200).dp)
        BackdropFigure.Snowflakes -> {
            Silhouette(rememberVectorPainter(Icons.Filled.AcUnit), tint, 72.dp, Alignment.BottomEnd, x = (-36).dp, y = (-280).dp)
            Silhouette(rememberVectorPainter(Icons.Filled.AcUnit), tint, 40.dp, Alignment.BottomEnd, x = (-130).dp, y = (-370).dp)
            Silhouette(rememberVectorPainter(Icons.Filled.AcUnit), tint, 28.dp, Alignment.BottomStart, x = 48.dp, y = (-320).dp)
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
