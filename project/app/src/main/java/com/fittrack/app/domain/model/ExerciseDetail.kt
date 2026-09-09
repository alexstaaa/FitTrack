package com.fittrack.app.domain.model

import com.fittrack.app.data.local.entity.ExerciseEntity

/** An exercise plus the instruction text resolved for the user's locale. */
data class ExerciseDetail(
    val exercise: ExerciseEntity,
    /** Instruction text in the device locale, falling back to English. */
    val instructions: String?,
    /** Locale code the instructions were resolved to (e.g. "en", "es"). */
    val instructionsLocale: String?,
)
