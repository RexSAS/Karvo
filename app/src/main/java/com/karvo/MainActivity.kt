package com.karvo

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.SystemBarStyle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karvo.data.ThemeChoice
import com.karvo.ui.KarvoNavHost
import com.karvo.ui.theme.KarvoTheme
import com.karvo.ui.theme.SPLASH_ANIMATION_MS
import com.karvo.ui.theme.animateSplashExit
import com.karvo.ui.theme.shouldKeepSplash
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableAlpineBars()
        val container = (application as KarvoApp).container
        val settings = container.settingsRepository
        container.authenticator.attach(this)

        // Keep the splash until the theme setting is read, so the first frame already has the right theme.
        var startTheme: ThemeChoice? = null
        val startedAt = SystemClock.elapsedRealtime()
        lifecycleScope.launch { startTheme = settings.theme.first() }
        // Android 12+ animates the logo, so let the animation play out; older versions show it still.
        val minShowMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) SPLASH_ANIMATION_MS else 0L
        splash.setKeepOnScreenCondition {
            shouldKeepSplash(startTheme != null, SystemClock.elapsedRealtime() - startedAt, minShowMs)
        }
        splash.setOnExitAnimationListener(::animateSplashExit)

        setContent {
            val theme by settings.theme.collectAsStateWithLifecycle(initialValue = startTheme ?: ThemeChoice.ALPINE_NIGHT)
            LaunchedEffect(theme) {
                if (theme == ThemeChoice.ALPINE_NIGHT) enableAlpineBars() else enableEdgeToEdge()
            }
            KarvoTheme(theme) { KarvoNavHost() }
        }
    }

    override fun onDestroy() {
        (application as KarvoApp).container.authenticator.detach(this)
        super.onDestroy()
    }

    /** Alpine Night is always dark, so system bar icons stay light regardless of the system setting. */
    private fun enableAlpineBars() =
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
}
