package com.fittrack.app.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fittrack.app.ui.screens.exercisedetail.ExerciseDetailScreen
import com.fittrack.app.ui.screens.history.HistoryScreen
import com.fittrack.app.ui.screens.home.HomeScreen
import com.fittrack.app.ui.screens.library.ExerciseLibraryScreen
import com.fittrack.app.ui.screens.planeditor.PlanEditorScreen
import com.fittrack.app.ui.screens.plans.PlansScreen
import com.fittrack.app.ui.screens.progress.ProgressScreen
import com.fittrack.app.ui.screens.settings.SettingsScreen
import com.fittrack.app.ui.screens.summary.WorkoutSummaryScreen
import com.fittrack.app.ui.screens.workout.ActiveWorkoutScreen

/** Route constants for every screen in the app. */
object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val EXERCISE_DETAIL = "exercise/{exerciseId}"
    const val PLANS = "plans"
    const val PLAN_EDITOR = "plan_editor/{planId}"
    const val ACTIVE_WORKOUT = "active_workout/{sessionId}?copyFrom={copyFrom}"
    const val HISTORY = "history"
    const val WORKOUT_SUMMARY = "workout_summary/{sessionId}"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"

    fun exerciseDetail(exerciseId: String) = "exercise/$exerciseId"
    fun planEditor(planId: Long) = "plan_editor/$planId"
    fun activeWorkout(sessionId: Long, copyFromSessionId: Long? = null) =
        "active_workout/$sessionId?copyFrom=${copyFromSessionId ?: -1L}"
    fun workoutSummary(sessionId: Long) = "workout_summary/$sessionId"
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Mockup-style bar: four icon destinations around a white center "+" button.
 * Plans is reached from Home ("Your plans" → VIEW ALL), not from the bar.
 */
private val leftNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home),
    BottomNavItem(Routes.LIBRARY, "Library", Icons.Filled.FitnessCenter),
)
private val rightNavItems = listOf(
    BottomNavItem(Routes.HISTORY, "History", Icons.Filled.History),
    BottomNavItem(Routes.PROGRESS, "Progress", Icons.AutoMirrored.Filled.ShowChart),
)

// Screens that keep the bottom bar visible.
private val barRoutes =
    (leftNavItems + rightNavItems).map { it.route }.toSet() + Routes.PLANS

@Composable
fun FitTrackNavGraph(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val quickStartViewModel: QuickStartViewModel = hiltViewModel()

    Scaffold(
        bottomBar = {
            // Hidden during a workout so a stray tap can't leave mid-set.
            if (currentRoute in barRoutes) {
                FitTrackBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onQuickStart = {
                        quickStartViewModel.startEmptyWorkout { sessionId ->
                            navController.navigate(Routes.activeWorkout(sessionId))
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) { HomeScreen(navController) }
            composable(Routes.LIBRARY) { ExerciseLibraryScreen(navController) }
            composable(
                route = Routes.EXERCISE_DETAIL,
                arguments = listOf(navArgument("exerciseId") { type = NavType.StringType }),
            ) { ExerciseDetailScreen(navController) }
            composable(Routes.PLANS) { PlansScreen(navController) }
            composable(
                route = Routes.PLAN_EDITOR,
                arguments = listOf(navArgument("planId") { type = NavType.LongType }),
            ) { PlanEditorScreen(navController) }
            composable(
                route = Routes.ACTIVE_WORKOUT,
                arguments = listOf(
                    navArgument("sessionId") { type = NavType.LongType },
                    navArgument("copyFrom") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
                // Slide the workout screen in/out to mark start/finish.
                enterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
                exitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() },
            ) { ActiveWorkoutScreen(navController) }
            composable(Routes.HISTORY) { HistoryScreen(navController) }
            composable(
                route = Routes.WORKOUT_SUMMARY,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
                enterTransition = { fadeIn() },
            ) { WorkoutSummaryScreen(navController) }
            composable(Routes.PROGRESS) { ProgressScreen(navController) }
            composable(Routes.SETTINGS) { SettingsScreen(navController) }
        }
    }
}

@Composable
private fun FitTrackBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onQuickStart: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .navigationBarsPadding()
            .height(76.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leftNavItems.forEach { item ->
            BarIcon(item, selected = currentRoute == item.route) { onNavigate(item.route) }
        }
        // Center "+": white rounded square, starts an empty workout.
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.onSurface)
                .clickable(onClick = onQuickStart),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Start empty workout",
                tint = MaterialTheme.colorScheme.surface,
            )
        }
        rightNavItems.forEach { item ->
            BarIcon(item, selected = currentRoute == item.route) { onNavigate(item.route) }
        }
    }
}

/** Icon-only item; the selected one gains a label and an orange underline. */
@Composable
private fun BarIcon(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        if (selected) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .width(20.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
