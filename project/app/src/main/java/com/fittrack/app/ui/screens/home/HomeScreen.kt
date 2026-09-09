package com.fittrack.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fittrack.app.data.local.dao.PlanSummary
import com.fittrack.app.ui.components.ExerciseImage
import com.fittrack.app.ui.components.LoadingState
import com.fittrack.app.ui.components.SimpleColumnChart
import com.fittrack.app.ui.components.formatDateTime
import com.fittrack.app.ui.components.formatDuration
import com.fittrack.app.ui.components.formatVolume
import com.fittrack.app.ui.navigation.Routes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold { padding ->
        if (uiState.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Greeting header: avatar · date overline + greeting · settings
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    ) {
                        Text(
                            text = LocalDate.now()
                                .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
                                .uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Hi there 👋",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { navController.navigate(Routes.SETTINGS) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            // Segmented "Today's Plan | Weekly Stats" toggle (white pill = selected)
            item {
                SegmentedToggle(
                    options = listOf("Today's Plan", "Weekly Stats"),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                )
            }

            // Week strip shows on both tabs, like the mockup header area.
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    uiState.weekDays.forEach { day -> WeekDayCell(day) }
                }
            }

            if (selectedTab == 0) {
                todaysPlanContent(uiState, viewModel, navController)
            } else {
                weeklyStatsContent(uiState)
            }
        }
    }
}

// --- Today's Plan tab ---

private fun androidx.compose.foundation.lazy.LazyListScope.todaysPlanContent(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    navController: NavController,
) {
    // Quick actions
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    viewModel.startEmptyWorkout { sessionId ->
                        navController.navigate(Routes.activeWorkout(sessionId))
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Empty workout", modifier = Modifier.padding(start = 4.dp))
            }
            if (uiState.lastWorkout != null) {
                OutlinedButton(
                    onClick = {
                        viewModel.repeatLastWorkout { sessionId, copyFrom ->
                            navController.navigate(Routes.activeWorkout(sessionId, copyFrom))
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text("Repeat last", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }

    if (uiState.plans.isNotEmpty()) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Your plans",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "VIEW ALL",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { navController.navigate(Routes.PLANS) }
                        .padding(4.dp),
                )
            }
        }
        items(uiState.plans.size, key = { uiState.plans[it].plan.id }) { index ->
            val summary = uiState.plans[index]
            PlanCard(
                summary = summary,
                onStart = {
                    viewModel.startPlanWorkout(summary.plan.id) { sessionId ->
                        navController.navigate(Routes.activeWorkout(sessionId))
                    }
                },
                onOpen = { navController.navigate(Routes.planEditor(summary.plan.id)) },
            )
        }
    }
}

// --- Weekly Stats tab ---

private fun androidx.compose.foundation.lazy.LazyListScope.weeklyStatsContent(uiState: HomeUiState) {
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressStatTile(
                value = uiState.workoutsThisWeek,
                target = 3,
                label = "workouts this week",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Default.LocalFireDepartment,
                value = "${uiState.weeklyStreak}",
                label = "week streak",
                modifier = Modifier.weight(1f),
            )
        }
    }
    uiState.lastWorkout?.let { last ->
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LAST WORKOUT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(last.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = buildString {
                            append(formatDateTime(last.startedAt))
                            last.endedAt?.let { append(" · ${formatDuration(last.startedAt, it)}") }
                            append(" · ${formatVolume(last.totalVolume)}")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Weekly Activity",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Text(
                        text = "  Workouts",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Last 8 weeks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SimpleColumnChart(
                    values = uiState.workoutsPerWeek.map { it.toDouble() },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

// --- Components ---

@Composable
private fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.onSurface
                        else androidx.compose.ui.graphics.Color.Transparent
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun WeekDayCell(day: WeekDay) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${day.dayOfMonth}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val isPastMissed = !day.done && day.isPast
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (day.done) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .then(
                    if (day.isToday && !day.done) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                day.done -> Icon(
                    Icons.Default.Check,
                    contentDescription = "Workout done",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
                isPastMissed -> Icon(
                    Icons.Default.Close,
                    contentDescription = "No workout",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                else -> Text(
                    text = day.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (day.isToday) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

/** Mockup-style activity tile with a circular progress ring (e.g. steps 81%). */
@Composable
private fun ProgressStatTile(
    value: Int,
    target: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (value.toFloat() / target).coerceIn(0f, 1f) },
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 6.dp,
                )
                Text(
                    text = "$value/$target",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Mockup-style plan card: chip + name + meta row, exercise thumbnail right. */
@Composable
fun PlanCard(
    summary: PlanSummary,
    onStart: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "Weightlift",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = summary.plan.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = " ${summary.exerciseCount} exercises   ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = " ~${summary.estimatedMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = onStart,
                    enabled = summary.exerciseCount > 0,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .height(38.dp),
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text("Start", modifier = Modifier.padding(start = 4.dp))
                }
            }
            ExerciseImage(
                exerciseId = summary.firstExerciseId,
                bodyPart = "",
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(96.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
        }
    }
}
