package com.karvo.ui.link

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karvo.R
import com.karvo.link.LinkState
import java.util.concurrent.Executors

/** Settings → Linked PCs: pair the PC web app by QR, see which PCs are connected, remove them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedPcsScreen(
    onBack: () -> Unit,
    showMessage: (String) -> Unit,
    viewModel: LinkedPcsViewModel = viewModel(factory = LinkedPcsViewModel.Factory),
) {
    val context = LocalContext.current
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    var scanning by rememberSaveable { mutableStateOf(false) }
    var removing by remember { mutableStateOf<PcRow?>(null) }

    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            showMessage(when (e) {
                LinkEvent.NotKarvo -> context.getString(R.string.link_error_not_karvo)
                LinkEvent.Outdated -> context.getString(R.string.link_error_outdated)
                LinkEvent.Expired -> context.getString(R.string.link_error_expired)
                LinkEvent.Failed -> context.getString(R.string.link_error_failed)
                is LinkEvent.Linked -> context.getString(R.string.link_linked, e.pcName)
            })
            // The "Karvo is linked to …" notification needs this on Android 13+; ask right after the first link.
            if (e is LinkEvent.Linked && Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (scanning) {
        ScanQr(onCode = { scanning = false; viewModel.onScanned(it) }, onClose = { scanning = false })
        return
    }

    Scaffold(
        containerColor = Color.Transparent, // the alpine backdrop shows through
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.linked_pcs)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(stringResource(R.string.linked_pcs_none), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(ui.pcs, key = { it.roomId }) { pc ->
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(Icons.Filled.Computer, contentDescription = null) },
                    headlineContent = { Text(pc.name) },
                    supportingContent = {
                        Text(stringResource(when {
                            pc.broken || pc.state == LinkState.BROKEN -> R.string.link_state_broken
                            pc.state == LinkState.CONNECTED -> R.string.link_state_connected
                            pc.state == LinkState.OUTDATED -> R.string.link_state_outdated
                            else -> R.string.link_state_offline
                        }), color = if (pc.state == LinkState.CONNECTED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingContent = { TextButton(onClick = { removing = pc }) { Text(stringResource(R.string.link_remove)) } },
                )
            }
            item {
                Button(onClick = { scanning = true }, enabled = !ui.busy, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    if (ui.busy) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.link_linking), Modifier.padding(start = 8.dp))
                    } else {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                        Text(stringResource(R.string.link_a_pc), Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    ui.confirm?.let { qr ->
        AlertDialog(
            onDismissRequest = viewModel::onDeny,
            title = { Text(stringResource(R.string.link_confirm_title, qr.pcName)) },
            text = { Text(stringResource(R.string.link_confirm_body)) },
            confirmButton = { TextButton(onClick = viewModel::onAllow) { Text(stringResource(R.string.link_allow)) } },
            dismissButton = { TextButton(onClick = viewModel::onDeny) { Text(stringResource(R.string.link_deny)) } },
        )
    }
    removing?.let { pc ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(pc.name) },
            text = { Text(stringResource(R.string.link_remove_confirm, pc.name)) },
            confirmButton = { TextButton(onClick = { viewModel.remove(pc.roomId); removing = null }) { Text(stringResource(R.string.link_remove)) } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** Full-screen camera with a square guide; reports the first Karvo link code. */
@Composable
private fun ScanQr(onCode: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    BackHandler(onBack = onClose)
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var asked by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; asked = true }
    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (granted) {
            val executor = remember { Executors.newSingleThreadExecutor() }
            DisposableEffect(Unit) { onDispose { executor.shutdown() } }
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
                PreviewView(ctx).also { view ->
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                        val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                            .also { it.setAnalyzer(executor, QrAnalyzer { text -> ContextCompat.getMainExecutor(ctx).execute { onCode(text) } }) }
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    }, ContextCompat.getMainExecutor(ctx))
                }
            })
            Box(Modifier.size(240.dp).border(3.dp, Color.White, RoundedCornerShape(20.dp)))
            Text(stringResource(R.string.link_scan_hint), color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
        } else if (asked) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.link_camera_denied))
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                }) { Text(stringResource(R.string.link_open_settings)) }
            }
        }
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
        }
    }
}
