package com.masterski

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.masterski.data.ThemeChoice
import com.masterski.ui.MasterSkiNavHost
import com.masterski.ui.theme.MasterSkiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableAlpineBars()
        val settings = (application as MasterSkiApp).container.settingsRepository
        setContent {
            val theme by settings.theme.collectAsStateWithLifecycle(initialValue = ThemeChoice.ALPINE_NIGHT)
            LaunchedEffect(theme) {
                if (theme == ThemeChoice.ALPINE_NIGHT) enableAlpineBars() else enableEdgeToEdge()
            }
            MasterSkiTheme(theme) { MasterSkiNavHost() }
        }
    }

    /** Alpine Night is always dark, so system bar icons stay light regardless of the system setting. */
    private fun enableAlpineBars() =
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
}
