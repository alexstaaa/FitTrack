package com.fittrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One performed set within a workout session. Written to the database the
 * moment the user checks the set off, so an interrupted workout loses nothing.
 */
@Entity(
    tableName = "logged_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class LoggedSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    /** Order of the exercise within the session (0-based). */
    val exerciseOrder: Int,
    /** 1-based set number within the exercise. */
    val setNumber: Int,
    val reps: Int,
    /** Kilograms; 0 for pure bodyweight work. */
    val weight: Double,
    /** Rate of perceived exertion, 1–10; optional. */
    val rpe: Int? = null,
    val note: String = "",
    /** Epoch millis. */
    val completedAt: Long,
)
