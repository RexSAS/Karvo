package com.karvo.testing

import com.karvo.data.LanguageChoice
import com.karvo.data.LanguageController

class FakeLanguageController(initial: LanguageChoice = LanguageChoice.SYSTEM) : LanguageController {
    private var choice = initial

    override fun current(): LanguageChoice = choice

    override fun set(choice: LanguageChoice) {
        this.choice = choice
    }
}
