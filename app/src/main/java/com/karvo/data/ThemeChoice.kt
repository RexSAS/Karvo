package com.karvo.data

enum class ThemeChoice { ALPINE_NIGHT, CLASSIC }

/** The theme to draw: Alpine Night is Pro only, so free users always get Classic. */
fun effectiveTheme(choice: ThemeChoice, pro: Boolean): ThemeChoice = if (pro) choice else ThemeChoice.CLASSIC

/** Parses a stored theme name; missing or unknown values fall back to Classic. */
fun themeChoiceOf(name: String?): ThemeChoice =
    ThemeChoice.entries.firstOrNull { it.name == name } ?: ThemeChoice.CLASSIC
