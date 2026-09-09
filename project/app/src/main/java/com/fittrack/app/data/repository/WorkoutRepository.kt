package com.fittrack.app.data.repository

import com.fittrack.app.data.local.dao.ExerciseProgressPoint
import com.fittrack.app.data.local.dao.LoggedSetWithExercise
import com.fittrack.app.data.local.dao.PlanDao
import com.fittrack.app.data.local.dao.SessionSummary
import com.fittrack.app.data.local.dao.WorkoutDao
import com.fittrack.app.data.local.entity.ExerciseEntity
import com.fittrack.app.data.local.entity.LoggedSetEntity
import com.fittrack.app.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** What the active-workout screen needs to render one plan exercise slot. */
data class PlanBlock(
    val exerciseId: String,
    val name: String,
    val bodyPart: String,
    val mediaId: String?,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeight: Double?,
    val restSeconds: Int,
)

@Singleton
class WorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val planDao: PlanDao,
) {
    // --- Session lifecycle ---

    /** Starts a workout from a plan; falls back to an ad-hoc session if the plan is gone. */
    suspend fun startSessionFromPlan(planId: Long): Long {
        val plan = planDao.getPlan(planId)
        return workoutDao.insertSession(
            WorkoutSessionEntity(
                planId = plan?.id,
                name = plan?.name ?: "Workout",
                startedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun startEmptySession(name: String = "Workout"): Long =
        workoutDao.insertSession(
            WorkoutSessionEntity(name = name, startedAt = System.currentTimeMillis())
        )

    /**
     * Marks the session complete, stamping duration and total volume
     * (sum of weight × reps across all logged sets).
     */
    suspend fun completeSession(sessionId: Long, note: String = "") {
        val session = workoutDao.getSession(sessionId) ?: return
        val volume = workoutDao.getSessionVolume(sessionId)
        workoutDao.updateSession(
            session.copy(
                endedAt = System.currentTimeMillis(),
                totalVolume = volume,
                note = note,
            )
        )
    }

    /** Discards a session (e.g. cancelled workout); logged sets cascade-delete. */
    suspend fun discardSession(sessionId: Long) {
        workoutDao.getSession(sessionId)?.let { workoutDao.deleteSession(it) }
    }

    suspend fun getSession(id: Long): WorkoutSessionEntity? = workoutDao.getSession(id)

    fun observeSession(id: Long): Flow<WorkoutSessionEntity?> = workoutDao.observeSession(id)

    fun observeCompletedSessionSummaries(): Flow<List<SessionSummary>> =
        workoutDao.observeCompletedSessionSummaries()

    fun observeCompletedSessionStartTimes(): Flow<List<Long>> =
        workoutDao.observeCompletedSessionStartTimes()

    suspend fun getLastCompletedSession(): WorkoutSessionEntity? =
        workoutDao.getLastCompletedSession()

    /** Plan slots in workout-ready form (empty if the plan was deleted). */
    suspend fun getPlanBlocks(planId: Long): List<PlanBlock> =
        planDao.getPlanExercises(planId).map { slot ->
            PlanBlock(
                exerciseId = slot.exercise.id,
                name = slot.exercise.name,
                bodyPart = slot.exercise.bodyPart,
                mediaId = slot.exercise.mediaId,
                targetSets = slot.planExercise.targetSets,
                targetReps = slot.planExercise.targetReps,
                targetWeight = slot.planExercise.targetWeight,
                restSeconds = slot.planExercise.restSeconds,
            )
        }

    // --- Sets ---

    suspend fun logSet(set: LoggedSetEntity): Long = workoutDao.insertSet(set)

    suspend fun deleteSet(setId: Long) = workoutDao.deleteSetById(setId)

    suspend fun getSetsForSession(sessionId: Long): List<LoggedSetEntity> =
        workoutDao.getSetsForSession(sessionId)

    suspend fun getSetsWithExercises(sessionId: Long): List<LoggedSetWithExercise> =
        workoutDao.getSetsWithExercises(sessionId)

    /** Last completed session's sets for an exercise — drives weight/reps suggestions. */
    suspend fun getLastSetsForExercise(exerciseId: String): List<LoggedSetEntity> =
        workoutDao.getLastSetsForExercise(exerciseId)

    // --- Progress ---

    fun observeExerciseProgress(exerciseId: String): Flow<List<ExerciseProgressPoint>> =
        workoutDao.observeExerciseProgress(exerciseId)

    fun observeLoggedExercises(): Flow<List<ExerciseEntity>> = workoutDao.observeLoggedExercises()
}
