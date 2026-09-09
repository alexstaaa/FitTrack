package com.fittrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One exercise slot inside a workout plan, with its position and targets.
 * Deleting the plan cascades; deleting an exercise (never happens for the
 * seeded dataset) would cascade too.
 */
@Entity(
    tableName = "plan_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("planId"), Index("exerciseId")],
)
data class PlanExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val exerciseId: String,
    /** 0-based order within the plan. */
    val position: Int,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    /** Kilograms; null = bodyweight / not set. */
    val targetWeight: Double? = null,
    val restSeconds: Int = 90,
)
