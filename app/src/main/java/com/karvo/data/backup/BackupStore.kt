package com.karvo.data.backup

import com.karvo.data.Lesson
import com.karvo.domain.csvFileBytes
import com.karvo.domain.lessonsToCsv
import com.karvo.domain.parseLessonsCsv
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant

const val MAX_BACKUPS = 3
const val FREE_MAX_BACKUPS = 1

sealed interface BackupReason {
    data object Manual : BackupReason
    data object BeforeRestore : BackupReason
    data object BeforeImport : BackupReason
    data class BeforeUpdate(val version: String) : BackupReason
}

/** A stored backup; [lessonCount] is -1 when the file can't be read. */
data class Backup(val fileName: String, val createdAt: Instant, val reason: BackupReason, val lessonCount: Int)

private val NAME = Regex("""backup-(\d+)-(manual|restore|import|update_([0-9.]+))\.csv""")

private fun BackupReason.slug(): String = when (this) {
    BackupReason.Manual -> "manual"
    BackupReason.BeforeRestore -> "restore"
    BackupReason.BeforeImport -> "import"
    is BackupReason.BeforeUpdate -> "update_$version"
}

/**
 * Lesson backups kept as CSV files in [dir] (app-private storage). The file name carries the time and
 * reason, so no other metadata is stored. Only the newest `keep` (default [MAX_BACKUPS]) are kept. Blocking I/O.
 */
class BackupStore(private val dir: File, private val clock: Clock) {

    fun list(): List<Backup> {
        val files = dir.listFiles().orEmpty()
        files.filter { it.name.endsWith(".tmp") }.forEach { it.delete() }
        return files.mapNotNull { file ->
            val match = NAME.matchEntire(file.name) ?: return@mapNotNull null
            val millis = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val reason = when (match.groupValues[2]) {
                "manual" -> BackupReason.Manual
                "restore" -> BackupReason.BeforeRestore
                "import" -> BackupReason.BeforeImport
                else -> BackupReason.BeforeUpdate(match.groupValues[3])
            }
            Backup(file.name, Instant.ofEpochMilli(millis), reason, countLessons(file))
        }.sortedByDescending { it.createdAt }
    }

    /** Writes a new backup atomically (temp file, then rename), then deletes the oldest beyond [keep]. */
    fun create(reason: BackupReason, lessons: List<Lesson>, keep: Int = MAX_BACKUPS): Backup {
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Can't create $dir")
        // Always newer than every existing backup, even if the clock went backwards; otherwise the
        // rotation below would treat the new backup as the oldest and delete it straight away.
        val newest = list().maxOfOrNull { it.createdAt.toEpochMilli() } ?: Long.MIN_VALUE
        val millis = maxOf(clock.millis(), newest + 1)
        val name = "backup-$millis-${reason.slug()}.csv"
        val tmp = File(dir, "$name.tmp")
        tmp.writeBytes(csvFileBytes(lessonsToCsv(lessons)))
        if (!tmp.renameTo(File(dir, name))) {
            tmp.delete()
            throw IOException("Can't save backup $name")
        }
        list().drop(keep).forEach { File(dir, it.fileName).delete() }
        return Backup(name, Instant.ofEpochMilli(millis), reason, lessons.size)
    }

    /** Throws [com.karvo.domain.BackupFormatException] for a damaged file, [IOException] for a missing one. */
    fun read(fileName: String): List<Lesson> = parseLessonsCsv(File(dir, fileName).readText(Charsets.UTF_8))

    /** The backup the next [create] would delete, or null while fewer than [keep] exist. */
    fun oldestToBeRemoved(keep: Int = MAX_BACKUPS): Backup? = list().takeIf { it.size >= keep }?.last()

    private fun countLessons(file: File): Int = try {
        parseLessonsCsv(file.readText(Charsets.UTF_8)).size
    } catch (_: Exception) {
        -1
    }
}
