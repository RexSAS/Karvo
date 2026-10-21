package com.masterski.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<Lesson>

    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun getById(id: Long): Lesson?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(lesson: Lesson): Long

    @Update
    suspend fun update(lesson: Lesson)

    @Delete
    suspend fun delete(lesson: Lesson)

    @Query("DELETE FROM lessons")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(lessons: List<Lesson>)

    /** Swaps every lesson for [lessons] atomically: if the insert fails, the delete is rolled back. */
    @Transaction
    suspend fun replaceAll(lessons: List<Lesson>) {
        deleteAll()
        insertAll(lessons)
    }

    @Query("SELECT student FROM lessons GROUP BY student ORDER BY MAX(date) DESC, MAX(id) DESC")
    fun observeStudentNames(): Flow<List<String>>
}
