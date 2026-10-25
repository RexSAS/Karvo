package com.karvo.ui.moments

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.karvo.data.moments.ShareStyle
import com.karvo.data.moments.loadMomentBitmap
import com.karvo.data.moments.renderShareCard
import com.karvo.data.moments.saveShareCard
import com.karvo.domain.dayTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.karvo.KarvoApp
import com.karvo.R
import com.karvo.domain.Moment
import com.karvo.domain.momentsOn
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f
/** Black margin left and right of a photo or video, so it doesn't touch the screen edges. */
private val SIDE_MARGIN = 16.dp
/** Below this much room under the video, the controls sit over its bottom edge instead. */
private val MIN_CONTROLS_GAP = 56.dp

/**
 * A day's moments full screen, starting at [startIndex]: swipe between them, pinch or double-tap photos to zoom,
 * and play videos with sound, a mute button and a seek bar.
 */
@Composable
fun MomentViewerScreen(date: LocalDate, startIndex: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { (context.applicationContext as KarvoApp).container.momentStore }
    val zone = rememberZone()
    val moments = rememberMoments()?.let { momentsOn(date, it, zone) }
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    var designFor by remember { mutableStateOf<Moment?>(null) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (moments == null) return@Box // loading
        if (moments.isEmpty()) {
            LaunchedEffect(Unit) { onBack() }
            return@Box
        }
        val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, moments.lastIndex)) { moments.size }
        // Zoomed in, a drag pans the photo instead of turning the page.
        var zoomed by remember { mutableStateOf(false) }
        // Behind the share picker the viewer blurs (Android 12+; older phones just get the picker's dimming).
        val behind = Modifier.blur(if (designFor != null) 24.dp else 0.dp)
        HorizontalPager(pager, Modifier.fillMaxSize().then(behind), userScrollEnabled = !zoomed && designFor == null) { page ->
            val moment = moments[page]
            val current = pager.currentPage == page
            // Same key as the moment's card on the day page, so opening it grows the photo out of the card.
            Box(Modifier.fillMaxSize().sharedMoment("moment/${moment.uri}")) {
                if (moment.isVideo) VideoPage(moment, current, bottomBar = BOTTOM_BAR_SPACE) else ZoomablePhoto(moment, current, onZoomedChange = { if (current) zoomed = it })
            }
        }

        Row(Modifier.fillMaxWidth().then(behind).safeDrawingPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White) }
            Spacer(Modifier.weight(1f))
            if (moments.size > 1) {
                Text("${pager.currentPage + 1} / ${moments.size}", color = Color.White, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete), tint = Color.White) }
        }

        // Bottom: Share, outlined in Instagram's gradient as a nudge towards stories.
        ShareButton(
            primary = false,
            gradient = true,
            modifier = Modifier.align(Alignment.BottomCenter).then(behind).safeDrawingPadding().padding(bottom = 16.dp)
                .sharedMoment("share/${moments[pager.currentPage].uri}"),
        ) {
            val moment = moments[pager.currentPage]
            // Videos go out as they are; photos first pick a Karvo design.
            if (moment.isVideo) shareFile(context, store.shareUri(moment), "video/mp4") else designFor = moment
        }

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text(stringResource(R.string.delete_moment_question)) },
                text = { Text(stringResource(R.string.delete_moment_body)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = false
                        scope.launch { if (store.delete(moments[pager.currentPage])) onBack() }
                    }) { Text(stringResource(R.string.delete)) }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
            )
        }

        designFor?.let { SharePicker(it, date, onDismiss = { designFor = null }) }
    }
}

/**
 * Before a photo is shared: the viewer blurs and its Karvo designs fill the screen; swipe between them, then tap
 * the design or Share to send it.
 */
