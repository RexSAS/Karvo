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

/** 3.2: several events per day replace the one-note-per-day table of the 3.2 test builds; notes become events. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `text` TEXT NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_events_date` ON `events` (`date`)")
        db.execSQL("INSERT INTO events (date, text) SELECT date, text FROM day_notes ORDER BY date")
        db.execSQL("DROP TABLE day_notes")
    }
}
