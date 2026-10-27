package com.karvo.ui.moments

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/** Play/pause, sound and position of one looping video; [VideoSurface] applies them to the player. */
class VideoController(muted: Boolean) {
    var playing by mutableStateOf(false)
    var muted by mutableStateOf(muted)
    var durationMs by mutableIntStateOf(0)
        internal set
    var positionMs by mutableIntStateOf(0)
        internal set
    /** Width / height as displayed; 0 until the video is ready. */
    var aspect by mutableFloatStateOf(0f)
        internal set

    internal var player: MediaPlayer? = null
    internal var prepared = false

    fun seekTo(ms: Int) {
        positionMs = ms
        player?.takeIf { prepared }?.seekTo(ms.toLong(), MediaPlayer.SEEK_CLOSEST)
    }

    internal fun apply() {
        val p = player?.takeIf { prepared } ?: return
        val volume = if (muted) 0f else 1f
        p.setVolume(volume, volume)
        if (playing && !p.isPlaying) p.start() else if (!playing && p.isPlaying) p.pause()
    }

    internal fun release() {
        player?.release()
        player = null
        prepared = false
    }
}

/** A muted video that loops while [playing], cropped to fill its bounds (the daily recap's cards). */
@Composable
fun LoopingVideo(uri: Uri, playing: Boolean, modifier: Modifier = Modifier) {
    val controller = remember(uri) { VideoController(muted = true) }
    SideEffect { controller.playing = playing }
    VideoSurface(uri, controller, crop = true, modifier = modifier)
}

/**
 * A looping video driven by [controller], cropped to fill ([crop]) or fitted inside its bounds. A TextureView (not a
 * SurfaceView) so it clips to rounded corners and scrolls with a pager; until the first frame it shows what's beneath.
 */
@Composable
fun VideoSurface(uri: Uri, controller: VideoController, crop: Boolean, modifier: Modifier = Modifier) {
    LaunchedEffect(controller, controller.playing, controller.muted) { controller.apply() }
    LaunchedEffect(controller, controller.playing) {
        while (controller.playing) {
            controller.player?.takeIf { controller.prepared }?.let { controller.positionMs = it.currentPosition }
            delay(100)
        }
    }
    AndroidView(
        factory = { context ->
            TextureView(context).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                        start(context, uri, Surface(texture), this@apply, controller, crop)
                    }

                    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) =
                        scale(this@apply, controller, crop)

                    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                        controller.release()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
                }
            }
        },
        onRelease = { controller.release() },
        modifier = modifier,
    )
}

private fun start(context: Context, uri: Uri, surface: Surface, view: TextureView, controller: VideoController, crop: Boolean) {
    controller.release()
    controller.player = MediaPlayer().apply {
        setDataSource(context, uri)
        setSurface(surface)
        isLooping = true
        setOnPreparedListener {
            controller.prepared = true
            controller.durationMs = it.duration
            if (it.videoHeight > 0) controller.aspect = it.videoWidth.toFloat() / it.videoHeight
            scale(view, controller, crop)
            controller.apply()
        }
        setOnVideoSizeChangedListener { _, w, h ->
            if (h > 0) controller.aspect = w.toFloat() / h
            scale(view, controller, crop)
        }
        setOnErrorListener { _, _, _ -> true } // a broken file just shows its still frame
        prepareAsync()
    }
}

/** Crops the video to fill the view (like ContentScale.Crop) or fits it inside (ContentScale.Fit). */
private fun scale(view: TextureView, controller: VideoController, crop: Boolean) {
    val p = controller.player?.takeIf { controller.prepared } ?: return
    if (p.videoWidth == 0 || view.width == 0 || view.height == 0) return
    val viewRatio = view.width.toFloat() / view.height
    val videoRatio = p.videoWidth.toFloat() / p.videoHeight
    val wider = videoRatio > viewRatio
    val (sx, sy) = when {
        crop && wider -> videoRatio / viewRatio to 1f
        crop -> 1f to viewRatio / videoRatio
        wider -> 1f to viewRatio / videoRatio
        else -> videoRatio / viewRatio to 1f
    }
    view.setTransform(Matrix().apply { setScale(sx, sy, view.width / 2f, view.height / 2f) })
}