@Composable
internal fun SharePicker(moment: Moment, date: LocalDate, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateText = remember(date) { dayTitle(date) }
    var previews by remember { mutableStateOf<Map<ShareStyle, ImageBitmap>?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(moment.uri) {
        val photo = withContext(Dispatchers.IO) { loadMomentBitmap(context, moment, 1080) }
        if (photo == null) {
            // Can't draw the designs: share the photo as it is rather than not at all.
            shareFile(context, context.momentStore().shareUri(moment), "image/jpeg")
            onDismiss()
            return@LaunchedEffect
        }
        previews = withContext(Dispatchers.Default) {
            ShareStyle.entries.associateWith { renderShareCard(context, photo, it, dateText).asImageBitmap() }
        }
    }
    val styles = ShareStyle.entries
    val pager = rememberPagerState { styles.size }
    val canStory = rememberCanStory()
    fun share(style: ShareStyle, toStory: Boolean = false) {
        busy = true
        scope.launch {
            val uri = renderCard(context, moment, style, dateText, story = toStory)
            if (uri != null) {
                if (toStory) shareToStory(context, uri) else shareFile(context, uri, "image/jpeg")
            }
            onDismiss()
        }
    }
    BackHandler(onBack = onDismiss)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            // Taps outside the designs close the picker; the designs and buttons take their own taps first.
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        Row(Modifier.fillMaxWidth().safeDrawingPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, stringResource(R.string.close), tint = Color.White) }
            Text(stringResource(R.string.share_pick_title), color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        val shown = previews
        if (shown == null || busy) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            return@Box
        }
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(top = 64.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                pager,
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 48.dp),
                pageSpacing = 16.dp,
            ) { page ->
                val style = styles[page]
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        shown.getValue(style),
                        contentDescription = stringResource(style.label),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { share(style) },
                    )
                }
            }
            Text(
                stringResource(styles[pager.currentPage].label),
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(styles.size) { i ->
                    Box(Modifier.size(7.dp).background(Color.White.copy(alpha = if (i == pager.currentPage) 1f else 0.35f), CircleShape))
                }
            }
            // Stories first: they're what gets Karvo seen.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (canStory) StoryButton { share(styles[pager.currentPage], toStory = true) }
                ShareButton(primary = !canStory) { share(styles[pager.currentPage]) }
            }
        }
    }
}

private val ShareStyle.label
    get() = when (this) {
        ShareStyle.Banner -> R.string.share_style_banner
        ShareStyle.Frame -> R.string.share_style_frame
    }

private fun Context.momentStore() = (applicationContext as KarvoApp).container.momentStore

/** Room the viewer's Share button takes above the navigation bar. */
private val BOTTOM_BAR_SPACE = 84.dp

/**
 * Instagram's story button: an outline in its own gradient, with its glyph, so it reads as "Instagram" at a glance.
 * The gradient flows along the border to draw the eye.
 */
@Composable
private fun StoryButton(onClick: () -> Unit) {
    var width by remember { mutableFloatStateOf(1f) }
    val flow by rememberInfiniteTransition(label = "story").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(STORY_FLOW_MILLIS, easing = LinearEasing)),
        label = "flow",
    )
    // Mirrored, so a shift of two widths lands where it started and the loop is seamless. [from] moves the gradient's
    // origin, so the glyph and text (which start inside the button's padding) line up with the border's colours.
    val shift = flow * 2 * width
    fun gradient(from: Float = 0f) =
        Brush.linearGradient(InstagramColors, start = Offset(shift - from, 0f), end = Offset(shift + width - from, 0f), tileMode = TileMode.Mirror)
    val inset = with(LocalDensity.current) { BUTTON_PADDING.toPx() }
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        border = BorderStroke(2.dp, gradient()),
        contentPadding = PaddingValues(horizontal = BUTTON_PADDING),
        modifier = Modifier.heightIn(min = BUTTON_HEIGHT).onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) },
    ) {
        // Glyph and text painted with the same flowing gradient: drawn white, then the gradient is kept only where they are.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(gradient(inset), blendMode = BlendMode.SrcIn)
                },
        ) {
            Icon(painterResource(R.drawable.ic_instagram), contentDescription = null, modifier = Modifier.size(BUTTON_ICON))
            Spacer(Modifier.size(10.dp))
            Text(stringResource(R.string.share_instagram_story), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Share to any app: filled when it's the only button, otherwise outlined (white, over photos), in Instagram's gradient if [gradient]. */
@Composable
internal fun ShareButton(primary: Boolean, gradient: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val content: @Composable RowScope.() -> Unit = {
        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(BUTTON_ICON))
        Spacer(Modifier.size(10.dp))
        Text(stringResource(R.string.share), style = MaterialTheme.typography.titleMedium, fontWeight = if (gradient) FontWeight.SemiBold else null)
    }
    val big = modifier.heightIn(min = BUTTON_HEIGHT)
    val padding = PaddingValues(horizontal = BUTTON_PADDING)
    if (primary) {
        Button(onClick = onClick, modifier = big, contentPadding = padding, content = content)
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = big,
            contentPadding = padding,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = if (gradient) BorderStroke(2.dp, InstagramGradient) else ButtonDefaults.outlinedButtonBorder(enabled = true),
            content = content,
        )
    }
}

