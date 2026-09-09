package com.fittrack.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.entity.ExerciseEntity
import com.fittrack.app.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Active library filters; null = "any". */
data class LibraryFilters(
    val bodyPart: String? = null,
    val equipment: String? = null,
    val target: String? = null,
    val favoritesOnly: Boolean = false,
)

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Content(val exercises: List<ExerciseEntity>) : LibraryUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filters = MutableStateFlow(LibraryFilters())
    val filters: StateFlow<LibraryFilters> = _filters.asStateFlow()

    val bodyParts: StateFlow<List<String>> = exerciseRepository.observeBodyParts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val equipment: StateFlow<List<String>> = exerciseRepository.observeEquipment()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val targets: StateFlow<List<String>> = exerciseRepository.observeTargets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<LibraryUiState> =
        combine(_query, _filters) { query, filters -> query to filters }
            .flatMapLatest { (query, filters) ->
                exerciseRepository
                    .search(
                        bodyPart = filters.bodyPart,
                        equipment = filters.equipment,
                        target = filters.target,
                        query = query.trim().ifBlank { null },
                    )
                    .map { list ->
                        val visible = if (filters.favoritesOnly) list.filter { it.isFavorite } else list
                        LibraryUiState.Content(visible) as LibraryUiState
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState.Loading)

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onBodyPartSelected(value: String?) = _filters.update { it.copy(bodyPart = value) }

    fun onEquipmentSelected(value: String?) = _filters.update { it.copy(equipment = value) }

    fun onTargetSelected(value: String?) = _filters.update { it.copy(target = value) }

    fun onFavoritesOnlyToggled() = _filters.update { it.copy(favoritesOnly = !it.favoritesOnly) }

    fun onClearFilters() {
        _filters.value = LibraryFilters()
    }

    fun toggleFavorite(exercise: ExerciseEntity) {
        viewModelScope.launch {
            exerciseRepository.setFavorite(exercise.id, !exercise.isFavorite)
        }
    }
}
