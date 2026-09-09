package com.fittrack.app.data.seed

import android.util.Log
import com.fittrack.app.data.local.dao.ExerciseDao
import com.fittrack.app.data.local.dao.PlanDao
import com.fittrack.app.data.local.entity.PlanExerciseEntity
import com.fittrack.app.data.local.entity.WorkoutPlanEntity
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "StarterPlanSeeder"

/**
 * Seeds a few pre-built plans (push/pull/legs + full body) on first launch,
 * after the exercise dataset has been imported. Exercises are looked up by
 * name; any name missing from the dataset is skipped rather than failing.
 */
@Singleton
class StarterPlanSeeder @Inject constructor(
    private val planDao: PlanDao,
    private val exerciseDao: ExerciseDao,
) {
    private data class StarterExercise(
        val name: String,
        val sets: Int = 3,
        val reps: Int = 10,
        val restSeconds: Int = 90,
    )

    private data class StarterPlan(
        val name: String,
        val description: String,
        val exercises: List<StarterExercise>,
    )

    private val starterPlans = listOf(
        StarterPlan(
            name = "Push Day",
            description = "Chest, shoulders and triceps.",
            exercises = listOf(
                StarterExercise("barbell bench press", sets = 4, reps = 8, restSeconds = 120),
                StarterExercise("dumbbell seated shoulder press", sets = 3, reps = 10),
                StarterExercise("dumbbell fly", sets = 3, reps = 12),
                StarterExercise("dumbbell lateral raise", sets = 3, reps = 15, restSeconds = 60),
                StarterExercise("cable pushdown", sets = 3, reps = 12, restSeconds = 60),
                StarterExercise("triceps dip", sets = 3, reps = 10),
            ),
        ),
        StarterPlan(
            name = "Pull Day",
            description = "Back and biceps.",
            exercises = listOf(
                StarterExercise("barbell deadlift", sets = 3, reps = 5, restSeconds = 180),
                StarterExercise("pull-up", sets = 3, reps = 8, restSeconds = 120),
                StarterExercise("barbell bent over row", sets = 4, reps = 8, restSeconds = 120),
                StarterExercise("cable pulldown", sets = 3, reps = 10),
                StarterExercise("barbell curl", sets = 3, reps = 10, restSeconds = 60),
                StarterExercise("dumbbell biceps curl", sets = 3, reps = 12, restSeconds = 60),
            ),
        ),
        StarterPlan(
            name = "Leg Day",
            description = "Quads, hamstrings, glutes and calves.",
            exercises = listOf(
                StarterExercise("barbell full squat", sets = 4, reps = 6, restSeconds = 180),
                StarterExercise("barbell romanian deadlift", sets = 3, reps = 8, restSeconds = 120),
                StarterExercise("dumbbell lunge", sets = 3, reps = 10),
                StarterExercise("barbell standing leg calf raise", sets = 4, reps = 15, restSeconds = 60),
                StarterExercise("hanging leg raise", sets = 3, reps = 12, restSeconds = 60),
            ),
        ),
        StarterPlan(
            name = "Full Body",
            description = "One-session full-body routine, 3x per week.",
            exercises = listOf(
                StarterExercise("barbell full squat", sets = 3, reps = 8, restSeconds = 150),
                StarterExercise("barbell bench press", sets = 3, reps = 8, restSeconds = 120),
                StarterExercise("barbell bent over row", sets = 3, reps = 8, restSeconds = 120),
                StarterExercise("dumbbell standing overhead press", sets = 3, reps = 10),
                StarterExercise("dumbbell romanian deadlift", sets = 3, reps = 10),
                StarterExercise("3/4 sit-up", sets = 3, reps = 15, restSeconds = 60),
            ),
        ),
    )

    suspend fun seedIfEmpty() {
        if (planDao.count() > 0) return
        for (starter in starterPlans) {
            val planId = planDao.insertPlan(
                WorkoutPlanEntity(
                    name = starter.name,
                    description = starter.description,
                    isStarter = true,
                    createdAt = System.currentTimeMillis(),
                )
            )
            val slots = starter.exercises.mapNotNull { spec ->
                exerciseDao.getByName(spec.name)?.let { exercise -> spec to exercise }
            }.mapIndexed { index, (spec, exercise) ->
                PlanExerciseEntity(
                    planId = planId,
                    exerciseId = exercise.id,
                    position = index,
                    targetSets = spec.sets,
                    targetReps = spec.reps,
                    restSeconds = spec.restSeconds,
                )
            }
            planDao.insertPlanExercises(slots)
            Log.i(TAG, "Seeded starter plan '${starter.name}' with ${slots.size} exercises")
        }
    }
}