/** Whether Instagram is installed and can take a story. */
@Composable
private fun rememberCanStory(): Boolean {
    val context = LocalContext.current
    return remember { context.packageManager.resolveActivity(Intent(INSTAGRAM_STORY).setType("image/*"), 0) != null }
}

/** [moment] drawn full size in [style] (story-sized for a [story]), saved for sharing; null if the photo can't be read. */
private suspend fun renderCard(context: Context, moment: Moment, style: ShareStyle, date: String, story: Boolean): Uri? = withContext(Dispatchers.IO) {
    loadMomentBitmap(context, moment, 2048)?.let { saveShareCard(context, renderShareCard(context, it, style, date, story)) }
}

private const val INSTAGRAM_STORY = "com.instagram.share.ADD_TO_STORY"
private val InstagramColors = listOf(Color(0xFF833AB4), Color(0xFFE1306C), Color(0xFFF77737), Color(0xFFFCAF45))
private val InstagramGradient = Brush.linearGradient(InstagramColors)
/** The share buttons are larger than Material's defaults, to invite the tap. */
private val BUTTON_HEIGHT = 56.dp
private val BUTTON_PADDING = 28.dp
private val BUTTON_ICON = 22.dp
/** One full loop of the Story button's flowing border. */
private const val STORY_FLOW_MILLIS = 2400
/** Karvo's Facebook App ID: Instagram refuses story shares without one. Public, not a secret. */
private const val FACEBOOK_APP_ID = "1121968750229346"

/** Opens Instagram's story editor with the story-sized card at [uri] as its full-screen background. */
private fun shareToStory(context: Context, uri: Uri) {
    val story = Intent(INSTAGRAM_STORY).apply {
        putExtra("source_application", FACEBOOK_APP_ID)
        setDataAndType(uri, "image/jpeg")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    // Instagram can be uninstalled or updated between opening the picker and tapping; fall back to the share sheet.
    try {
        context.startActivity(story)
    } catch (_: android.content.ActivityNotFoundException) {
        shareFile(context, uri, "image/jpeg")
    }
}

/** Opens the share sheet for [uri]; the clip gives the sheet its thumbnail preview. */
internal fun shareFile(context: Context, uri: Uri, mime: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, null))
}

