package com.karvo.data.backup

import com.karvo.data.Lesson
import com.karvo.domain.BackupFormatException
import com.karvo.testing.FakeLessonRepository
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** A clock that moves forward one second every time it is read. */
class TickingClock(private var now: Instant = Instant.parse("2026-10-21T10:00:00Z")) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = now.also { now = now.plusSeconds(1) }
}

class BackupStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var dir: File
    private lateinit var store: BackupStore

    private fun lessons(n: Int) = (1..n).map { Lesson(date = LocalDate.of(2026, 1, it), durationMinutes = 60, student = "S$it") }

    @Before fun setUp() {
        dir = File(tmp.root, "backups")
        store = BackupStore(dir, TickingClock())
    }

    @Test fun createThenListNewestFirst() {
        store.create(BackupReason.Manual, lessons(1))
        store.create(BackupReason.BeforeUpdate("1.3"), lessons(2))
        store.create(BackupReason.BeforeRestore, lessons(3))
        val list = store.list()
        assertEquals(listOf(BackupReason.BeforeRestore, BackupReason.BeforeUpdate("1.3"), BackupReason.Manual), list.map { it.reason })
        assertEquals(listOf(3, 2, 1), list.map { it.lessonCount })
        assertTrue(list[0].createdAt > list[1].createdAt)
    }

    @Test fun keepsOnlyNewestThree() {
        val first = store.create(BackupReason.Manual, lessons(1))
        repeat(3) { store.create(BackupReason.Manual, lessons(2)) }
        val list = store.list()
        assertEquals(3, list.size)
        assertFalse(list.any { it.fileName == first.fileName })
        assertFalse(File(dir, first.fileName).exists())
    }

    @Test fun oldestToBeRemoved() {
        val oldest = store.create(BackupReason.Manual, lessons(1))
        store.create(BackupReason.Manual, lessons(1))
        assertNull(store.oldestToBeRemoved())
        store.create(BackupReason.Manual, lessons(1))
        assertEquals(oldest.fileName, store.oldestToBeRemoved()?.fileName)
    }

    @Test fun ignoresTmpAndForeignFiles() {
        store.create(BackupReason.Manual, lessons(1))
        File(dir, "x.tmp").writeText("partial")
        File(dir, "notes.txt").writeText("hi")
        File(dir, "backup-abc-manual.csv").writeText("bad name")
        assertEquals(1, store.list().size)
        assertFalse(File(dir, "x.tmp").exists())
    }

    @Test fun corruptFileListedAsUnreadable() {
        dir.mkdirs()
        File(dir, "backup-1-manual.csv").writeText("garbage")
        assertEquals(listOf(-1), store.list().map { it.lessonCount })
        assertThrows(BackupFormatException::class.java) { store.read("backup-1-manual.csv") }
    }

    @Test fun readRoundTrips() {
        val b = store.create(BackupReason.Manual, lessons(3))
        assertEquals(lessons(3), store.read(b.fileName))
    }

    @Test fun serviceBacksUpAllLessons() = runTest {
        val service = BackupService(store, FakeLessonRepository(lessons(2).mapIndexed { i, l -> l.copy(id = i + 1L) }), UnconfinedTestDispatcher(testScheduler))
        assertEquals(2, service.backup(BackupReason.Manual).lessonCount)
        assertEquals(1, service.list().size)
    }

    @Test fun backupMadeWhileClockIsBehindIsKept() {
        val ahead = BackupStore(dir, TickingClock(Instant.parse("2026-12-01T10:00:00Z")))
        repeat(3) { ahead.create(BackupReason.Manual, lessons(1)) }
        val behind = BackupStore(dir, TickingClock(Instant.parse("2026-10-21T10:00:00Z")))
        val safety = behind.create(BackupReason.BeforeRestore, lessons(2))
        val list = behind.list()
        assertEquals(3, list.size)
        assertEquals(safety.fileName, list.first().fileName)
        assertTrue(File(dir, safety.fileName).exists())
    }
}
