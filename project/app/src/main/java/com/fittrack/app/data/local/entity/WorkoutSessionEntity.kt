package com.fittrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One workout (in progress or completed). A session is "completed" when
 * [endedAt] is non-null. Deleting the source plan keeps the session
 * (planId is set to null).
 */
@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("planId")],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long? = null,
    val name: String,
    /** Epoch millis. */
    val startedAt: Long,
    /** Epoch millis; null while the workout is still running. */
    val endedAt: Long? = null,
    /** Sum of weight × reps over all logged sets, in kg. Set on completion. */
    val totalVolume: Double = 0.0,
    val note: String = "",
)
