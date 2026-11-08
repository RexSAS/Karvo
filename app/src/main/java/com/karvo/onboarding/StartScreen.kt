package com.karvo.onboarding

import com.karvo.domain.parseVersion

enum class StartScreen { Welcome, WhatsNew, None }

/**
 * What to show on launch: Welcome on a fresh install, What's new once after an update, otherwise nothing.
 * [lastSeen] is null both on a fresh install and on an update from 3.1 (which stored no version): lessons tell them apart.
 */
fun startScreen(lastSeen: String?, current: String, hasLessons: Boolean): StartScreen {
    if (lastSeen == null) return if (hasLessons) StartScreen.WhatsNew else StartScreen.Welcome
    val seen = parseVersion(lastSeen) ?: return StartScreen.None
    val now = parseVersion(current) ?: return StartScreen.None
    return if (seen < now) StartScreen.WhatsNew else StartScreen.None
}
