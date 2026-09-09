package com.fittrack.app.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.dao.ExerciseProgressPoint
import com.fittrack.app.data.local.entity.BodyStatEntity
import com.fittrack.app.data.local.entity.ExerciseEntity
import com.fittrack.app.data.repository.BodyStatRepository
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.domain.StreakCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

enum class ProgressMetric { MaxWeight, Volume }

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val bodyStatRepository: BodyStatRepository,
) : ViewModel() {

    // --- Per-exercise progress chart ---

    /** Exercises with at least one logged set. */
    val loggedExercises: StateFlow<List<ExerciseEntity>> = workoutRepository.observeLoggedExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedExerciseId = MutableStateFlow<String?>(null)
    val selectedExerciseId: StateFlow<String?> = _selectedExerciseId.asStateFlow()

    private val _metric = MutableStateFlow(ProgressMetric.MaxWeight)
    val metric: StateFlow<ProgressMetric> = _metric.asStateFlow()

    val progressPoints: StateFlow<List<ExerciseProgressPoint>> = _selectedExerciseId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else workoutRepository.observeExerciseProgress(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectExercise(id: String) {
        _selectedExerciseId.value = id
    }

    fun setMetric(metric: ProgressMetric) {
        _metric.value = metric
    }

    // --- Body stats ---

    val bodyStats: StateFlow<List<BodyStatEntity>> = bodyStatRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addBodyStat(weightKg: Double, chestCm: Double?, waistCm: Double?, hipsCm: Double?) {
        viewModelScope.launch {
            bodyStatRepository.add(
                BodyStatEntity(
                    recordedAt = System.currentTimeMillis(),
                    weightKg = weightKg,
                    chestCm = chestCm,
                    waistCm = waistCm,
                    hipsCm = hipsCm,
                )
            )
        }
    }

    fun deleteBodyStat(id: Long) {
        viewModelScope.launch { bodyStatRepository.delete(id) }
    }

    // --- Consistency ---

    data class Consistency(
        val workoutsThisWeek: Int,
        val weeklyStreak: Int,
        val workoutsPerWeek: List<Int>,
    )

    val consistency: StateFlow<Consistency> = workoutRepository.observeCompletedSessionStartTimes()
        .map { times ->
            val now = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            Consistency(
                workoutsThisWeek = StreakCalculator.workoutsThisWeek(times, now, zone),
                weeklyStreak = StreakCalculator.weeklyStreak(times, now, zone),
                workoutsPerWeek = StreakCalculator.workoutsPerWeek(times, now, zone, weeks = 8),
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            Consistency(0, 0, emptyList()),
        )
}
