package com.karvo

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.karvo.data.AppCompatLanguageController
import com.karvo.data.BiometricAuthenticator
import com.karvo.data.ContentResolverTextFileReader
import com.karvo.data.DataStoreSettingsRepository
import com.karvo.data.LanguageController
import com.karvo.data.LessonRepository
import com.karvo.data.MIGRATION_1_2
import com.karvo.data.KarvoDatabase
import com.karvo.data.RoomLessonRepository
import com.karvo.data.SettingsRepository
import com.karvo.data.backup.BackupService
import com.karvo.data.moments.MomentStore
import com.karvo.data.backup.BackupStore
import com.karvo.data.backup.FREE_MAX_BACKUPS
import com.karvo.data.backup.MAX_BACKUPS
import com.karvo.data.update.GitHubReleaseSource
import com.karvo.data.update.PackageInstallerUpdater
import com.karvo.data.update.UrlApkDownloader
import java.io.File
import java.time.Clock
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.first

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: KarvoDatabase by lazy {
        Room.databaseBuilder(appContext, KarvoDatabase::class.java, "karvo.db")
            .addMigrations(MIGRATION_1_2)
            .build()
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
    val textFileReader by lazy { ContentResolverTextFileReader(appContext) }
    val authenticator = BiometricAuthenticator(appContext)
    val releaseSource by lazy { GitHubReleaseSource(BuildConfig.VERSION_NAME) }
    val apkDownloader by lazy { UrlApkDownloader() }
    val updateInstaller: PackageInstallerUpdater by lazy { PackageInstallerUpdater(appContext) }
    val updatesDir: File get() = File(appContext.cacheDir, "updates")

    private val updateCheckClaimed = AtomicBoolean(false)

    /** True only for the first caller in this process, so the automatic update check runs once per cold start. */
    fun claimUpdateCheck(): Boolean = updateCheckClaimed.compareAndSet(false, true)

    val momentStore by lazy { MomentStore(appContext) }

    val backupService: BackupService by lazy {
        BackupService(BackupStore(File(appContext.filesDir, "backups"), Clock.systemUTC()), lessonRepository) {
            if (settingsRepository.pro.first()) MAX_BACKUPS else FREE_MAX_BACKUPS
        }
    }
}
