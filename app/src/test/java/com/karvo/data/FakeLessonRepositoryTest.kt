package com.karvo.data

import com.karvo.testing.FakeLessonRepository

class FakeLessonRepositoryTest : LessonRepositoryContract() {
    override fun repo(): LessonRepository = FakeLessonRepository()
}
