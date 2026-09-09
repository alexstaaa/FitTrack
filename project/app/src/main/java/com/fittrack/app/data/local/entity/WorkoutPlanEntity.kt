package com.fittrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-created (or starter) workout plan. Exercises belonging to the plan
 * live in [PlanExerciseEntity] (ordered join table).
 */
@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    /** True for the seeded starter plans (push/pull/legs, full body). */
    val isStarter: Boolean = false,
    /** Epoch millis. */
    val createdAt: Long,
)
