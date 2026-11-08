package com.karvo.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** 2.1: lessons gain a nullable paid date; existing lessons become unpaid. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE lessons ADD COLUMN paidOn INTEGER")
    }
}

/** 3.2: day notes (one per day). */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `day_notes` (`date` INTEGER NOT NULL, `text` TEXT NOT NULL, PRIMARY KEY(`date`))")
    }
}
