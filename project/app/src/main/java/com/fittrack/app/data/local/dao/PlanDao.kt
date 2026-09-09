package com.fittrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.fittrack.app.data.local.entity.ExerciseEntity
import com.fittrack.app.data.local.entity.PlanExerciseEntity
import com.fittrack.app.data.local.entity.WorkoutPlanEntity
import kotlinx.coroutines.flow.Flow

/** A plan row plus card metadata (count, thumbnail source, duration estimate). */
data class PlanSummary(
    @Embedded val plan: WorkoutPlanEntity,
    val exerciseCount: Int,
    /** First exercise in the plan — used for the card thumbnail. */
    val firstExerciseId: String?,
    /** Rough duration: sets × (rest + ~4s per rep), summed, in minutes. */
    val estimatedMinutes: Int,
)

/** A plan exercise slot joined with the full exercise record. */
data class PlanExerciseWithExercise(
    @Embedded val planExercise: PlanExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
)

@Dao
interface PlanDao {

    @Query(
        """
        SELECT p.*, COUNT(pe.id) AS exerciseCount,
               (SELECT pe2.exerciseId FROM plan_exercises pe2
                WHERE pe2.planId = p.id ORDER BY pe2.position ASC LIMIT 1) AS firstExerciseId,
               COALESCE(SUM(pe.targetSets * (pe.restSeconds + pe.targetReps * 4)) / 60, 0) AS estimatedMinutes
        FROM workout_plans p
        LEFT JOIN plan_exercises pe ON pe.planId = p.id
        GROUP BY p.id
        ORDER BY p.createdAt DESC
        """
    )
    fun observePlanSummaries(): Flow<List<PlanSummary>>

    @Query("SELECT * FROM workout_plans WHERE id = :id")
    suspend fun getPlan(id: Long): WorkoutPlanEntity?

    @Query("SELECT * FROM workout_plans WHERE id = :id")
    fun observePlan(id: Long): Flow<WorkoutPlanEntity?>

    @Transaction
    @Query("SELECT * FROM plan_exercises WHERE planId = :planId ORDER BY position ASC")
    fun observePlanExercises(planId: Long): Flow<List<PlanExerciseWithExercise>>

    @Transaction
    @Query("SELECT * FROM plan_exercises WHERE planId = :planId ORDER BY position ASC")
    suspend fun getPlanExercises(planId: Long): List<PlanExerciseWithExercise>

    @Query("SELECT COUNT(*) FROM workout_plans")
    suspend fun count(): Int

    @Insert
    suspend fun insertPlan(plan: WorkoutPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: WorkoutPlanEntity)

    @Delete
    suspend fun deletePlan(plan: WorkoutPlanEntity)

    @Query("DELETE FROM workout_plans WHERE id = :id")
    suspend fun deletePlanById(id: Long)

    @Insert
    suspend fun insertPlanExercise(planExercise: PlanExerciseEntity): Long

    @Insert
    suspend fun insertPlanExercises(planExercises: List<PlanExerciseEntity>)

    @Update
    suspend fun updatePlanExercise(planExercise: PlanExerciseEntity)

    @Update
    suspend fun updatePlanExercises(planExercises: List<PlanExerciseEntity>)

    @Query("DELETE FROM plan_exercises WHERE id = :id")
    suspend fun deletePlanExerciseById(id: Long)
}
