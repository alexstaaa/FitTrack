package com.fittrack.app.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fittrack.app.data.local.entity.ExerciseEntity
import com.fittrack.app.ui.components.EmptyState
import com.fittrack.app.ui.components.ExerciseImage
import com.fittrack.app.ui.components.LoadingState
import com.fittrack.app.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    navController: NavController,
    viewModel: ExerciseLibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val bodyParts by viewModel.bodyParts.collectAsStateWithLifecycle()
    val equipment by viewModel.equipment.collectAsStateWithLifecycle()
    val targets by viewModel.targets.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Exercise Library") }) }) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search 1,300+ exercises…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filters.favoritesOnly,
                    onClick = viewModel::onFavoritesOnlyToggled,
                    label = { Text("Favorites") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (filters.favoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
                DropdownFilterChip(
                    label = "Body part",
                    selected = filters.bodyPart,
                    options = bodyParts,
                    onSelected = viewModel::onBodyPartSelected,
                )
                DropdownFilterChip(
                    label = "Equipment",
                    selected = filters.equipment,
                    options = equipment,
                    onSelected = viewModel::onEquipmentSelected,
                )
                DropdownFilterChip(
                    label = "Muscle",
                    selected = filters.target,
                    options = targets,
                    onSelected = viewModel::onTargetSelected,
                )
            }

            when (val state = uiState) {
                is LibraryUiState.Loading -> LoadingState()
                is LibraryUiState.Content -> {
                    if (state.exercises.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.FitnessCenter,
                            title = "No exercises found",
                            subtitle = "Try a different search or clear the filters.",
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.exercises, key = { it.id }) { exercise ->
                                ExerciseRow(
                                    exercise = exercise,
                                    onClick = {
                                        navController.navigate(Routes.exerciseDetail(exercise.id))
                                    },
                                    onToggleFavorite = { viewModel.toggleFavorite(exercise) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownFilterChip(
    label: String,
    selected: String?,
    options: List<String>,
    onSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(selected ?: label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Any $label") },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: ExerciseEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(exercise.name) },
        supportingContent = {
            Text(
                text = "${exercise.bodyPart} · ${exercise.target} · ${exercise.equipment}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            ExerciseImage(
                exerciseId = exercise.id,
                bodyPart = exercise.bodyPart,
                contentDescription = exercise.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        },
        trailingContent = {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (exercise.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (exercise.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (exercise.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
