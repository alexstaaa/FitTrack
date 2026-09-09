package com.fittrack.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the bottom bar's center "+" button: starts an ad-hoc workout. */
@HiltViewModel
class QuickStartViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    fun startEmptyWorkout(onStarted: (sessionId: Long) -> Unit) {
        viewModelScope.launch {
            onStarted(workoutRepository.startEmptySession())
        }
    }
}
