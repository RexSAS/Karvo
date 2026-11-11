package com.karvo.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** One way of moving for the whole app: long, soft ease-outs, like snow settling. */
object KarvoMotion {
    /** Starts quickly and settles slowly. */
    val Ease = CubicBezierEasing(.2f, .8f, .2f, 1f)

    /** The answer to a tap: fades and small changes. */
    const val Short = 250

    /** Things that arrive: text, cards, the first screen. */
    const val Medium = 450

    /** Things that grow: charts, the podium, a glint. */
    const val Long = 650
}

/**
 * True when the phone's "Remove animations" setting is on. Compose already ends every animation at once then;
 * this is for movement that follows a finger (the backdrop's parallax), which must stop too.
 */
@Composable
fun reducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}
