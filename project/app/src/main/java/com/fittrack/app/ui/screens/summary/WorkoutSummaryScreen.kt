package com.fittrack.app.ui.screens.summary

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.ExerciseImage
import com.fittrack.app.ui.components.LoadingState
import com.fittrack.app.ui.components.formatDateTime
import com.fittrack.app.ui.components.formatDuration
import com.fittrack.app.ui.components.formatVolume
import com.fittrack.app.ui.components.formatWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryScreen(
    navController: NavController,
    viewModel: WorkoutSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Summary") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.deleteSession { navController.popBackStack() }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete workout")
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is SummaryUiState.Loading -> LoadingState(Modifier.padding(padding))
            is SummaryUiState.NotFound -> EmptyState(
                icon = Icons.Default.History,
                title = "Workout not found",
                modifier = Modifier.padding(padding),
            )
            is SummaryUiState.Content -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // "Training complete!" hero (per design mockups)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(48.dp),
                                )
                            }
                            Text(
                                text = "Training complete!",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(top = 16.dp),
                            )
                            Text(
                                text = "Great job! You crushed this session.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Workout Summary", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${state.session.name} · ${formatDateTime(state.session.startedAt)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                ) {
                                    StatBlock(
                                        label = "Duration",
                                        value = state.session.endedAt?.let {
                                            formatDuration(state.session.startedAt, it)
                                        } ?: "—",
                                        modifier = Modifier.weight(1f),
                                    )
                                    StatBlock(
                                        label = "Sets finished",
                                        value = state.exercises.sumOf { it.sets.size }.toString(),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                ) {
                                    StatBlock(
                                        label = "Volume",
                                        value = formatVolume(state.session.totalVolume),
                                        modifier = Modifier.weight(1f),
                                    )
                                    StatBlock(
                                        label = "Total reps",
                                        value = state.exercises
                                            .sumOf { ex -> ex.sets.sumOf { it.loggedSet.reps } }
                                            .toString(),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                ) {
                                    StatBlock(
                                        label = "Exercises",
                                        value = state.exercises.size.toString(),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                    items(state.exercises) { exercise ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ExerciseImage(
                                        exerciseId = exercise.exerciseId,
                                        bodyPart = exercise.bodyPart,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )
                                    Text(
                                        text = exercise.exerciseName,
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(start = 12.dp),
                                    )
                                }
                                exercise.sets.forEach { row ->
                                    val set = row.loggedSet
                                    Text(
                                        text = buildString {
                                            append("Set ${set.setNumber}: ${set.reps} reps @ ${formatWeight(set.weight)}")
                                            set.rpe?.let { append(" · RPE $it") }
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 6.dp),
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

@Composable
private fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}
