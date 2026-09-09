package com.fittrack.app.data.repository

import com.fittrack.app.data.local.dao.PlanDao
import com.fittrack.app.data.local.dao.PlanExerciseWithExercise
import com.fittrack.app.data.local.dao.PlanSummary
import com.fittrack.app.data.local.entity.PlanExerciseEntity
import com.fittrack.app.data.local.entity.WorkoutPlanEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanRepository @Inject constructor(
    private val planDao: PlanDao,
) {
    fun observePlanSummaries(): Flow<List<PlanSummary>> = planDao.observePlanSummaries()

    fun observePlan(id: Long): Flow<WorkoutPlanEntity?> = planDao.observePlan(id)

    fun observePlanExercises(planId: Long): Flow<List<PlanExerciseWithExercise>> =
        planDao.observePlanExercises(planId)

    suspend fun getPlan(id: Long): WorkoutPlanEntity? = planDao.getPlan(id)

    suspend fun getPlanExercises(planId: Long): List<PlanExerciseWithExercise> =
        planDao.getPlanExercises(planId)

    suspend fun createPlan(name: String): Long =
        planDao.insertPlan(WorkoutPlanEntity(name = name, createdAt = System.currentTimeMillis()))

    suspend fun renamePlan(plan: WorkoutPlanEntity, name: String) =
        planDao.updatePlan(plan.copy(name = name))

    suspend fun deletePlan(planId: Long) = planDao.deletePlanById(planId)

    /** Appends an exercise at the end of the plan with default targets. */
    suspend fun addExercise(planId: Long, exerciseId: String) {
        val position = planDao.getPlanExercises(planId).size
        planDao.insertPlanExercise(
            PlanExerciseEntity(planId = planId, exerciseId = exerciseId, position = position)
        )
    }

    suspend fun updatePlanExercise(planExercise: PlanExerciseEntity) =
        planDao.updatePlanExercise(planExercise)

    /** Removes a slot and compacts the remaining positions. */
    suspend fun removeExercise(planId: Long, planExerciseId: Long) {
        planDao.deletePlanExerciseById(planExerciseId)
        val remaining = planDao.getPlanExercises(planId).map { it.planExercise }
        planDao.updatePlanExercises(reindexed(remaining))
    }

    /** Persists a drag-and-drop move of the slot at [from] to [to] (0-based). */
    suspend fun moveExercise(planId: Long, from: Int, to: Int) {
        val current = planDao.getPlanExercises(planId).map { it.planExercise }.toMutableList()
        if (from !in current.indices || to !in current.indices || from == to) return
        current.add(to, current.removeAt(from))
        planDao.updatePlanExercises(reindexed(current))
    }

    private fun reindexed(slots: List<PlanExerciseEntity>): List<PlanExerciseEntity> =
        slots.mapIndexed { index, slot ->
            if (slot.position == index) slot else slot.copy(position = index)
        }
}
