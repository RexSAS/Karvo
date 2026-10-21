package com.masterski

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.masterski.data.AppCompatLanguageController
import com.masterski.data.DataStoreSettingsRepository
import com.masterski.data.LanguageController
import com.masterski.data.LessonRepository
import com.masterski.data.MasterSkiDatabase
import com.masterski.data.RoomLessonRepository
import com.masterski.data.SettingsRepository
import com.masterski.data.backup.BackupService
import com.masterski.data.backup.BackupStore
import com.masterski.data.update.GitHubReleaseSource
import com.masterski.data.update.PackageInstallerUpdater
import com.masterski.data.update.UrlApkDownloader
import java.io.File
import java.time.Clock
import java.util.concurrent.atomic.AtomicBoolean

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: MasterSkiDatabase by lazy {
        Room.databaseBuilder(appContext, MasterSkiDatabase::class.java, "masterski.db").build()
    }

    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { appContext.preferencesDataStoreFile("settings") },
        )
    }

    val lessonRepository: LessonRepository by lazy { RoomLessonRepository(database.lessonDao()) }
    val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(dataStore) }
    val languageController: LanguageController = AppCompatLanguageController()
    val releaseSource by lazy { GitHubReleaseSource(BuildConfig.VERSION_NAME) }
    val apkDownloader by lazy { UrlApkDownloader() }
    val updateInstaller: PackageInstallerUpdater by lazy { PackageInstallerUpdater(appContext) }
    val updatesDir: File get() = File(appContext.cacheDir, "updates")

    private val updateCheckClaimed = AtomicBoolean(false)

    /** True only for the first caller in this process, so the automatic update check runs once per cold start. */
    fun claimUpdateCheck(): Boolean = updateCheckClaimed.compareAndSet(false, true)

    val backupService: BackupService by lazy {
        BackupService(BackupStore(File(appContext.filesDir, "backups"), Clock.systemUTC()), lessonRepository)
    }
}
