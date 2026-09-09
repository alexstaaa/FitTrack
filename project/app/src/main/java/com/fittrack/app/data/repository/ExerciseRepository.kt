package com.fittrack.app.data.repository

import com.fittrack.app.data.local.dao.ExerciseDao
import com.fittrack.app.data.local.entity.ExerciseEntity
import com.fittrack.app.domain.model.ExerciseDetail
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val FALLBACK_LOCALE = "en"

@Singleton
class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
) {
    fun search(
        bodyPart: String?,
        equipment: String?,
        target: String?,
        query: String?,
    ): Flow<List<ExerciseEntity>> = exerciseDao.search(bodyPart, equipment, target, query)

    fun observeById(id: String): Flow<ExerciseEntity?> = exerciseDao.observeById(id)

    fun observeBodyParts(): Flow<List<String>> = exerciseDao.observeBodyParts()

    fun observeEquipment(): Flow<List<String>> = exerciseDao.observeEquipment()

    fun observeTargets(): Flow<List<String>> = exerciseDao.observeTargets()

    suspend fun setFavorite(id: String, favorite: Boolean) = exerciseDao.setFavorite(id, favorite)

    /**
     * Loads an exercise with its instructions resolved to [locale]
     * (device language by default), falling back to English, then to any
     * available translation. Fully offline — instructions live in Room.
     */
    suspend fun getDetail(
        id: String,
        locale: String = Locale.getDefault().language,
    ): ExerciseDetail? {
        val exercise = exerciseDao.getById(id) ?: return null
        val instructions = exerciseDao.getInstructions(id)
        val resolved = instructions.firstOrNull { it.locale == locale }
            ?: instructions.firstOrNull { it.locale == FALLBACK_LOCALE }
            ?: instructions.firstOrNull()
        return ExerciseDetail(
            exercise = exercise,
            instructions = resolved?.text,
            instructionsLocale = resolved?.locale,
        )
    }
}
