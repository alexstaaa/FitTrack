package com.fittrack.app.ui.screens.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.dao.PlanSummary
import com.fittrack.app.data.repository.PlanRepository
import com.fittrack.app.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PlansUiState {
    data object Loading : PlansUiState
    data class Content(val plans: List<PlanSummary>) : PlansUiState
}

@HiltViewModel
class PlansViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    val uiState: StateFlow<PlansUiState> = planRepository.observePlanSummaries()
        .map { PlansUiState.Content(it) as PlansUiState }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlansUiState.Loading)

    /** Creates an empty plan and hands the new id to the caller for navigation. */
    fun createPlan(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(planRepository.createPlan("New Plan"))
        }
    }

    fun deletePlan(planId: Long) {
        viewModelScope.launch { planRepository.deletePlan(planId) }
    }

    /** Starts a workout session from this plan and navigates via [onStarted]. */
    fun startWorkout(planId: Long, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            onStarted(workoutRepository.startSessionFromPlan(planId))
        }
    }
}
