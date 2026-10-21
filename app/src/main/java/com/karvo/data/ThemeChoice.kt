package com.karvo.data

enum class ThemeChoice { ALPINE_NIGHT, CLASSIC }

/** Parses a stored theme name; missing or unknown values fall back to Alpine Night. */
fun themeChoiceOf(name: String?): ThemeChoice =
    ThemeChoice.entries.firstOrNull { it.name == name } ?: ThemeChoice.ALPINE_NIGHT
