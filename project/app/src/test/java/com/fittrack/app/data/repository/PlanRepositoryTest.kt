package com.fittrack.app.data.repository

import com.fittrack.app.data.local.dao.PlanDao
import com.fittrack.app.data.local.dao.PlanExerciseWithExercise
import com.fittrack.app.data.local.dao.PlanSummary
import com.fittrack.app.data.local.entity.PlanExerciseEntity
import com.fittrack.app.data.local.entity.WorkoutPlanEntity
import com.fittrack.app.fakes.exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** In-memory PlanDao capturing writes, for reorder/remove logic tests. */
private class FakePlanDao : PlanDao {
    val plans = MutableStateFlow<List<WorkoutPlanEntity>>(emptyList())
    val slots = MutableStateFlow<List<PlanExerciseEntity>>(emptyList())
    private var nextPlanId = 1L
    private var nextSlotId = 1L

    override fun observePlanSummaries(): Flow<List<PlanSummary>> = plans.map { list ->
        list.map { plan ->
            val planSlots = slots.value.filter { it.planId == plan.id }.sortedBy { it.position }
            PlanSummary(
                plan = plan,
                exerciseCount = planSlots.size,
                firstExerciseId = planSlots.firstOrNull()?.exerciseId,
                estimatedMinutes = planSlots.sumOf { it.targetSets * (it.restSeconds + it.targetReps * 4) } / 60,
            )
        }
    }

    override suspend fun getPlan(id: Long): WorkoutPlanEntity? =
        plans.value.firstOrNull { it.id == id }

    override fun observePlan(id: Long): Flow<WorkoutPlanEntity?> =
        plans.map { list -> list.firstOrNull { it.id == id } }

    override fun observePlanExercises(planId: Long): Flow<List<PlanExerciseWithExercise>> =
        slots.map { list -> withExercises(list.filter { it.planId == planId }) }

    override suspend fun getPlanExercises(planId: Long): List<PlanExerciseWithExercise> =
        withExercises(slots.value.filter { it.planId == planId }.sortedBy { it.position })

    private fun withExercises(list: List<PlanExerciseEntity>): List<PlanExerciseWithExercise> =
        list.sortedBy { it.position }.map { slot ->
            PlanExerciseWithExercise(slot, exercise(slot.exerciseId, "exercise-${slot.exerciseId}"))
        }

    override suspend fun count(): Int = plans.value.size

    override suspend fun insertPlan(plan: WorkoutPlanEntity): Long {
        val id = nextPlanId++
        plans.value = plans.value + plan.copy(id = id)
        return id
    }

    override suspend fun updatePlan(plan: WorkoutPlanEntity) {
        plans.value = plans.value.map { if (it.id == plan.id) plan else it }
    }

    override suspend fun deletePlan(plan: WorkoutPlanEntity) = deletePlanById(plan.id)

    override suspend fun deletePlanById(id: Long) {
        plans.value = plans.value.filter { it.id != id }
        slots.value = slots.value.filter { it.planId != id }
    }

    override suspend fun insertPlanExercise(planExercise: PlanExerciseEntity): Long {
        val id = nextSlotId++
        slots.value = slots.value + planExercise.copy(id = id)
        return id
    }

    override suspend fun insertPlanExercises(planExercises: List<PlanExerciseEntity>) {
        planExercises.forEach { insertPlanExercise(it) }
    }

    override suspend fun updatePlanExercise(planExercise: PlanExerciseEntity) {
        slots.value = slots.value.map { if (it.id == planExercise.id) planExercise else it }
    }

    override suspend fun updatePlanExercises(planExercises: List<PlanExerciseEntity>) {
        planExercises.forEach { updatePlanExercise(it) }
    }

    override suspend fun deletePlanExerciseById(id: Long) {
        slots.value = slots.value.filter { it.id != id }
    }
}

class PlanRepositoryTest {

    private lateinit var dao: FakePlanDao
    private lateinit var repository: PlanRepository
    private var planId: Long = 0

    @Before
    fun setUp() = runTest {
        dao = FakePlanDao()
        repository = PlanRepository(dao)
        planId = repository.createPlan("Test Plan")
        repository.addExercise(planId, "a")
        repository.addExercise(planId, "b")
        repository.addExercise(planId, "c")
    }

    private suspend fun order(): List<String> =
        repository.getPlanExercises(planId).map { it.planExercise.exerciseId }

    @Test
    fun `addExercise appends with sequential positions`() = runTest {
        val slots = repository.getPlanExercises(planId).map { it.planExercise }
        assertEquals(listOf(0, 1, 2), slots.map { it.position })
        assertEquals(listOf("a", "b", "c"), order())
    }

    @Test
    fun `moveExercise reorders and reindexes`() = runTest {
        repository.moveExercise(planId, from = 0, to = 2)
        assertEquals(listOf("b", "c", "a"), order())
        assertEquals(
            listOf(0, 1, 2),
            repository.getPlanExercises(planId).map { it.planExercise.position },
        )
    }

    @Test
    fun `moveExercise ignores out of range indices`() = runTest {
        repository.moveExercise(planId, from = 0, to = 99)
        assertEquals(listOf("a", "b", "c"), order())
    }

    @Test
    fun `removeExercise compacts positions`() = runTest {
        val middle = repository.getPlanExercises(planId)[1].planExercise.id
        repository.removeExercise(planId, middle)
        assertEquals(listOf("a", "c"), order())
        assertEquals(
            listOf(0, 1),
            repository.getPlanExercises(planId).map { it.planExercise.position },
        )
    }
}
