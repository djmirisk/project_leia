package com.solaresenergy.sisfv.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {

        override fun migrate(database: SupportSQLiteDatabase) {

            database.execSQL(
                """
                ALTER TABLE mantenimientos_offline
                ADD COLUMN tecnicoId INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )
        }
    }
}