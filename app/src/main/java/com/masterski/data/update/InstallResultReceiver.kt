package com.masterski.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.masterski.MasterSkiApp

/** Receives [android.content.pm.PackageInstaller] session results for an update. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as MasterSkiApp).container.updateInstaller.onResult(intent)
    }
}
