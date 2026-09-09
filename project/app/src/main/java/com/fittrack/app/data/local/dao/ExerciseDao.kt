package com.fittrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fittrack.app.data.local.entity.ExerciseEntity
import com.fittrack.app.data.local.entity.ExerciseInstructionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstructions(instructions: List<ExerciseInstructionEntity>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT * FROM exercises
        WHERE (:bodyPart IS NULL OR bodyPart = :bodyPart)
          AND (:equipment IS NULL OR equipment = :equipment)
          AND (:target IS NULL OR target = :target)
          AND (:query IS NULL OR name LIKE '%' || :query || '%')
        ORDER BY name ASC
        """
    )
    fun search(
        bodyPart: String?,
        equipment: String?,
        target: String?,
        query: String?,
    ): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeById(id: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE isFavorite = 1 ORDER BY name ASC")
    fun observeFavorites(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise_instructions WHERE exerciseId = :exerciseId")
    suspend fun getInstructions(exerciseId: String): List<ExerciseInstructionEntity>

    @Query("SELECT DISTINCT bodyPart FROM exercises ORDER BY bodyPart ASC")
    fun observeBodyParts(): Flow<List<String>>

    @Query("SELECT DISTINCT equipment FROM exercises ORDER BY equipment ASC")
    fun observeEquipment(): Flow<List<String>>

    @Query("SELECT DISTINCT target FROM exercises ORDER BY target ASC")
    fun observeTargets(): Flow<List<String>>

    @Query("UPDATE exercises SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Update
    suspend fun update(exercise: ExerciseEntity)
}
