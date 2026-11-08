package com.karvo

import android.app.Application

class KarvoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.noteAlarms // arms the next day-note reminder and keeps it in step with the notes
    }
}
