package com.fittrack.app.ui.screens.exercisedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.ExerciseImage
import com.fittrack.app.ui.components.LoadingState
import androidx.compose.material.icons.filled.FitnessCenter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    navController: NavController,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val exercise by viewModel.exercise.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "Exercise") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    exercise?.let { ex ->
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                imageVector = if (ex.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (ex.isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (ex.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is ExerciseDetailUiState.Loading -> LoadingState(Modifier.padding(padding))
            is ExerciseDetailUiState.NotFound -> EmptyState(
                icon = Icons.Default.FitnessCenter,
                title = "Exercise not found",
                modifier = Modifier.padding(padding),
            )
            is ExerciseDetailUiState.Content -> {
                val ex = exercise ?: return@Scaffold
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Animated GIF demo; falls back to a body-part icon offline.
                    ExerciseImage(
                        exerciseId = ex.id,
                        bodyPart = ex.bodyPart,
                        contentDescription = ex.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp)),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text(ex.bodyPart) })
                        AssistChip(onClick = {}, label = { Text(ex.equipment) })
                        AssistChip(onClick = {}, label = { Text(ex.target) })
                    }

                    Column {
                        Text("Muscles worked", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = buildString {
                                append("Primary: ${ex.target}")
                                if (ex.muscleGroup.isNotBlank()) append(" · ${ex.muscleGroup}")
                                if (ex.secondaryMuscles.isNotEmpty()) {
                                    append("\nSecondary: ${ex.secondaryMuscles.joinToString(", ")}")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    Column {
                        Text("Instructions", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = state.instructions
                                ?: "No instructions available for this exercise.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