/** Pinch to zoom (1–5×), drag to pan while zoomed, double-tap to zoom in or back out; resets when swiped away. */
@Composable
private fun ZoomablePhoto(moment: Moment, current: Boolean, onZoomedChange: (Boolean) -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    fun clamp(o: Offset, s: Float): Offset {
        val maxX = size.width * (s - 1) / 2
        val maxY = size.height * (s - 1) / 2
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }
    LaunchedEffect(current) {
        if (!current) {
            scale = 1f
            offset = Offset.Zero
        }
    }
    val scope = rememberCoroutineScope()
    // Double-tap glides the zoom and position together instead of jumping.
    fun glideTo(targetScale: Float, targetOffset: Offset) {
        val startScale = scale
        val startOffset = offset
        scope.launch {
            animate(0f, 1f, animationSpec = tween(300, easing = FastOutSlowInEasing)) { t, _ ->
                scale = startScale + (targetScale - startScale) * t
                offset = startOffset + (targetOffset - startOffset) * t
            }
        }
    }
    // The zoom is read only while drawing and in gestures, so watch it directly rather than on recomposition.
    LaunchedEffect(current) {
        if (current) snapshotFlow { scale > 1f }.collect(onZoomedChange)
    }

    MomentImage(
        moment,
        maxPx = 2048,
        contentScale = ContentScale.Fit,
        fitShape = MediaShape,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SIDE_MARGIN)
            .pointerInput(Unit) { size = this.size }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        // One finger at 1× belongs to the pager; two fingers, or any drag while zoomed, belong to the photo.
                        if (event.changes.size > 1 || scale > 1f) {
                            val newScale = (scale * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                            offset = clamp(offset + event.calculatePan(), newScale)
                            scale = newScale
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { tap ->
                    if (scale > 1f) {
                        glideTo(1f, Offset.Zero)
                    } else {
                        // Zoom towards the tapped point.
                        glideTo(DOUBLE_TAP_ZOOM, clamp((Offset(size.width / 2f, size.height / 2f) - tap) * (DOUBLE_TAP_ZOOM - 1), DOUBLE_TAP_ZOOM))
                    }
                })
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}

/** Full-screen video with sound, looping while it's the page on screen, with a mute button and a thin seek bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoPage(moment: Moment, current: Boolean, bottomBar: Dp) {
    val controller = remember(moment.uri) { VideoController(muted = false) }
    SideEffect { controller.playing = current }
    // Bottom edge of the video on screen, and the page height, to centre the controls in the gap below the video.
    var mediaBottom by remember { mutableFloatStateOf(0f) }
    var pageHeight by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize().onSizeChanged { pageHeight = it.height }) {
        // Sized to the video's own shape so its corners can be rounded; the still frame shows until it's ready.
        val aspect = controller.aspect
        Box(Modifier.fillMaxSize().padding(horizontal = SIDE_MARGIN)) {
        if (aspect == 0f) MomentImage(moment, maxPx = 1080, contentScale = ContentScale.Fit, fitShape = MediaShape, modifier = Modifier.fillMaxSize())
        // One container for the player throughout, so it isn't rebuilt (and reloaded) once the shape is known;
        // until then it's a speck that only loads the video and reports its shape.
        Box(
            if (aspect > 0f) Modifier.align(Alignment.Center).aspectRatio(aspect).onGloballyPositioned { mediaBottom = it.boundsInParent().bottom }.clip(MediaShape)
            else Modifier.align(Alignment.Center).size(1.dp),
        ) {
            if (aspect > 0f) MomentImage(moment, maxPx = 1080, modifier = Modifier.fillMaxSize())
            VideoSurface(moment.uri, controller, crop = true, modifier = Modifier.fillMaxSize())
        }
        }
        // Halfway between the bottom of the video and the [bottomBar] (above the navigation bar); when the video leaves
        // too little room, just above the bar.
        val density = LocalDensity.current
        val reserved = bottomBar + with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
        val gap = with(density) { (pageHeight - mediaBottom).coerceAtLeast(0f).toDp() } - reserved
        val fitsBelow = mediaBottom > 0f && gap >= MIN_CONTROLS_GAP
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = reserved)
                .then(if (fitsBelow) Modifier.height(gap) else Modifier.padding(bottom = 8.dp)),
            contentAlignment = Alignment.Center,
        ) {
        // One slim pill: mute, elapsed time, a hairline seek bar and the length, all on one line.
        Row(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                .padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { controller.muted = !controller.muted }, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (controller.muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(if (controller.muted) R.string.unmute else R.string.mute),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            val duration = controller.durationMs
            val position = controller.positionMs.coerceIn(0, maxOf(duration, 0))
            Text(clock(position), color = Color.White, style = MaterialTheme.typography.labelMedium)
            Slider(
                value = position.toFloat(),
                onValueChange = { controller.seekTo(it.toInt()) },
                valueRange = 0f..maxOf(duration, 1).toFloat(),
                enabled = duration > 0,
                modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 12.dp),
                thumb = {}, // just the line: dragging anywhere on it still seeks
                track = { state ->
                    SliderDefaults.Track(
                        sliderState = state,
                        modifier = Modifier.height(2.dp),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                            disabledActiveTrackColor = Color.White.copy(alpha = 0.25f),
                            disabledInactiveTrackColor = Color.White.copy(alpha = 0.25f),
                        ),
                        thumbTrackGapSize = 0.dp,
                        drawStopIndicator = null,
                    )
                },
            )
            Text(clock(duration), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
        }
        }
    }
}

/** 4300 ms → "0:04". */
private fun clock(ms: Int): String {
    val seconds = (ms + 500) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
