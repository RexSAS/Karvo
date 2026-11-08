package com.karvo

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Play build: when Play has a newer Karvo, Play's update dialog appears; the download runs while Karvo stays
 * usable, then Karvo offers to restart into the new version (now or later; Play also installs it when Karvo is closed).
 */
object StoreUpdates {
    fun check(activity: AppCompatActivity, offer: Boolean) {
        val manager = AppUpdateManagerFactory.create(activity)
        val launcher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}
        var asking: AlertDialog? = null
        fun askRestart() {
            if (asking?.isShowing == true || activity.isFinishing) return
            asking = AlertDialog.Builder(activity)
                .setTitle(R.string.play_update_ready_title)
                .setMessage(R.string.play_update_ready_text)
                .setPositiveButton(R.string.play_update_restart) { _, _ -> manager.completeUpdate() }
                .setNegativeButton(R.string.play_update_later, null)
                .show()
        }
        val listener = InstallStateUpdatedListener { if (it.installStatus() == InstallStatus.DOWNLOADED) askRestart() }
        manager.registerListener(listener)
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            // Downloaded while Karvo was in the background: ask when it comes back.
            override fun onResume(owner: LifecycleOwner) {
                manager.appUpdateInfo.addOnSuccessListener { if (it.installStatus() == InstallStatus.DOWNLOADED) askRestart() }
            }
            override fun onDestroy(owner: LifecycleOwner) = manager.unregisterListener(listener)
        })
        if (!offer) return // once per launch, not again on rotation
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())
            }
        } // no Play / offline: nothing happens
    }
}
