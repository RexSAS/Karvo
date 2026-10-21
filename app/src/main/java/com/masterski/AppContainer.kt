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
}
