package com.fittrack.app.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fittrack.app.data.local.dao.SessionSummary
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.LoadingState
import com.fittrack.app.ui.components.formatDateTime
import com.fittrack.app.ui.components.formatDuration
import com.fittrack.app.ui.components.formatVolume
import com.fittrack.app.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("History") }) }) { padding ->
        when (val state = uiState) {
            is HistoryUiState.Loading -> LoadingState(Modifier.padding(padding))
            is HistoryUiState.Content -> {
                if (state.months.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.History,
                        title = "No workouts yet",
                        subtitle = "Completed workouts will show up here.",
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.months.forEach { group ->
                            item(key = "header-${group.label}") {
                                Text(
                                    text = group.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                )
                            }
                            items(group.sessions, key = { it.session.id }) { summary ->
                                SessionCard(summary = summary) {
                                    navController.navigate(Routes.workoutSummary(summary.session.id))
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
private fun SessionCard(summary: SessionSummary, onClick: () -> Unit) {
    val session = summary.session
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(session.name, style = MaterialTheme.typography.titleSmall)
            Text(
                text = formatDateTime(session.startedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = buildString {
                        session.endedAt?.let { append(formatDuration(session.startedAt, it)) }
                        append(" · ${summary.exerciseCount} exercises · ${summary.setCount} sets")
                        append(" · ${formatVolume(session.totalVolume)}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
