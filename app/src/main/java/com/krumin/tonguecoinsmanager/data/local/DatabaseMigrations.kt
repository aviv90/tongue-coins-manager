package com.krumin.tonguecoinsmanager.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = Migration(1, 2) { db: SupportSQLiteDatabase ->
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `test_devices` (
                `token` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `addedAt` INTEGER NOT NULL,
                PRIMARY KEY(`token`)
            )
            """.trimIndent()
        )
    }
}
