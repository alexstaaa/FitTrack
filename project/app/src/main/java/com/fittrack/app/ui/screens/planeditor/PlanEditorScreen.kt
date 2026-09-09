package com.fittrack.app.ui.screens.planeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fittrack.app.data.local.dao.PlanExerciseWithExercise
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.ExerciseImage
import com.fittrack.app.ui.components.ExercisePickerSheet
import com.fittrack.app.ui.components.LoadingState
import com.fittrack.app.ui.components.dragContainer
import com.fittrack.app.ui.components.rememberDragDropState
import com.fittrack.app.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    navController: NavController,
    viewModel: PlanEditorViewModel = hiltViewModel(),
) {
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    val planExercises by viewModel.planExercises.collectAsStateWithLifecycle()
    val pickerQuery by viewModel.pickerQuery.collectAsStateWithLifecycle()
    val pickerResults by viewModel.pickerResults.collectAsStateWithLifecycle()

    var showPicker by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<PlanExerciseWithExercise?>(null) }

    // Local name buffer, committed on change; seeded once the plan loads.
    var nameField by rememberSaveable { mutableStateOf("") }
    var nameSeeded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(plan) {
        val loaded = plan
        if (loaded != null && !nameSeeded) {
            nameField = loaded.name
            nameSeeded = true
        }
    }

    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(listState) { from, to ->
        viewModel.moveExercise(from, to)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Plan") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.startWorkout { sessionId ->
                                navController.navigate(Routes.activeWorkout(sessionId))
                            }
                        },
                        enabled = !planExercises.isNullOrEmpty(),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start workout")
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
    ) { padding ->
        val exercises = planExercises
        if (plan == null || exercises == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            OutlinedTextField(
                value = nameField,
                onValueChange = {
                    nameField = it
                    viewModel.renamePlan(it)
                },
                label = { Text("Plan name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (exercises.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.FitnessCenter,
                    title = "No exercises in this plan",
                    subtitle = "Add exercises, then long-press and drag to reorder.",
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .dragContainer(dragDropState),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(exercises, key = { _, item -> item.planExercise.id }) { index, item ->
                        PlanExerciseCard(
                            item = item,
                            index = index,
                            modifier = dragDropState.itemModifier(this, index),
                            onClick = { editingSlot = item },
                            onRemove = { viewModel.removeExercise(item.planExercise.id) },
                        )
                    }
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

    editingSlot?.let { slot ->
        EditTargetsDialog(
            item = slot,
            onDismiss = { editingSlot = null },
            onSave = { sets, reps, weight, rest ->
                viewModel.updateTargets(slot.planExercise, sets, reps, weight, rest)
                editingSlot = null
            },
        )
    }
}

@Composable
private fun PlanExerciseCard(
    item: PlanExerciseWithExercise,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "EXERCISE ${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = item.exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                val pe = item.planExercise
                Text(
                    text = buildString {
                        append("${pe.targetReps}×${pe.targetSets}")
                        pe.targetWeight?.let { append(" @ ${if (it % 1.0 == 0.0) it.toInt() else it} kg") }
                        append(" · rest ${pe.restSeconds}s")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            ExerciseImage(
                exerciseId = item.exercise.id,
                bodyPart = item.exercise.bodyPart,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove exercise")
            }
        }
    }
}

@Composable
private fun EditTargetsDialog(
    item: PlanExerciseWithExercise,
    onDismiss: () -> Unit,
    onSave: (sets: Int, reps: Int, weight: Double?, restSeconds: Int) -> Unit,
) {
    val pe = item.planExercise
    var sets by remember { mutableStateOf(pe.targetSets.toString()) }
    var reps by remember { mutableStateOf(pe.targetReps.toString()) }
    var weight by remember { mutableStateOf(pe.targetWeight?.toString() ?: "") }
    var rest by remember { mutableStateOf(pe.restSeconds.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.exercise.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sets,
                        onValueChange = { sets = it },
                        label = { Text("Sets") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = rest,
                        onValueChange = { rest = it },
                        label = { Text("Rest (s)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    sets.toIntOrNull() ?: pe.targetSets,
                    reps.toIntOrNull() ?: pe.targetReps,
                    weight.toDoubleOrNull(),
                    rest.toIntOrNull() ?: pe.restSeconds,
                )
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
