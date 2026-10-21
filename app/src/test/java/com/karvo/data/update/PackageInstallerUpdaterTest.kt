package com.karvo.data.update

import android.app.Application
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PackageInstallerUpdaterTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val updater = PackageInstallerUpdater(app)
    private val confirm = Intent("android.content.pm.action.CONFIRM_INSTALL")

    private fun status(code: Int) = Intent().putExtra(PackageInstaller.EXTRA_STATUS, code)
    private fun pendingUserAction() = status(PackageInstaller.STATUS_PENDING_USER_ACTION).putExtra(Intent.EXTRA_INTENT, confirm)
    private fun nextStarted(): Intent? = shadowOf(app).nextStartedActivity

    @Test fun confirmationWaitsUntilTheAppIsInForeground() {
        updater.setAppInForeground(false)
        updater.onResult(pendingUserAction())
        assertNull(nextStarted())
        updater.setAppInForeground(true)
        assertEquals(confirm.action, nextStarted()?.action)
    }

    @Test fun confirmationIsShownOnlyOnce() {
        updater.setAppInForeground(true)
        updater.onResult(pendingUserAction())
        assertNotNull(nextStarted())
        updater.setAppInForeground(false)
        updater.setAppInForeground(true)
        assertNull(nextStarted())
    }

    @Test fun abortClearsAPendingConfirmation() = runTest {
        val results = mutableListOf<InstallResult>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { updater.results.collect { results += it } }
        updater.setAppInForeground(false)
        updater.onResult(pendingUserAction())
        updater.onResult(status(PackageInstaller.STATUS_FAILURE_ABORTED))
        updater.setAppInForeground(true)
        assertNull(nextStarted())
        assertEquals(listOf<InstallResult>(InstallResult.Aborted), results)
        job.cancel()
    }

    @Test fun confirmationCanBeReopenedUntilTheInstallEnds() {
        updater.setAppInForeground(true)
        updater.onResult(pendingUserAction())
        assertNotNull(nextStarted())
        assertEquals(true, updater.reopenConfirmation())
        assertEquals(confirm.action, nextStarted()?.action)
        updater.onResult(status(PackageInstaller.STATUS_FAILURE_ABORTED))
        assertEquals(false, updater.reopenConfirmation())
        assertNull(nextStarted())
    }
}
