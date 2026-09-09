package com.fittrack.app.ui.screens.library

import app.cash.turbine.test
import com.fittrack.app.data.repository.ExerciseRepository
import com.fittrack.app.fakes.FakeExerciseDao
import com.fittrack.app.fakes.exercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseLibraryViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var dao: FakeExerciseDao
    private lateinit var viewModel: ExerciseLibraryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = FakeExerciseDao()
        dao.exercises.value = listOf(
            exercise("1", "barbell bench press", bodyPart = "chest", equipment = "barbell"),
            exercise("2", "push-up", bodyPart = "chest", equipment = "body weight"),
            exercise("3", "barbell full squat", bodyPart = "upper legs", equipment = "barbell"),
        )
        viewModel = ExerciseLibraryViewModel(ExerciseRepository(dao))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits all exercises sorted by name`() = runTest {
        viewModel.uiState.test {
            val state = awaitContent()
            assertEquals(
                listOf("barbell bench press", "barbell full squat", "push-up"),
                state.exercises.map { it.name },
            )
        }
    }

    @Test
    fun `search query filters by name`() = runTest {
        viewModel.uiState.test {
            awaitContent()
            viewModel.onQueryChange("squat")
            val state = awaitContent()
            assertEquals(listOf("barbell full squat"), state.exercises.map { it.name })
        }
    }

    @Test
    fun `body part and equipment filters combine`() = runTest {
        viewModel.uiState.test {
            awaitContent()
            viewModel.onBodyPartSelected("chest")
            val chestOnly = awaitContent()
            assertEquals(2, chestOnly.exercises.size)

            viewModel.onEquipmentSelected("body weight")
            val chestBodyweight = awaitContent()
            assertEquals(listOf("push-up"), chestBodyweight.exercises.map { it.name })
        }
    }

    @Test
    fun `favorites only shows favorited exercises`() = runTest {
        viewModel.uiState.test {
            val initial = awaitContent()
            viewModel.toggleFavorite(initial.exercises.first { it.id == "2" })
            awaitContent() // list re-emits with the favorite flag set

            viewModel.onFavoritesOnlyToggled()
            val favorites = awaitContent()
            assertEquals(listOf("push-up"), favorites.exercises.map { it.name })
            assertTrue(favorites.exercises.all { it.isFavorite })
        }
    }

    @Test
    fun `clear filters restores full list`() = runTest {
        viewModel.uiState.test {
            awaitContent()
            viewModel.onBodyPartSelected("upper legs")
            assertEquals(1, awaitContent().exercises.size)
            viewModel.onClearFilters()
            assertEquals(3, awaitContent().exercises.size)
        }
    }

    /** Skips Loading emissions and returns the next Content state. */
    private suspend fun app.cash.turbine.TurbineTestContext<LibraryUiState>.awaitContent(): LibraryUiState.Content {
        while (true) {
            when (val item = awaitItem()) {
                is LibraryUiState.Content -> return item
                is LibraryUiState.Loading -> continue
            }
        }
    }
}
