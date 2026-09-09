package com.fittrack.app.data.seed

import android.content.Context
import android.util.Log
import com.fittrack.app.data.local.dao.ExerciseDao
import com.fittrack.app.data.local.entity.ExerciseEntity
import com.fittrack.app.data.local.entity.ExerciseInstructionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ExerciseSeeder"
private const val ASSET_FILE = "exercises.json"

/**
 * DTOs mirror the raw shape of assets/exercises.json exactly (see
 * docs/dataset-schema.md). Kept separate from ExerciseEntity/
 * ExerciseInstructionEntity so a future dataset format change doesn't ripple
 * into the Room schema directly.
 */
@Serializable
data class ExerciseDto(
    val id: String,
    val name: String,
    val category: String,
    val body_part: String,
    val equipment: String,
    val instructions: Map<String, String>,
    val muscle_group: String,
    val secondary_muscles: List<String> = emptyList(),
    val target: String,
    val media_id: String? = null,
    val image: String? = null,
    val gif_url: String? = null,
    val created_at: String,
)

/**
 * One-time importer: reads assets/exercises.json, parses ~1,324 records, and
 * seeds the Room database. Call seedIfEmpty() from app startup (e.g. a Hilt
 * WorkManager job or a check in the repository's init) — do not re-run on
 * every launch.
 */
@Singleton
class ExerciseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exerciseDao: ExerciseDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun seedIfEmpty() {
        if (exerciseDao.count() > 0) {
            Log.i(TAG, "Exercises already seeded, skipping import.")
            return
        }
        seedFromAssets()
    }

    private suspend fun seedFromAssets() {
        val raw = readAsset(ASSET_FILE)
        val dtos: List<ExerciseDto> = try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse $ASSET_FILE", e)
            return
        }

        var imported = 0
        var skipped = 0
        val entities = mutableListOf<ExerciseEntity>()
        val instructionRows = mutableListOf<ExerciseInstructionEntity>()

        for (dto in dtos) {
            if (dto.id.isBlank() || dto.name.isBlank()) {
                skipped++
                continue
            }
            entities += ExerciseEntity(
                id = dto.id,
                name = dto.name,
                bodyPart = dto.body_part.ifBlank { dto.category },
                equipment = dto.equipment,
                muscleGroup = dto.muscle_group,
                secondaryMuscles = dto.secondary_muscles,
                target = dto.target,
                mediaId = dto.media_id,
                createdAt = dto.created_at,
            )
            dto.instructions.forEach { (locale, text) ->
                if (text.isNotBlank()) {
                    instructionRows += ExerciseInstructionEntity(dto.id, locale, text)
                }
            }
            imported++
        }

        exerciseDao.insertAll(entities)
        exerciseDao.insertInstructions(instructionRows)
        Log.i(TAG, "Seed complete: imported=$imported skipped=$skipped totalRows=${dtos.size}")
    }

    private fun readAsset(fileName: String): String {
        context.assets.open(fileName).use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                return reader.readText()
            }
        }
    }
}
