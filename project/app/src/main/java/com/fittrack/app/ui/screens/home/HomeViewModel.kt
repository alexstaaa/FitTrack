package com.fittrack.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.dao.PlanSummary
import com.fittrack.app.data.local.entity.WorkoutSessionEntity
import com.fittrack.app.data.repository.PlanRepository
import com.fittrack.app.data.repository.WorkoutRepository
import com.fittrack.app.domain.StreakCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

/** One day cell in the Mon–Sun week strip on Home. */
data class WeekDay(
    val label: String,       // "M", "T", …
    val dayOfMonth: Int,
    val done: Boolean,       // has a completed workout
    val isToday: Boolean,
    val isPast: Boolean,     // earlier than today (missed if !done)
)

data class HomeUiState(
    val loading: Boolean = true,
    val workoutsThisWeek: Int = 0,
    val weeklyStreak: Int = 0,
    val lastWorkout: WorkoutSessionEntity? = null,
    val plans: List<PlanSummary> = emptyList(),
    val weekDays: List<WeekDay> = emptyList(),
    /** Workouts per ISO week, oldest first, current week last (8 weeks). */
    val workoutsPerWeek: List<Int> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    planRepository: PlanRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        workoutRepository.observeCompletedSessionStartTimes(),
        workoutRepository.observeCompletedSessionSummaries().map { it.firstOrNull()?.session },
        planRepository.observePlanSummaries(),
    ) { startTimes, lastWorkout, plans ->
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        HomeUiState(
            loading = false,
            workoutsThisWeek = StreakCalculator.workoutsThisWeek(startTimes, now, zone),
            weeklyStreak = StreakCalculator.weeklyStreak(startTimes, now, zone),
            lastWorkout = lastWorkout,
            plans = plans,
            weekDays = buildWeekStrip(startTimes, now, zone),
            workoutsPerWeek = StreakCalculator.workoutsPerWeek(startTimes, now, zone, weeks = 8),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun buildWeekStrip(startTimes: List<Long>, now: Long, zone: ZoneId): List<WeekDay> {
        val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val monday = today.with(java.time.DayOfWeek.MONDAY)
        val workoutDates = startTimes
            .map { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            .toHashSet()
        val labels = listOf("M", "T", "W", "T", "F", "S", "S")
        return (0..6).map { offset ->
            val date = monday.plusDays(offset.toLong())
            WeekDay(
                label = labels[offset],
                dayOfMonth = date.dayOfMonth,
                done = date in workoutDates,
                isToday = date == today,
                isPast = date.isBefore(today),
            )
        }
    }

    fun startEmptyWorkout(onStarted: (sessionId: Long) -> Unit) {
        viewModelScope.launch {
            onStarted(workoutRepository.startEmptySession())
        }
    }

    fun startPlanWorkout(planId: Long, onStarted: (sessionId: Long) -> Unit) {
        viewModelScope.launch {
            onStarted(workoutRepository.startSessionFromPlan(planId))
        }
    }

    /**
     * Repeats the last completed workout: from its plan when it had one,
     * otherwise as an ad-hoc copy of its exercises/sets.
     */
    fun repeatLastWorkout(onStarted: (sessionId: Long, copyFromSessionId: Long?) -> Unit) {
        viewModelScope.launch {
            val last = workoutRepository.getLastCompletedSession() ?: return@launch
            val planId = last.planId
            if (planId != null) {
                onStarted(workoutRepository.startSessionFromPlan(planId), null)
            } else {
                onStarted(workoutRepository.startEmptySession(last.name), last.id)
            }
        }
    }
}
