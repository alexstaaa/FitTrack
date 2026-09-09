package com.fittrack.app.ui.screens.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.local.dao.LoggedSetWithExercise
import com.fittrack.app.data.local.entity.ExerciseEntity
import com.fittrack.app.data.local.entity.LoggedSetEntity
import com.fittrack.app.data.local.entity.WorkoutSessionEntity
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One editable set row in the active workout. */
data class ActiveSetUi(
    val loggedSetId: Long? = null,
    val weight: String = "",
    val reps: String = "",
    val rpe: String = "",
    val completed: Boolean = false,
)

/** One exercise section in the active workout. */
data class ExerciseBlockUi(
    val exerciseId: String,
    val name: String,
    val bodyPart: String,
    val mediaId: String?,
    val restSeconds: Int = 90,
    val sets: List<ActiveSetUi> = emptyList(),
    /** e.g. "Last time: 3×10 @ 40 kg" — auto-fill source. */
    val lastSessionHint: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])
    private val copyFromSessionId: Long = savedStateHandle["copyFrom"] ?: -1L

    val session: StateFlow<WorkoutSessionEntity?> = workoutRepository.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _blocks = MutableStateFlow<List<ExerciseBlockUi>?>(null)
    val blocks: StateFlow<List<ExerciseBlockUi>?> = _blocks.asStateFlow()

    /** Seconds remaining on the rest timer, or null when no timer is running. */
    private val _restRemaining = MutableStateFlow<Int?>(null)
    val restRemaining: StateFlow<Int?> = _restRemaining.asStateFlow()

    /** Elapsed workout seconds, ticking. */
    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private var restJob: Job? = null

    // --- Exercise picker (ad-hoc add) ---

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

    init {
        viewModelScope.launch { buildInitialBlocks() }
        viewModelScope.launch {
            val started = workoutRepository.getSession(sessionId)?.startedAt ?: return@launch
            while (true) {
                _elapsedSeconds.value = (System.currentTimeMillis() - started) / 1000
                delay(1_000)
            }
        }
    }

    private suspend fun buildInitialBlocks() {
        val current = workoutRepository.getSession(sessionId)
        if (current == null) {
            _blocks.value = emptyList()
            return
        }
        val planId = current.planId
        val blocks = when {
            planId != null -> planBlocks(planId)
            copyFromSessionId > 0 -> copiedBlocks(copyFromSessionId)
            else -> emptyList()
        }
        // Re-apply sets already logged for this session (e.g. after process death).
        val logged = workoutRepository.getSetsWithExercises(sessionId)
        _blocks.value = mergeLogged(blocks, logged)
    }

    private suspend fun planBlocks(planId: Long): List<ExerciseBlockUi> =
        workoutRepository.getPlanBlocks(planId).map { slot ->
            val suggestion = workoutRepository.getLastSetsForExercise(slot.exerciseId)
            val prefillWeight = suggestion.lastOrNull()?.weight ?: slot.targetWeight
            val prefillReps = suggestion.lastOrNull()?.reps ?: slot.targetReps
            ExerciseBlockUi(
                exerciseId = slot.exerciseId,
                name = slot.name,
                bodyPart = slot.bodyPart,
                mediaId = slot.mediaId,
                restSeconds = slot.restSeconds,
                sets = List(slot.targetSets) {
                    ActiveSetUi(
                        weight = prefillWeight?.let { w -> formatNumber(w) } ?: "",
                        reps = prefillReps.toString(),
                    )
                },
                lastSessionHint = hintFrom(suggestion),
            )
        }

    private suspend fun copiedBlocks(sourceSessionId: Long): List<ExerciseBlockUi> {
        val sourceSets = workoutRepository.getSetsWithExercises(sourceSessionId)
        return sourceSets
            .groupBy { it.exercise.id }
            .map { (_, sets) ->
                val exercise = sets.first().exercise
                ExerciseBlockUi(
                    exerciseId = exercise.id,
                    name = exercise.name,
                    bodyPart = exercise.bodyPart,
                    mediaId = exercise.mediaId,
                    sets = sets.map { s ->
                        ActiveSetUi(
                            weight = formatNumber(s.loggedSet.weight),
                            reps = s.loggedSet.reps.toString(),
                        )
                    },
                    lastSessionHint = hintFrom(sets.map { it.loggedSet }),
                )
            }
    }

    private fun mergeLogged(
        blocks: List<ExerciseBlockUi>,
        logged: List<LoggedSetWithExercise>,
    ): List<ExerciseBlockUi> {
        if (logged.isEmpty()) return blocks
        var result = blocks
        val byExercise = logged.groupBy { it.exercise.id }
        for ((exerciseId, rows) in byExercise) {
            val completedRows = rows.map { row ->
                ActiveSetUi(
                    loggedSetId = row.loggedSet.id,
                    weight = formatNumber(row.loggedSet.weight),
                    reps = row.loggedSet.reps.toString(),
                    rpe = row.loggedSet.rpe?.toString() ?: "",
                    completed = true,
                )
            }
            val blockIndex = result.indexOfFirst { it.exerciseId == exerciseId }
            result = if (blockIndex >= 0) {
                val block = result[blockIndex]
                val remaining = block.sets.drop(completedRows.size)
                result.toMutableList().also {
                    it[blockIndex] = block.copy(sets = completedRows + remaining)
                }
            } else {
                // Ad-hoc exercise logged before a restart — rebuild its block.
                val exercise = rows.first().exercise
                result + ExerciseBlockUi(
                    exerciseId = exercise.id,
                    name = exercise.name,
                    bodyPart = exercise.bodyPart,
                    mediaId = exercise.mediaId,
                    sets = completedRows,
                )
            }
        }
        return result
    }

    private fun hintFrom(sets: List<LoggedSetEntity>): String? {
        if (sets.isEmpty()) return null
        val top = sets.maxByOrNull { it.weight } ?: return null
        return "Last time: ${sets.size}×${top.reps} @ ${formatNumber(top.weight)} kg"
    }

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

    // --- Set editing ---

    fun updateSet(blockIndex: Int, setIndex: Int, weight: String? = null, reps: String? = null, rpe: String? = null) {
        mutateSet(blockIndex, setIndex) { set ->
            set.copy(
                weight = weight ?: set.weight,
                reps = reps ?: set.reps,
                rpe = rpe ?: set.rpe,
            )
        }
    }

    fun addSet(blockIndex: Int) {
        _blocks.update { blocks ->
            blocks?.toMutableList()?.also { list ->
                val block = list.getOrNull(blockIndex) ?: return@also
                val template = block.sets.lastOrNull() ?: ActiveSetUi()
                list[blockIndex] = block.copy(
                    sets = block.sets + template.copy(loggedSetId = null, completed = false),
                )
            }
        }
    }

    fun removeSet(blockIndex: Int, setIndex: Int) {
        val set = _blocks.value?.getOrNull(blockIndex)?.sets?.getOrNull(setIndex) ?: return
        viewModelScope.launch {
            set.loggedSetId?.let { workoutRepository.deleteSet(it) }
            _blocks.update { blocks ->
                blocks?.toMutableList()?.also { list ->
                    val block = list.getOrNull(blockIndex) ?: return@also
                    list[blockIndex] = block.copy(
                        sets = block.sets.filterIndexed { i, _ -> i != setIndex },
                    )
                }
            }
        }
    }

    /** Logs the set to Room immediately and starts the rest timer. */
    fun completeSet(blockIndex: Int, setIndex: Int) {
        val blocks = _blocks.value ?: return
        val block = blocks.getOrNull(blockIndex) ?: return
        val set = block.sets.getOrNull(setIndex) ?: return
        if (set.completed) return
        val reps = set.reps.toIntOrNull() ?: return
        val weight = set.weight.toDoubleOrNull() ?: 0.0
        viewModelScope.launch {
            val id = workoutRepository.logSet(
                LoggedSetEntity(
                    sessionId = sessionId,
                    exerciseId = block.exerciseId,
                    exerciseOrder = blockIndex,
                    setNumber = setIndex + 1,
                    reps = reps,
                    weight = weight,
                    rpe = set.rpe.toIntOrNull()?.coerceIn(1, 10),
                    completedAt = System.currentTimeMillis(),
                )
            )
            mutateSet(blockIndex, setIndex) { it.copy(loggedSetId = id, completed = true) }
            startRestTimer(block.restSeconds)
        }
    }

    /** Un-checks a completed set and removes its database row. */
    fun uncompleteSet(blockIndex: Int, setIndex: Int) {
        val set = _blocks.value?.getOrNull(blockIndex)?.sets?.getOrNull(setIndex) ?: return
        val id = set.loggedSetId ?: return
        viewModelScope.launch {
            workoutRepository.deleteSet(id)
            mutateSet(blockIndex, setIndex) { it.copy(loggedSetId = null, completed = false) }
        }
    }

    fun addExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            val suggestion = workoutRepository.getLastSetsForExercise(exercise.id)
            val prefill = suggestion.lastOrNull()
            val newBlock = ExerciseBlockUi(
                exerciseId = exercise.id,
                name = exercise.name,
                bodyPart = exercise.bodyPart,
                mediaId = exercise.mediaId,
                sets = List(3) {
                    ActiveSetUi(
                        weight = prefill?.let { formatNumber(it.weight) } ?: "",
                        reps = prefill?.reps?.toString() ?: "10",
                    )
                },
                lastSessionHint = hintFrom(suggestion),
            )
            _blocks.update { (it ?: emptyList()) + newBlock }
        }
    }

    // --- Rest timer ---

    private fun startRestTimer(seconds: Int) {
        if (seconds <= 0) return
        restJob?.cancel()
        restJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                _restRemaining.value = remaining
                delay(1_000)
                remaining--
            }
            _restRemaining.value = null
        }
    }

    fun skipRest() {
        restJob?.cancel()
        _restRemaining.value = null
    }

    // --- Finish / discard ---

    val hasCompletedSets: Boolean
        get() = _blocks.value?.any { block -> block.sets.any { it.completed } } == true

    fun finishWorkout(onFinished: (Long) -> Unit) {
        viewModelScope.launch {
            skipRest()
            workoutRepository.completeSession(sessionId)
            onFinished(sessionId)
        }
    }

    fun discardWorkout(onDiscarded: () -> Unit) {
        viewModelScope.launch {
            skipRest()
            workoutRepository.discardSession(sessionId)
            onDiscarded()
        }
    }

    private fun mutateSet(blockIndex: Int, setIndex: Int, transform: (ActiveSetUi) -> ActiveSetUi) {
        _blocks.update { blocks ->
            blocks?.toMutableList()?.also { list ->
                val block = list.getOrNull(blockIndex) ?: return@also
                val sets = block.sets.toMutableList()
                val set = sets.getOrNull(setIndex) ?: return@also
                sets[setIndex] = transform(set)
                list[blockIndex] = block.copy(sets = sets)
            }
        }
    }
}
