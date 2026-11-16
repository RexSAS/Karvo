package com.karvo

import android.app.Application
import com.karvo.widget.WeekWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class KarvoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.noteAlarms // arms the next day-note reminder and keeps it in step with the notes
        // Any lesson change (app, phone link, restore, import) redraws the home-screen widget.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.lessonRepository.observeLessons().drop(1).collect { runCatching { WeekWidget.refresh(this@KarvoApp) } }
        }
    }
}
