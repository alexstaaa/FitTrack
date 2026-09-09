package com.fittrack.app.ui.screens.plans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fittrack.app.data.local.dao.PlanSummary
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.ExerciseImage
import com.fittrack.app.ui.components.LoadingState
import com.fittrack.app.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    navController: NavController,
    viewModel: PlansViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Workout Plans") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.createPlan { planId ->
                    navController.navigate(Routes.planEditor(planId))
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "New plan")
            }
        },
    ) { padding ->
        when (val state = uiState) {
            is PlansUiState.Loading -> LoadingState(Modifier.padding(padding))
            is PlansUiState.Content -> {
                if (state.plans.isEmpty()) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.ListAlt,
                        title = "No plans yet",
                        subtitle = "Tap + to build your first workout plan.",
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.plans, key = { it.plan.id }) { summary ->
                            PlanCard(
                                summary = summary,
                                onOpen = { navController.navigate(Routes.planEditor(summary.plan.id)) },
                                onStart = {
                                    viewModel.startWorkout(summary.plan.id) { sessionId ->
                                        navController.navigate(Routes.activeWorkout(sessionId))
                                    }
                                },
                                onDelete = { viewModel.deletePlan(summary.plan.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    summary: PlanSummary,
    onOpen: () -> Unit,
    onStart: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = summary.plan.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Plan options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    menuExpanded = false
                                    onOpen()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                },
                            )
                        }
                    }
                }
                if (summary.plan.description.isNotBlank()) {
                    Text(
                        text = summary.plan.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${summary.exerciseCount} exercises · ~${summary.estimatedMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                FilledTonalButton(
                    onClick = onStart,
                    modifier = Modifier.padding(top = 8.dp),
                    enabled = summary.exerciseCount > 0,
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text("Start workout", modifier = Modifier.padding(start = 4.dp))
                }
            }
            ExerciseImage(
                exerciseId = summary.firstExerciseId,
                bodyPart = "",
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(88.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
        }
    }
}
