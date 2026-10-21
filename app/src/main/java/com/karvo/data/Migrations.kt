package com.karvo.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** 2.1: lessons gain a nullable paid date; existing lessons become unpaid. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE lessons ADD COLUMN paidOn INTEGER")
    }
}
