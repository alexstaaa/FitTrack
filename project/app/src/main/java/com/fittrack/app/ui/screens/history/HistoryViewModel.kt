package com.fittrack.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.dao.SessionSummary
import com.fittrack.app.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    /** Sessions grouped by month, newest first. */
    data class Content(val months: List<MonthGroup>) : HistoryUiState

    data class MonthGroup(val label: String, val sessions: List<SessionSummary>)
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    val uiState: StateFlow<HistoryUiState> = workoutRepository.observeCompletedSessionSummaries()
        .map { sessions ->
            val groups = sessions
                .groupBy { summary ->
                    YearMonth.from(
                        Instant.ofEpochMilli(summary.session.startedAt).atZone(ZoneId.systemDefault())
                    )
                }
                .map { (month, monthSessions) ->
                    HistoryUiState.MonthGroup(monthFormatter.format(month.atDay(1)), monthSessions)
                }
            HistoryUiState.Content(groups) as HistoryUiState
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)
}
