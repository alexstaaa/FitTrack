package com.fittrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A body measurement snapshot. Weight is required; tape measurements are optional. */
@Entity(tableName = "body_stats")
data class BodyStatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Epoch millis. */
    val recordedAt: Long,
    val weightKg: Double,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val hipsCm: Double? = null,
    val note: String = "",
)
