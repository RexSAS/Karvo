package com.masterski.data

import com.masterski.testing.FakeLessonRepository

class FakeLessonRepositoryTest : LessonRepositoryContract() {
    override fun repo(): LessonRepository = FakeLessonRepository()
}
