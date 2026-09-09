package com.fittrack.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fittrack.app.data.local.converter.Converters
import com.fittrack.app.data.local.dao.BodyStatDao
import com.fittrack.app.data.local.dao.ExerciseDao
import com.fittrack.app.data.local.dao.PlanDao
import com.fittrack.app.data.local.dao.WorkoutDao
import com.fittrack.app.data.local.entity.BodyStatEntity
import com.fittrack.app.data.local.entity.ExerciseEntity
import com.fittrack.app.data.local.entity.ExerciseInstructionEntity
import com.fittrack.app.data.local.entity.LoggedSetEntity
import com.fittrack.app.data.local.entity.PlanExerciseEntity
import com.fittrack.app.data.local.entity.WorkoutPlanEntity
import com.fittrack.app.data.local.entity.WorkoutSessionEntity

@Database(
    entities = [
        ExerciseEntity::class,
        ExerciseInstructionEntity::class,
        WorkoutPlanEntity::class,
        PlanExerciseEntity::class,
        WorkoutSessionEntity::class,
        LoggedSetEntity::class,
        BodyStatEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FitTrackDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun planDao(): PlanDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun bodyStatDao(): BodyStatDao

    companion object {
        const val DATABASE_NAME = "fittrack.db"
    }
}
