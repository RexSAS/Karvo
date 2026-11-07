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
import com.karvo.data.update.GitHubReleaseSource
import com.karvo.data.update.PackageInstallerUpdater
import com.karvo.data.update.UrlApkDownloader
import android.os.Build
import android.provider.Settings
import com.karvo.domain.LessonError
import com.karvo.link.KeystoreSecretBox
import com.karvo.link.LinkClient
import com.karvo.link.LinkCommandHandler
import com.karvo.link.LinkManager
import com.karvo.link.LinkService
import com.karvo.link.OkHttpRelaySocketFactory
import com.karvo.link.Refusal
import com.karvo.ui.messageRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import com.karvo.link.PairingStore
import java.io.File
import java.time.Clock
import java.util.concurrent.atomic.AtomicBoolean

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

    /** Linked PCs (phone link). Its own file, so a corrupt link list never touches the app settings. */
    val pairingStore: PairingStore by lazy {
        PairingStore(
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                produceFile = { appContext.preferencesDataStoreFile("link") },
            ),
            KeystoreSecretBox(),
        )
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val linkHandler by lazy {
        LinkCommandHandler(lessonRepository, settingsRepository,
            messages = { e: LessonError -> appContext.getString(e.messageRes()) },
            refusal = { r -> appContext.getString(when (r) {
                Refusal.LessonGone -> R.string.link_refused_lesson_gone
                Refusal.PhoneNotEmpty -> R.string.link_refused_phone_not_empty
                Refusal.InvalidRate -> R.string.link_refused_invalid_rate
                Refusal.InvalidMonth -> R.string.link_refused_invalid_month
                Refusal.InvalidData -> R.string.link_refused_invalid_data
                Refusal.UnknownCommand -> R.string.link_refused_unknown
            }) })
    }

    private val relaySockets by lazy { OkHttpRelaySocketFactory(OkHttpClient()) }

    /** The name the PC shows ("Connected to …"): the phone's own device name, else its model. */
    private val deviceName: String
        get() = Settings.Global.getString(appContext.contentResolver, Settings.Global.DEVICE_NAME)?.takeIf { it.isNotBlank() } ?: Build.MODEL

    val linkManager: LinkManager by lazy {
        LinkManager(pairingStore, { pc -> LinkClient(pc, linkHandler, relaySockets, appScope, BuildConfig.VERSION_NAME, deviceName) },
            LinkService.starter(appContext), appScope)
    }

    val backupService: BackupService by lazy {
        BackupService(BackupStore(File(appContext.filesDir, "backups"), Clock.systemUTC()), lessonRepository)
    }
}
