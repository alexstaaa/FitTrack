package com.fittrack.app.ui.screens.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.ExerciseImage
import com.fittrack.app.ui.components.ExercisePickerSheet
import com.fittrack.app.ui.components.LoadingState
import com.fittrack.app.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    navController: NavController,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val blocks by viewModel.blocks.collectAsStateWithLifecycle()
    val restRemaining by viewModel.restRemaining.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()

    var showPicker by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    val pickerQuery by viewModel.pickerQuery.collectAsStateWithLifecycle()
    val pickerResults by viewModel.pickerResults.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(session?.name ?: "Workout")
                        Text(
                            text = formatElapsed(elapsedSeconds),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { showFinishDialog = true }) {
                        Text("Finish")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPicker = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add exercise") },
            )
        },
        bottomBar = {
            // Rest timer bar slides in after each completed set.
            AnimatedVisibility(
                visible = restRemaining != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                Snackbar(
                    modifier = Modifier.padding(12.dp),
                    action = {
                        TextButton(onClick = viewModel::skipRest) { Text("Skip") }
                    },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null)
                        Text(
                            text = "Rest: ${restRemaining ?: 0}s",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        val blockList = blocks
        if (blockList == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        if (blockList.isEmpty()) {
            EmptyState(
                icon = Icons.Default.FitnessCenter,
                title = "Empty workout",
                subtitle = "Add an exercise to start logging sets.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(blockList, key = { index, block -> "$index-${block.exerciseId}" }) { blockIndex, block ->
                    ExerciseBlockCard(
                        block = block,
                        onUpdateSet = { setIndex, weight, reps, rpe ->
                            viewModel.updateSet(blockIndex, setIndex, weight, reps, rpe)
                        },
                        onCompleteSet = { setIndex -> viewModel.completeSet(blockIndex, setIndex) },
                        onUncompleteSet = { setIndex -> viewModel.uncompleteSet(blockIndex, setIndex) },
                        onAddSet = { viewModel.addSet(blockIndex) },
                        onRemoveSet = { setIndex -> viewModel.removeSet(blockIndex, setIndex) },
                    )
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerSheet(
            query = pickerQuery,
            results = pickerResults,
            onQueryChange = viewModel::onPickerQueryChange,
            onPick = { exercise ->
                viewModel.addExercise(exercise)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }

    if (showFinishDialog) {
        val anyCompleted = viewModel.hasCompletedSets
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text(if (anyCompleted) "Finish workout?" else "Discard workout?") },
            text = {
                Text(
                    if (anyCompleted) {
                        "Duration and total volume will be saved to your history."
                    } else {
                        "No sets were logged — this workout will be discarded."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    if (anyCompleted) {
                        viewModel.finishWorkout { sessionId ->
                            navController.navigate(Routes.workoutSummary(sessionId)) {
                                popUpTo(Routes.HOME)
                            }
                        }
                    } else {
                        viewModel.discardWorkout { navController.popBackStack() }
                    }
                }) { Text(if (anyCompleted) "Finish" else "Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("Keep going") }
            },
        )
    }
}

private fun formatElapsed(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun ExerciseBlockCard(
    block: ExerciseBlockUi,
    onUpdateSet: (setIndex: Int, weight: String?, reps: String?, rpe: String?) -> Unit,
    onCompleteSet: (setIndex: Int) -> Unit,
    onUncompleteSet: (setIndex: Int) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (setIndex: Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseImage(
                    exerciseId = block.exerciseId,
                    bodyPart = block.bodyPart,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(block.name, style = MaterialTheme.typography.titleSmall)
                    block.lastSessionHint?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            block.sets.forEachIndexed { setIndex, set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${setIndex + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    OutlinedTextField(
                        value = set.weight,
                        onValueChange = { onUpdateSet(setIndex, it, null, null) },
                        label = { Text("kg") },
                        enabled = !set.completed,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = set.reps,
                        onValueChange = { onUpdateSet(setIndex, null, it, null) },
                        label = { Text("reps") },
                        enabled = !set.completed,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = set.rpe,
                        onValueChange = { onUpdateSet(setIndex, null, null, it) },
                        label = { Text("RPE") },
                        enabled = !set.completed,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f),
                    )
                    if (set.completed) {
                        FilledIconButton(onClick = { onUncompleteSet(setIndex) }) {
                            Icon(Icons.Default.Close, contentDescription = "Undo set")
                        }
                    } else {
                        FilledIconButton(onClick = { onCompleteSet(setIndex) }) {
                            Icon(Icons.Default.Check, contentDescription = "Complete set")
                        }
                    }
                }
            }

            Row(modifier = Modifier.padding(top = 4.dp)) {
                TextButton(onClick = onAddSet) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Add set", modifier = Modifier.padding(start = 4.dp))
                }
                if (block.sets.isNotEmpty()) {
                    TextButton(onClick = { onRemoveSet(block.sets.lastIndex) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Remove set", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }
}
