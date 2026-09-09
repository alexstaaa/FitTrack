package com.fittrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * One row per (exercise, locale). Locale is one of: en, es, it, tr, ru, zh
 * (matches the dataset's `instructions` object keys). Query with the app's
 * current locale and fall back to "en" if missing.
 */
@Entity(
    tableName = "exercise_instructions",
    primaryKeys = ["exerciseId", "locale"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseId")],
)
data class ExerciseInstructionEntity(
    val exerciseId: String,
    val locale: String,
    val text: String,
)
