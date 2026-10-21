package com.karvo.i18n

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element

class TranslationCompletenessTest {
    private fun keys(path: String): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))
        return listOf("string", "plurals").flatMap { tag ->
            val nodes = doc.getElementsByTagName(tag)
            (0 until nodes.length).map { "$tag:" + (nodes.item(it) as Element).getAttribute("name") }
        }.filterNot { it == "string:app_name" }.toSet()
    }

    @Test fun italianHasExactlyTheEnglishKeys() {
        assertEquals(keys("src/main/res/values/strings.xml"), keys("src/main/res/values-it/strings.xml"))
    }
}
