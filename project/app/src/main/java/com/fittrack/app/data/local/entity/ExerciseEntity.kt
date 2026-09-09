package com.fittrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Maps directly to records in assets/exercises.json (1,324 rows, ExerciseDB v1
 * derived). Notes:
 *  - `category` and `body_part` were identical in every source record, so only
 *    `bodyPart` is kept here.
 *  - Instructions are per-language; stored as a child table
 *    (see ExerciseInstructionEntity) rather than columns, so we're not stuck
 *    with 6 nullable text blobs on every query.
 *  - `secondaryMuscles` uses a Room TypeConverter (see Converters.kt) backed
 *    by JSON encoding of List<String>.
 *  - `imageUrl` / `gifUrl` are intentionally not modeled — the dataset ships
 *    them as null (media is not redistributed with the dataset). `mediaId`
 *    references the original ExerciseDB media; per the build prompt's Media
 *    Strategy, GIFs are streamed at runtime from the official ExerciseDB CDN
 *    (see data/remote/MediaUrlProvider) and never bundled. The UI must always
 *    degrade to a body-part placeholder icon when media is unavailable.
 */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val bodyPart: String,
    val equipment: String,
    val muscleGroup: String,
    val secondaryMuscles: List<String>,
    val target: String,
    val mediaId: String?,
    val createdAt: String,
    val isFavorite: Boolean = false,
)
