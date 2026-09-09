package com.fittrack.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: adds workout plans, sessions, logged sets and body stats.
 * SQL mirrors the Room-generated schema exactly (see app/schemas/).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `workout_plans` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `isStarter` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `plan_exercises` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `planId` INTEGER NOT NULL,
                `exerciseId` TEXT NOT NULL,
                `position` INTEGER NOT NULL,
                `targetSets` INTEGER NOT NULL,
                `targetReps` INTEGER NOT NULL,
                `targetWeight` REAL,
                `restSeconds` INTEGER NOT NULL,
                FOREIGN KEY(`planId`) REFERENCES `workout_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_exercises_planId` ON `plan_exercises` (`planId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_exercises_exerciseId` ON `plan_exercises` (`exerciseId`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `workout_sessions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `planId` INTEGER,
                `name` TEXT NOT NULL,
                `startedAt` INTEGER NOT NULL,
                `endedAt` INTEGER,
                `totalVolume` REAL NOT NULL,
                `note` TEXT NOT NULL,
                FOREIGN KEY(`planId`) REFERENCES `workout_plans`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_planId` ON `workout_sessions` (`planId`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `logged_sets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sessionId` INTEGER NOT NULL,
                `exerciseId` TEXT NOT NULL,
                `exerciseOrder` INTEGER NOT NULL,
                `setNumber` INTEGER NOT NULL,
                `reps` INTEGER NOT NULL,
                `weight` REAL NOT NULL,
                `rpe` INTEGER,
                `note` TEXT NOT NULL,
                `completedAt` INTEGER NOT NULL,
                FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_logged_sets_sessionId` ON `logged_sets` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_logged_sets_exerciseId` ON `logged_sets` (`exerciseId`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `body_stats` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `recordedAt` INTEGER NOT NULL,
                `weightKg` REAL NOT NULL,
                `chestCm` REAL,
                `waistCm` REAL,
                `hipsCm` REAL,
                `note` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}
