package com.fittrack.app.di

import android.content.Context
import androidx.room.Room
import com.fittrack.app.data.local.FitTrackDatabase
import com.fittrack.app.data.local.MIGRATION_1_2
import com.fittrack.app.data.local.dao.BodyStatDao
import com.fittrack.app.data.local.dao.ExerciseDao
import com.fittrack.app.data.local.dao.PlanDao
import com.fittrack.app.data.local.dao.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FitTrackDatabase =
        Room.databaseBuilder(context, FitTrackDatabase::class.java, FitTrackDatabase.DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideExerciseDao(database: FitTrackDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun providePlanDao(database: FitTrackDatabase): PlanDao = database.planDao()

    @Provides
    fun provideWorkoutDao(database: FitTrackDatabase): WorkoutDao = database.workoutDao()

    @Provides
    fun provideBodyStatDao(database: FitTrackDatabase): BodyStatDao = database.bodyStatDao()
}
