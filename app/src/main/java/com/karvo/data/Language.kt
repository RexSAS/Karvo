package com.karvo.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** App language; SYSTEM follows the phone. The choice is stored by AppCompat, not DataStore. */
enum class LanguageChoice(val tag: String) { SYSTEM(""), ITALIAN("it"), ENGLISH("en") }

/** Maps AppCompat's comma-separated locale tags to a choice; unknown languages count as SYSTEM. */
fun languageChoiceFromTags(tags: String): LanguageChoice {
    val first = tags.substringBefore(',').trim()
    return when {
        first.isEmpty() -> LanguageChoice.SYSTEM
        first.startsWith("it") -> LanguageChoice.ITALIAN
        first.startsWith("en") -> LanguageChoice.ENGLISH
        else -> LanguageChoice.SYSTEM
    }
}

interface LanguageController {
    fun current(): LanguageChoice
    fun set(choice: LanguageChoice)
}

class AppCompatLanguageController : LanguageController {
    override fun current(): LanguageChoice =
        languageChoiceFromTags(AppCompatDelegate.getApplicationLocales().toLanguageTags())

    override fun set(choice: LanguageChoice) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(choice.tag))
    }
}
