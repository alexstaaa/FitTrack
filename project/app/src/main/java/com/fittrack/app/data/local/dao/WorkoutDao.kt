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
import com.fittrack.app.data.local.entity.LoggedSetEntity
import com.fittrack.app.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

/** A completed session plus aggregate counts, for history lists. */
data class SessionSummary(
    @Embedded val session: WorkoutSessionEntity,
    val setCount: Int,
    val exerciseCount: Int,
)

/** A logged set joined with its exercise (for the summary screen). */
data class LoggedSetWithExercise(
    @Embedded val loggedSet: LoggedSetEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
)

/** One point on a per-exercise progress chart: one completed session. */
data class ExerciseProgressPoint(
    val date: Long,
    val maxWeight: Double,
    val totalVolume: Double,
)

@Dao
interface WorkoutDao {

    // --- Sessions ---

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Delete
    suspend fun deleteSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSession(id: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<WorkoutSessionEntity?>

    @Query(
        """
        SELECT s.*, COUNT(ls.id) AS setCount, COUNT(DISTINCT ls.exerciseId) AS exerciseCount
        FROM workout_sessions s
        LEFT JOIN logged_sets ls ON ls.sessionId = s.id
        WHERE s.endedAt IS NOT NULL
        GROUP BY s.id
        ORDER BY s.startedAt DESC
        """
    )
    fun observeCompletedSessionSummaries(): Flow<List<SessionSummary>>

    @Query("SELECT * FROM workout_sessions WHERE endedAt IS NOT NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLastCompletedSession(): WorkoutSessionEntity?

    @Query("SELECT startedAt FROM workout_sessions WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeCompletedSessionStartTimes(): Flow<List<Long>>

    // --- Sets ---

    @Insert
    suspend fun insertSet(set: LoggedSetEntity): Long

    @Update
    suspend fun updateSet(set: LoggedSetEntity)

    @Query("DELETE FROM logged_sets WHERE id = :id")
    suspend fun deleteSetById(id: Long)

    @Query("SELECT * FROM logged_sets WHERE sessionId = :sessionId ORDER BY exerciseOrder ASC, setNumber ASC")
    suspend fun getSetsForSession(sessionId: Long): List<LoggedSetEntity>

    @Transaction
    @Query("SELECT * FROM logged_sets WHERE sessionId = :sessionId ORDER BY exerciseOrder ASC, setNumber ASC")
    suspend fun getSetsWithExercises(sessionId: Long): List<LoggedSetWithExercise>

    @Query("SELECT COALESCE(SUM(weight * reps), 0) FROM logged_sets WHERE sessionId = :sessionId")
    suspend fun getSessionVolume(sessionId: Long): Double

    /**
     * The sets performed for [exerciseId] in the most recent *completed*
     * session that included it — used to suggest weight/reps.
     */
    @Query(
        """
        SELECT * FROM logged_sets
        WHERE exerciseId = :exerciseId
          AND sessionId = (
            SELECT s.id FROM workout_sessions s
            JOIN logged_sets ls ON ls.sessionId = s.id
            WHERE ls.exerciseId = :exerciseId AND s.endedAt IS NOT NULL
            ORDER BY s.startedAt DESC LIMIT 1
          )
        ORDER BY setNumber ASC
        """
    )
    suspend fun getLastSetsForExercise(exerciseId: String): List<LoggedSetEntity>

    // --- Progress ---

    @Query(
        """
        SELECT s.startedAt AS date,
               MAX(ls.weight) AS maxWeight,
               SUM(ls.weight * ls.reps) AS totalVolume
        FROM logged_sets ls
        JOIN workout_sessions s ON s.id = ls.sessionId
        WHERE ls.exerciseId = :exerciseId AND s.endedAt IS NOT NULL
        GROUP BY s.id
        ORDER BY s.startedAt ASC
        """
    )
    fun observeExerciseProgress(exerciseId: String): Flow<List<ExerciseProgressPoint>>

    /** Exercises that have at least one logged set (for the progress picker). */
    @Query(
        """
        SELECT DISTINCT e.* FROM exercises e
        JOIN logged_sets ls ON ls.exerciseId = e.id
        ORDER BY e.name ASC
        """
    )
    fun observeLoggedExercises(): Flow<List<ExerciseEntity>>
}
