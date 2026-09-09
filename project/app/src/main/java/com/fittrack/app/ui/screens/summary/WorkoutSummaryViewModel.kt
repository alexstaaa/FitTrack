package com.fittrack.app.ui.screens.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.dao.LoggedSetWithExercise
import com.fittrack.app.data.local.entity.WorkoutSessionEntity
import com.fittrack.app.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Per-exercise group of logged sets for the summary list. */
data class SummaryExercise(
    val exerciseId: String,
    val exerciseName: String,
    val bodyPart: String,
    val sets: List<LoggedSetWithExercise>,
)

sealed interface SummaryUiState {
    data object Loading : SummaryUiState
    data object NotFound : SummaryUiState
    data class Content(
        val session: WorkoutSessionEntity,
        val exercises: List<SummaryExercise>,
    ) : SummaryUiState
}

@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow<SummaryUiState>(SummaryUiState.Loading)
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = workoutRepository.getSession(sessionId)
            if (session == null) {
                _uiState.value = SummaryUiState.NotFound
                return@launch
            }
            val exercises = workoutRepository.getSetsWithExercises(sessionId)
                .groupBy { it.exercise.id }
                .map { (_, sets) ->
                    val exercise = sets.first().exercise
                    SummaryExercise(
                        exerciseId = exercise.id,
                        exerciseName = exercise.name,
                        bodyPart = exercise.bodyPart,
                        sets = sets,
                    )
                }
            _uiState.value = SummaryUiState.Content(session, exercises)
        }
    }

    fun deleteSession(onDeleted: () -> Unit) {
        viewModelScope.launch {
            workoutRepository.discardSession(sessionId)
            onDeleted()
        }
    }
}
