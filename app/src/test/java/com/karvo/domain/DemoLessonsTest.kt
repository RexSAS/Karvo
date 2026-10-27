package com.karvo.domain

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The demo season in docs/demo (for screenshots and demos) must stay importable. */
class DemoLessonsTest {
    @Test fun demoFileImports() {
        val file = listOf(File("docs/demo/karvo-demo-lessons.csv"), File("../docs/demo/karvo-demo-lessons.csv")).first { it.exists() }
        val lessons = parseLessonsCsv(file.readText(Charsets.UTF_8))
        assertTrue(lessons.size > 100)
        assertTrue(lessons.any { it.paidOn != null } && lessons.any { it.paidOn == null })
    }
}
