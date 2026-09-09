package com.fittrack.app.fakes

import com.fittrack.app.data.local.dao.ExerciseDao
import com.fittrack.app.data.local.entity.ExerciseEntity
import com.fittrack.app.data.local.entity.ExerciseInstructionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory ExerciseDao for ViewModel/repository tests. */
class FakeExerciseDao : ExerciseDao {

    val exercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    private val instructions = mutableListOf<ExerciseInstructionEntity>()

    override suspend fun insertAll(exercises: List<ExerciseEntity>) {
        this.exercises.value = this.exercises.value + exercises
    }

    override suspend fun insertInstructions(instructions: List<ExerciseInstructionEntity>) {
        this.instructions += instructions
    }

    override suspend fun count(): Int = exercises.value.size

    override fun observeAll(): Flow<List<ExerciseEntity>> =
        exercises.map { list -> list.sortedBy { it.name } }

    override fun search(
        bodyPart: String?,
        equipment: String?,
        target: String?,
        query: String?,
    ): Flow<List<ExerciseEntity>> = exercises.map { list ->
        list.filter { ex ->
            (bodyPart == null || ex.bodyPart == bodyPart) &&
                (equipment == null || ex.equipment == equipment) &&
                (target == null || ex.target == target) &&
                (query == null || ex.name.contains(query, ignoreCase = true))
        }.sortedBy { it.name }
    }

    override suspend fun getById(id: String): ExerciseEntity? =
        exercises.value.firstOrNull { it.id == id }

    override fun observeById(id: String): Flow<ExerciseEntity?> =
        exercises.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getByName(name: String): ExerciseEntity? =
        exercises.value.firstOrNull { it.name == name }

    override fun observeFavorites(): Flow<List<ExerciseEntity>> =
        exercises.map { list -> list.filter { it.isFavorite }.sortedBy { it.name } }

    override suspend fun getInstructions(exerciseId: String): List<ExerciseInstructionEntity> =
        instructions.filter { it.exerciseId == exerciseId }

    override fun observeBodyParts(): Flow<List<String>> =
        exercises.map { list -> list.map { it.bodyPart }.distinct().sorted() }

    override fun observeEquipment(): Flow<List<String>> =
        exercises.map { list -> list.map { it.equipment }.distinct().sorted() }

    override fun observeTargets(): Flow<List<String>> =
        exercises.map { list -> list.map { it.target }.distinct().sorted() }

    override suspend fun setFavorite(id: String, favorite: Boolean) {
        exercises.value = exercises.value.map {
            if (it.id == id) it.copy(isFavorite = favorite) else it
        }
    }

    override suspend fun update(exercise: ExerciseEntity) {
        exercises.value = exercises.value.map { if (it.id == exercise.id) exercise else it }
    }
}

fun exercise(
    id: String,
    name: String,
    bodyPart: String = "chest",
    equipment: String = "barbell",
    target: String = "pectorals",
    isFavorite: Boolean = false,
) = ExerciseEntity(
    id = id,
    name = name,
    bodyPart = bodyPart,
    equipment = equipment,
    muscleGroup = target,
    secondaryMuscles = emptyList(),
    target = target,
    mediaId = null,
    createdAt = "2026-01-01T00:00:00Z",
    isFavorite = isFavorite,
)
