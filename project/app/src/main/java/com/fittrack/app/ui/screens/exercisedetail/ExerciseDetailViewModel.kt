package com.fittrack.app.ui.screens.exercisedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.entity.ExerciseEntity
import com.fittrack.app.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ExerciseDetailUiState {
    data object Loading : ExerciseDetailUiState
    data object NotFound : ExerciseDetailUiState
    data class Content(
        val instructions: String?,
        val instructionsLocale: String?,
    ) : ExerciseDetailUiState
}

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val exerciseId: String = checkNotNull(savedStateHandle["exerciseId"])

    /** Live exercise row — keeps the favorite icon in sync. */
    val exercise: StateFlow<ExerciseEntity?> = exerciseRepository.observeById(exerciseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow<ExerciseDetailUiState>(ExerciseDetailUiState.Loading)
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val detail = exerciseRepository.getDetail(exerciseId)
            _uiState.value = if (detail == null) {
                ExerciseDetailUiState.NotFound
            } else {
                ExerciseDetailUiState.Content(detail.instructions, detail.instructionsLocale)
            }
        }
    }

    fun toggleFavorite() {
        val current = exercise.value ?: return
        viewModelScope.launch {
            exerciseRepository.setFavorite(current.id, !current.isFavorite)
        }
    }
}
