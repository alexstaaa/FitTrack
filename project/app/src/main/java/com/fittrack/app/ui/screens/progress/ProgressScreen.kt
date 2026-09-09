package com.fittrack.app.ui.screens.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fittrack.app.data.local.entity.ExerciseEntity
import com.fittrack.app.ui.components.SimpleColumnChart
import com.fittrack.app.ui.components.SimpleLineChart
import com.fittrack.app.ui.components.formatDate
import com.fittrack.app.ui.components.formatWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    navController: NavController,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val loggedExercises by viewModel.loggedExercises.collectAsStateWithLifecycle()
    val selectedExerciseId by viewModel.selectedExerciseId.collectAsStateWithLifecycle()
    val metric by viewModel.metric.collectAsStateWithLifecycle()
    val progressPoints by viewModel.progressPoints.collectAsStateWithLifecycle()
    val bodyStats by viewModel.bodyStats.collectAsStateWithLifecycle()
    val consistency by viewModel.consistency.collectAsStateWithLifecycle()

    var showAddStat by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Progress") }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // --- Consistency ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Consistency", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            Stat("${consistency.workoutsThisWeek}", "this week")
                            Stat("${consistency.weeklyStreak}", "week streak")
                        }
                        Text(
                            text = "Workouts per week (last 8 weeks)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SimpleColumnChart(
                            values = consistency.workoutsPerWeek.map { it.toDouble() },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            // --- Per-exercise progress ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Exercise progress", style = MaterialTheme.typography.titleMedium)
                        if (loggedExercises.isEmpty()) {
                            Text(
                                text = "Log a few workouts and your lifts will chart here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        } else {
                            ExerciseSelector(
                                exercises = loggedExercises,
                                selectedId = selectedExerciseId,
                                onSelect = viewModel::selectExercise,
                            )
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilterChip(
                                    selected = metric == ProgressMetric.MaxWeight,
                                    onClick = { viewModel.setMetric(ProgressMetric.MaxWeight) },
                                    label = { Text("Max weight") },
                                )
                                FilterChip(
                                    selected = metric == ProgressMetric.Volume,
                                    onClick = { viewModel.setMetric(ProgressMetric.Volume) },
                                    label = { Text("Volume") },
                                )
                            }
                            if (selectedExerciseId == null) {
                                Text(
                                    text = "Pick an exercise to see its trend.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            } else {
                                SimpleLineChart(
                                    values = progressPoints.map {
                                        when (metric) {
                                            ProgressMetric.MaxWeight -> it.maxWeight
                                            ProgressMetric.Volume -> it.totalVolume
                                        }
                                    },
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                Text(
                                    text = "One point per completed session, oldest to newest.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // --- Body stats ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Body weight",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(onClick = { showAddStat = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("Log", modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                        if (bodyStats.isEmpty()) {
                            Text(
                                text = "No entries yet — log your weight to start the trend.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        } else {
                            SimpleLineChart(
                                values = bodyStats.map { it.weightKg },
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            bodyStats.takeLast(5).reversed().forEach { stat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${formatDate(stat.recordedAt)} — ${formatWeight(stat.weightKg)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(onClick = { viewModel.deleteBodyStat(stat.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete entry",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddStat) {
        AddBodyStatDialog(
            onDismiss = { showAddStat = false },
            onSave = { weight, chest, waist, hips ->
                viewModel.addBodyStat(weight, chest, waist, hips)
                showAddStat = false
            },
        )
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExerciseSelector(
    exercises: List<ExerciseEntity>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = exercises.firstOrNull { it.id == selectedId }?.name ?: "Choose exercise"
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.padding(top = 8.dp)) {
            Text(selectedName)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            exercises.forEach { exercise ->
                DropdownMenuItem(
                    text = { Text(exercise.name) },
                    onClick = {
                        onSelect(exercise.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AddBodyStatDialog(
    onDismiss: () -> Unit,
    onSave: (weight: Double, chest: Double?, waist: Double?, hips: Double?) -> Unit,
) {
    var weight by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var hips by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log body stats") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = chest,
                        onValueChange = { chest = it },
                        label = { Text("Chest (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = waist,
                        onValueChange = { waist = it },
                        label = { Text("Waist (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = hips,
                        onValueChange = { hips = it },
                        label = { Text("Hips (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    weight.toDoubleOrNull()?.let { w ->
                        onSave(w, chest.toDoubleOrNull(), waist.toDoubleOrNull(), hips.toDoubleOrNull())
                    }
                },
                enabled = weight.toDoubleOrNull() != null,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
