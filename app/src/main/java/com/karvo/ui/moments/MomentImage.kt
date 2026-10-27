package com.karvo.ui.moments

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.karvo.data.moments.loadMomentBitmap
import com.karvo.domain.Moment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Big media everywhere gets the same rounded corners. */
val MediaShape = RoundedCornerShape(20.dp)

/**
 * Loads [moment] off the main thread at up to [maxPx]; a plain tile shows meanwhile. With [ContentScale.Fit] and a
 * [fitShape], the shape clips the picture itself (sized to its own aspect), not the empty space around it.
 */
@Composable
fun MomentImage(
    moment: Moment,
    maxPx: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fitShape: Shape? = null,
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, moment.uri, maxPx) {
        value = withContext(Dispatchers.IO) { loadMomentBitmap(context, moment, maxPx)?.asImageBitmap() }
    }
    val image = bitmap
    when {
        image == null -> Box(modifier.background(if (fitShape != null) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant))
        fitShape != null && contentScale == ContentScale.Fit -> Box(modifier, contentAlignment = Alignment.Center) {
            Image(
                image,
                contentDescription = null,
                modifier = Modifier.aspectRatio(image.width.toFloat() / image.height).clip(fitShape),
                contentScale = ContentScale.Fit,
            )
        }
        else -> Image(image, contentDescription = null, modifier = modifier, contentScale = contentScale)
    }
}
