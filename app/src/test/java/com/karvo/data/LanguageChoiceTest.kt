package com.karvo.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageChoiceTest {
    @Test fun mapsTags() {
        assertEquals(LanguageChoice.SYSTEM, languageChoiceFromTags(""))
        assertEquals(LanguageChoice.ITALIAN, languageChoiceFromTags("it"))
        assertEquals(LanguageChoice.ITALIAN, languageChoiceFromTags("it-IT"))
        assertEquals(LanguageChoice.ENGLISH, languageChoiceFromTags("en-GB,it"))
        assertEquals(LanguageChoice.SYSTEM, languageChoiceFromTags("de"))
    }
}
