package com.fittrack.app.ui.screens.planeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.dao.PlanExerciseWithExercise
import com.fittrack.app.data.local.entity.ExerciseEntity
import com.fittrack.app.data.local.entity.PlanExerciseEntity
import com.fittrack.app.data.local.entity.WorkoutPlanEntity
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.PlanRepository
import com.fittrack.app.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlanEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val planRepository: PlanRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val planId: Long = checkNotNull(savedStateHandle["planId"])

    val plan: StateFlow<WorkoutPlanEntity?> = planRepository.observePlan(planId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val planExercises: StateFlow<List<PlanExerciseWithExercise>?> =
        planRepository.observePlanExercises(planId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // --- Exercise picker (add-exercise bottom sheet) ---

    private val _pickerQuery = MutableStateFlow("")
    val pickerQuery: StateFlow<String> = _pickerQuery.asStateFlow()

    val pickerResults: StateFlow<List<ExerciseEntity>> = _pickerQuery
        .flatMapLatest { query ->
            exerciseRepository.search(null, null, null, query.trim().ifBlank { null })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onPickerQueryChange(value: String) {
        _pickerQuery.value = value
    }

    // --- Editing ---

    fun renamePlan(name: String) {
        val current = plan.value ?: return
        if (name.isBlank() || name == current.name) return
        viewModelScope.launch { planRepository.renamePlan(current, name) }
    }

    fun addExercise(exercise: ExerciseEntity) {
        viewModelScope.launch { planRepository.addExercise(planId, exercise.id) }
    }

    fun removeExercise(planExerciseId: Long) {
        viewModelScope.launch { planRepository.removeExercise(planId, planExerciseId) }
    }

    fun moveExercise(from: Int, to: Int) {
        viewModelScope.launch { planRepository.moveExercise(planId, from, to) }
    }

    fun updateTargets(slot: PlanExerciseEntity, sets: Int, reps: Int, weight: Double?, restSeconds: Int) {
        viewModelScope.launch {
            planRepository.updatePlanExercise(
                slot.copy(
                    targetSets = sets.coerceIn(1, 20),
                    targetReps = reps.coerceIn(1, 100),
                    targetWeight = weight,
                    restSeconds = restSeconds.coerceIn(0, 600),
                )
            )
        }
    }

    fun startWorkout(onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            onStarted(workoutRepository.startSessionFromPlan(planId))
        }
    }
}
