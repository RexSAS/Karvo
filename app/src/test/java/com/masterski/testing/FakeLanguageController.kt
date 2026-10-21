package com.masterski.testing

import com.masterski.data.LanguageChoice
import com.masterski.data.LanguageController

class FakeLanguageController(initial: LanguageChoice = LanguageChoice.SYSTEM) : LanguageController {
    private var choice = initial

    override fun current(): LanguageChoice = choice

    override fun set(choice: LanguageChoice) {
        this.choice = choice
    }
}
