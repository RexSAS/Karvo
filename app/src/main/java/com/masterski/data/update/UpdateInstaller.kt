package com.masterski.data.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext

sealed interface InstallResult {
    /** The user cancelled the system install dialog. */
    data object Aborted : InstallResult
    data class Failed(val code: Int, val signatureMismatch: Boolean) : InstallResult
}

interface UpdateInstaller {
    fun canInstall(): Boolean
    fun openInstallPermissionSettings()
    /** Hands [apk] to the system installer; the outcome arrives on [results] (success ends this process). */
    suspend fun install(apk: File)
    val results: SharedFlow<InstallResult>
}

/** Installs updates through a [PackageInstaller] session; Android asks the user to confirm. */
class PackageInstallerUpdater(private val context: Context) : UpdateInstaller {
    private val _results = MutableSharedFlow<InstallResult>(extraBufferCapacity = 4)
    override val results: SharedFlow<InstallResult> = _results

    override fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    override fun openInstallPermissionSettings() {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override suspend fun install(apk: File) = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            .apply { setAppPackageName(context.packageName) }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("MasterSki.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val callback = PendingIntent.getBroadcast(
                context, sessionId, Intent(context, InstallResultReceiver::class.java).setPackage(context.packageName), flags,
            )
            session.commit(callback.intentSender)
        }
    }

    /** Called by [InstallResultReceiver] with the session's status. */
    fun onResult(intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
                context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PackageInstaller.STATUS_SUCCESS -> Unit
            PackageInstaller.STATUS_FAILURE_ABORTED -> _results.tryEmit(InstallResult.Aborted)
            else -> _results.tryEmit(
                InstallResult.Failed(
                    status,
                    signatureMismatch = status == PackageInstaller.STATUS_FAILURE_CONFLICT ||
                        status == PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
                ),
            )
        }
    }
}
