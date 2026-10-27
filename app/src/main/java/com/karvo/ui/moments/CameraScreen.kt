package com.karvo.ui.moments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.karvo.KarvoApp
import com.karvo.R
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.HdrOff
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.extensions.ExtensionMode
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import com.karvo.data.moments.uprightCopy
import com.karvo.data.moments.applyPhotoLook
import androidx.camera.core.ImageProxy
import androidx.camera.video.QualitySelector
import androidx.camera.video.Quality
import androidx.camera.video.FallbackStrategy
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.camera.core.FocusMeteringAction
import java.time.LocalDate
import com.karvo.domain.momentsOn
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.PlayArrow
import com.karvo.domain.MAX_VIDEO_MILLIS
import com.karvo.domain.MOMENTS_PER_DAY
import com.karvo.domain.Moment
import com.karvo.ui.components.accentGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen camera for today's moment: tap the shutter for a photo, hold it for a video of up to 10 seconds.
 * A day keeps [MOMENTS_PER_DAY] moments; when it's full, the user first picks the one to replace. After a capture
 * the new moment is shown full screen and the camera closes by itself.
 */
@Composable
fun CameraScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val container = remember { (context.applicationContext as KarvoApp).container }
    val store = container.momentStore
    val scope = rememberCoroutineScope()

    fun granted(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    var hasCamera by remember { mutableStateOf(granted(Manifest.permission.CAMERA)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasCamera = granted(Manifest.permission.CAMERA)
    }
    fun requestPermissions() = permissionLauncher.launch(
        buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }.toTypedArray(),
    )
    // After two refusals Android stops showing the prompt.
    LaunchedEffect(Unit) {
        val legacyStorage = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !granted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (!hasCamera || legacyStorage || !granted(Manifest.permission.RECORD_AUDIO)) requestPermissions()
    }

    // Focus is handled by the tap layer below, which also draws the focus ring.
    val controller = remember {
        LifecycleCameraController(context).apply {
            isTapToFocusEnabled = false
            // The sensor's full 4:3 frame at its best normal resolution; the preview matches, so what you see is saved.
            val fourByThree = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .build()
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
            imageCaptureResolutionSelector = fourByThree
            previewResolutionSelector = fourByThree
            videoCaptureQualitySelector = QualitySelector.from(Quality.FHD, FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD))
        }
    }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var focusAt by remember { mutableStateOf<Offset?>(null) }
    val focusRing = remember { Animatable(0f) }
    LaunchedEffect(focusAt) {
        if (focusAt != null) {
            focusRing.snapTo(0f)
            focusRing.animateTo(1f, tween(250))
            delay(900)
            focusAt = null
        }
    }
    // The phone maker's own processing (Auto, HDR or Night), when the phone offers it to apps; null until known or if not.
    var extensions by remember { mutableStateOf<ExtensionsManager?>(null) }
    LaunchedEffect(Unit) {
        val provider = ProcessCameraProvider.getInstance(context)
        provider.addListener({
            val manager = ExtensionsManager.getInstanceAsync(context, provider.get())
            manager.addListener({ extensions = runCatching { manager.get() }.getOrNull() }, ContextCompat.getMainExecutor(context))
        }, ContextCompat.getMainExecutor(context))
    }
    var front by remember { mutableStateOf(false) }
    val baseSelector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
    // Auto or HDR suit every shot; some phones (Nothing, for one) only offer Night, which is slow for action.
    val hdrMode = extensions?.let { manager ->
        listOf(ExtensionMode.AUTO, ExtensionMode.HDR, ExtensionMode.NIGHT).firstOrNull { manager.isExtensionAvailable(baseSelector, it) }
    }
    val isNight = hdrMode == ExtensionMode.NIGHT
    // HDR starts on; Night starts off, so daytime action shots stay sharp.
    var hdrOn by remember(isNight) { mutableStateOf(!isNight) }
    // The phone can't run its HDR and video together, so holding for a video switches HDR off first.
    var videoMode by remember { mutableStateOf(false) }
    var videoPending by remember { mutableStateOf(false) }
    val hdrActive = hdrMode != null && hdrOn && !videoMode
    LaunchedEffect(hdrActive, baseSelector, hdrMode) {
        val manager = extensions
        if (hdrActive && manager != null && hdrMode != null) {
            controller.setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            controller.cameraSelector = manager.getExtensionEnabledCameraSelector(baseSelector, hdrMode)
        } else {
            controller.cameraSelector = baseSelector
            controller.setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE)
        }
    }
    DisposableEffect(hasCamera, lifecycleOwner) {
        if (hasCamera) controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }

    val zone = rememberZone()
    var today by remember { mutableStateOf<List<Moment>?>(null) }
    LaunchedEffect(zone) { today = momentsOn(LocalDate.now(zone), store.list(), zone) }
    // The moment the next capture replaces, once the user has picked it; deleted only after the new one is saved.
    var replacing by remember { mutableStateOf<Moment?>(null) }
    val todayMoments = today
    val choosing = todayMoments != null && replacing == null && todayMoments.size >= MOMENTS_PER_DAY
    var captured by remember { mutableStateOf<Moment?>(null) }
    // Set the moment the shutter fires: processing takes a moment, and quick extra taps must not shoot again.
    var busy by remember { mutableStateOf(false) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(captured) {
        if (captured != null) {
            // Done shooting: stop the live feed so nothing moves behind the preview.
            controller.unbind()
            reveal.animateTo(1f, tween(350))
            delay(1300)
            // Fade the preview out to black; leaving then crossfades back to the page underneath.
            reveal.animateTo(0f, tween(350))
            onClose()
        }
    }
    fun onSaved(moment: Moment) {
        store.scan(moment.uri)
        replacing?.let { old -> scope.launch { store.delete(old) } }
        captured = moment
    }

    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (message != null) {
            delay(2500)
            message = null
        }
    }
    // Shooting before the preview runs gives black photos, so the shutter waits for it.
    var streaming by remember { mutableStateOf(false) }
    val flash = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    val saved = stringResource(R.string.moment_saved)
    fun failed(reason: String?) = context.getString(R.string.moment_failed, reason ?: "?")

    fun takePhoto() {
        if (!streaming || choosing || busy || captured != null || recording != null) return
        busy = true
        scope.launch {
            flash.snapTo(0.8f)
            flash.animateTo(0f, tween(250))
        }
        controller.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    scope.launch {
                        // NonCancellable: closing the camera mid-save must not lose the photo.
                        val uri = try {
                            withContext(NonCancellable + Dispatchers.Default) {
                                val photo = image.use { uprightCopy(it.toBitmap(), it.imageInfo.rotationDegrees) }
                                try {
                                    applyPhotoLook(photo)
                                } catch (_: OutOfMemoryError) {
                                    // Low memory: keep the photo as the camera took it rather than lose it.
                                }
                                store.savePhoto(photo)
                            }
                        } catch (e: Exception) {
                            message = failed(e.message)
                            busy = false
                            return@launch
                        }
                        onSaved(Moment(uri, isVideo = false, takenAt = java.time.Instant.now()))
                    }
                }

                override fun onError(e: ImageCaptureException) {
                    message = failed(e.message)
                    busy = false
                }
            },
        )
    }

    @SuppressLint("MissingPermission") // audio is only enabled after checking the permission
    fun startVideo() {
        if (!streaming || choosing || busy || captured != null || recording != null) return
        val audio = AudioConfig.create(granted(Manifest.permission.RECORD_AUDIO))
        val executor = ContextCompat.getMainExecutor(context)
        val listener = androidx.core.util.Consumer<VideoRecordEvent> { event ->
            if (event is VideoRecordEvent.Finalize) {
                recording = null
                scope.launch { progress.snapTo(0f) }
                // Hitting the 10-second limit still saves the clip.
                if (!event.hasError() || event.error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED) {
                    scope.launch { flash.snapTo(0.8f); flash.animateTo(0f, tween(250)) }
                    onSaved(Moment(event.outputResults.outputUri, isVideo = true, takenAt = java.time.Instant.now()))
                } else {
                    message = failed(event.cause?.message)
                    busy = false
                }
            }
        }
        recording = when (val output = store.videoOutput()) {
            is MediaStoreOutputOptions -> controller.startRecording(output, audio, executor, listener)
            is FileOutputOptions -> controller.startRecording(output, audio, executor, listener)
            else -> null
        }
        if (recording != null) {
            busy = true
            scope.launch { progress.animateTo(1f, tween(MAX_VIDEO_MILLIS.toInt(), easing = LinearEasing)) }
        }
    }

    LaunchedEffect(videoPending) {
        if (videoPending) {
            // Let the camera rebind without HDR before recording.
            delay(VIDEO_SWITCH_MILLIS)
            if (videoPending) {
                videoPending = false
                startVideo()
            }
        }
    }

    fun stopVideo() {
        recording?.stop()
    }

    // Exactly the 4:3 photo, centred: the preview, the focus taps and the focus ring all live inside it,
    // so the black bars around it neither show the feed nor take focus taps.
    val photoFrame = Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(MediaShape)
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (hasCamera) {
            AndroidView(
                factory = {
                    PreviewView(it).apply {
                        previewView = this
                        this.controller = controller
                        scaleType = PreviewView.ScaleType.FIT_CENTER
                        // A TextureView fades with the screen; the default SurfaceView would show through the crossfade.
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewStreamState.observe(lifecycleOwner) { state -> streaming = state == PreviewView.StreamState.STREAMING }
                    }
                },
                modifier = photoFrame,
            )
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.camera_permission_needed), color = Color.White, textAlign = TextAlign.Center)
                Button(onClick = { requestPermissions() }) { Text(stringResource(R.string.allow_camera)) }
            }
        }
        if (hasCamera) {
            Box(
                photoFrame.pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val view = previewView ?: return@detectTapGestures
                        val point = view.meteringPointFactory.createPoint(offset.x, offset.y)
                        controller.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
                        focusAt = offset
                    }
                },
            )
            focusAt?.let { at ->
                val gold = accentGold()
                Canvas(photoFrame) {
                    val radius = 36.dp.toPx() * (1.4f - 0.4f * focusRing.value)
                    drawCircle(Color.White, radius, at, alpha = focusRing.value, style = Stroke(width = 2.dp.toPx()))
                    drawCircle(gold, 3.dp.toPx(), at, alpha = focusRing.value)
                }
            }
        }

        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(R.string.close), tint = Color.White) }
                Spacer(Modifier.weight(1f))
                if (hdrMode != null) {
                    IconButton(onClick = { hdrOn = !hdrOn }, enabled = recording == null) {
                        Icon(
                            when {
                                isNight -> Icons.Filled.NightsStay
                                hdrOn -> Icons.Filled.HdrOn
                                else -> Icons.Filled.HdrOff
                            },
                            contentDescription = stringResource(
                                when {
                                    isNight && hdrOn -> R.string.night_on
                                    isNight -> R.string.night_off
                                    hdrOn -> R.string.hdr_on
                                    else -> R.string.hdr_off
                                },
                            ),
                            tint = if (hdrOn) accentGold() else Color.White,
                        )
                    }
                }

            }
            Text(
                stringResource(R.string.camera_consent),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Spacer(Modifier.weight(1f))

            message?.let {
                Text(
                    it,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Text(
                // Night mode merges several frames over about a second: moving would blur them.
                stringResource(
                    when {
                        busy && isNight && hdrActive -> R.string.hold_still
                        else -> R.string.camera_hint
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
            )

            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                // Right of the shutter; spins half a turn on every switch.
                var turns by remember { mutableIntStateOf(0) }
                val spin by animateFloatAsState(turns * 180f, tween(400), label = "switch camera")
                IconButton(
                    onClick = {
                        front = !front
                        turns++
                    },
                    enabled = hasCamera && recording == null && !busy,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 40.dp)
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                ) {
                    Icon(
                        Icons.Filled.Cameraswitch,
                        stringResource(R.string.switch_camera),
                        tint = Color.White,
                        modifier = Modifier.graphicsLayer { rotationZ = spin },
                    )
                }
                val photoLabel = stringResource(R.string.take_photo)
                val videoLabel = stringResource(R.string.record_video)
                val gold = accentGold()
                Box(
                    Modifier
                        .size(84.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = photoLabel
                            onClick(photoLabel) { takePhoto(); true }
                            // Screen readers can't hold: this records a full 10-second clip instead.
                            customActions = listOf(CustomAccessibilityAction(videoLabel) { startVideo(); true })
                        }
                        .pointerInput(hasCamera) {
                            awaitEachGesture {
                                awaitFirstDown()
                                val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                    waitForUpOrCancellation()
                                    true
                                }
                                if (released == true) {
                                    takePhoto()
                                } else {
                                    if (hdrActive) {
                                        videoMode = true
                                        videoPending = true
                                    } else {
                                        startVideo()
                                    }
                                    waitForUpOrCancellation()
                                    if (videoPending) {
                                        // Let go before the switch finished: no video, HDR back on.
                                        videoPending = false
                                        videoMode = false
                                    } else {
                                        stopVideo()
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(Color.White, style = Stroke(width = 5.dp.toPx()))
                        if (recording != null) {
                            drawArc(gold, -90f, 360f * progress.value, useCenter = false, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }
                    Box(
                        Modifier
                            .size(if (recording != null) 36.dp else 66.dp)
                            .clip(if (recording != null) RoundedCornerShape(8.dp) else CircleShape)
                            .background(if (recording != null) Color(0xFFE53935) else Color.White),
                    )
                }
            }
        }

        if (choosing && todayMoments != null) {
            ReplaceChooser(todayMoments, onPick = { replacing = it }, onCancel = onClose)
        }

        captured?.let { moment ->
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                MomentImage(
                    moment,
                    maxPx = 2048,
                    contentScale = ContentScale.Fit,
                    fitShape = MediaShape,
                    modifier = Modifier.fillMaxSize().scale(1.08f - 0.08f * reveal.value).alpha(reveal.value),
                )
                Text(
                    saved,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .alpha(reveal.value)
                        .safeDrawingPadding()
                        .padding(bottom = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        // The white flash goes on top of everything, the preview included.
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash.value)))
    }
}

/** Today is full: the user taps the moment to replace before shooting, or leaves. */
@Composable
private fun ReplaceChooser(today: List<Moment>, onPick: (Moment) -> Unit, onCancel: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.today_full),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.today_full_pick),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        // As big as the screen allows: the photos share the full width.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val label = stringResource(R.string.replace_this_moment)
            today.forEach { moment ->
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(0.6f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                        .clickable(onClickLabel = label) { onPick(moment) },
                ) {
                    MomentImage(moment, maxPx = 720, modifier = Modifier.fillMaxSize())
                    if (moment.isVideo) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.video),
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center).size(40.dp),
                        )
                    }
                }
            }
        }
        TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel), color = Color.White) }
    }
}

/** Time for the camera to drop HDR and get video ready before recording starts. */
private const val VIDEO_SWITCH_MILLIS = 400L
