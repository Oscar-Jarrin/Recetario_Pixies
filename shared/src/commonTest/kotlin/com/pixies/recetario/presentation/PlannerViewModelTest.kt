package com.pixies.recetario.presentation

import com.pixies.recetario.TestDispatcherRule
import com.pixies.recetario.domain.exception.NetworkException
import com.pixies.recetario.domain.model.RecipeOverview
import com.pixies.recetario.domain.model.WeeklyPlan
import com.pixies.recetario.domain.usecase.DeleteWeeklyPlanUseCase
import com.pixies.recetario.domain.usecase.GetAllWeeklyPlansUseCase
import com.pixies.recetario.domain.usecase.SaveWeeklyPlanUseCase
import com.pixies.recetario.presentation.planner.PlannerState
import com.pixies.recetario.presentation.planner.PlannerViewModel
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModelTest {

    private val dispatcherRule = TestDispatcherRule()
    private val getAllWeeklyPlans: GetAllWeeklyPlansUseCase = mockk()
    private val saveWeeklyPlan: SaveWeeklyPlanUseCase = mockk()
    private val deleteWeeklyPlan: DeleteWeeklyPlanUseCase = mockk()

    @BeforeTest
    fun setUp() = dispatcherRule.before()

    @AfterTest
    fun tearDown() = dispatcherRule.after()

    private fun viewModel() = PlannerViewModel(
        getAllWeeklyPlans = getAllWeeklyPlans,
        saveWeeklyPlan = saveWeeklyPlan,
        deleteWeeklyPlan = deleteWeeklyPlan
    )

    @Test
    fun `initial state is Loading`() = runTest(dispatcherRule.dispatcher) {
        coEvery { getAllWeeklyPlans() } returns emptyList()

        val vm = viewModel()

        assertEquals(PlannerState.Loading, vm.plannerState.value)
    }

    @Test
    fun `loadAll transitions to Success with plans`() = runTest(dispatcherRule.dispatcher) {
        val plans = listOf(fakePlan(1L, "Plan A"), fakePlan(2L, "Plan B"))
        coEvery { getAllWeeklyPlans() } returns plans

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(PlannerState.Success(plans), vm.plannerState.value)
    }

    @Test
    fun `loadAll transitions to Error on exception`() = runTest(dispatcherRule.dispatcher) {
        val exception = NetworkException(RuntimeException("fail"))
        coEvery { getAllWeeklyPlans() } throws exception

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.plannerState.value
        assertTrue(state is PlannerState.Error)
        assertEquals(exception, state.exception)
    }

    @Test
    fun `createPlan calls savePlan with empty slots then refreshes list`() = runTest(dispatcherRule.dispatcher) {
        coEvery { getAllWeeklyPlans() } returns emptyList()
        coJustRun { saveWeeklyPlan(any()) }

        val vm = viewModel()
        advanceUntilIdle()

        vm.createPlan("Semana Saludable")
        advanceUntilIdle()

        coVerify { saveWeeklyPlan(match { it.planName == "Semana Saludable" && it.daySlots.isEmpty() }) }
        coVerify(atLeast = 2) { getAllWeeklyPlans() }
    }

    @Test
    fun `deletePlan calls deleteWeeklyPlan and refreshes list`() = runTest(dispatcherRule.dispatcher) {
        coEvery { getAllWeeklyPlans() } returns emptyList()
        coJustRun { deleteWeeklyPlan(5L) }

        val vm = viewModel()
        advanceUntilIdle()

        vm.deletePlan(5L)
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteWeeklyPlan(5L) }
        coVerify(atLeast = 2) { getAllWeeklyPlans() }
    }

    @Test
    fun `assignRecipe updates selected plan and calls savePlan`() = runTest(dispatcherRule.dispatcher) {
        val plan = fakePlan(id = 1L, name = "Plan A")
        val recipe = fakeRecipe()
        coEvery { getAllWeeklyPlans() } returns listOf(plan)
        coJustRun { saveWeeklyPlan(any()) }

        val vm = viewModel()
        advanceUntilIdle()

        vm.selectPlan(plan)
        vm.assignRecipe(dayIndex = 2, recipe = recipe)
        advanceUntilIdle()

        coVerify { saveWeeklyPlan(match { it.daySlots[2]?.contains(recipe) == true }) }
    }

    @Test
    fun `removeRecipe removes specific recipe from day and saves`() = runTest(dispatcherRule.dispatcher) {
        val recipe = fakeRecipe()
        val plan = fakePlan(id = 1L, name = "Plan A", daySlots = mapOf(3 to listOf(recipe)))
        coEvery { getAllWeeklyPlans() } returns listOf(plan)
        coJustRun { saveWeeklyPlan(any()) }

        val vm = viewModel()
        advanceUntilIdle()

        vm.selectPlan(plan)
        vm.removeRecipe(dayIndex = 3, recipeId = recipe.id)
        advanceUntilIdle()

        coVerify { saveWeeklyPlan(match { it.daySlots[3].isNullOrEmpty() }) }
    }

    @Test
    fun `removeRecipe with two recipes on same day keeps other recipe`() = runTest(dispatcherRule.dispatcher) {
        val r1 = fakeRecipe()
        val r2 = RecipeOverview(id = 2, title = "Other", imageUrl = "", readyInMinutes = 0, dishType = "")
        val plan = fakePlan(id = 1L, name = "Plan A", daySlots = mapOf(0 to listOf(r1, r2)))
        coEvery { getAllWeeklyPlans() } returns listOf(plan)
        coJustRun { saveWeeklyPlan(any()) }

        val vm = viewModel()
        advanceUntilIdle()

        vm.selectPlan(plan)
        vm.removeRecipe(dayIndex = 0, recipeId = r1.id)
        advanceUntilIdle()

        coVerify { saveWeeklyPlan(match { it.daySlots[0] == listOf(r2) }) }
    }

    @Test
    fun `assignRecipe twice on same day accumulates two recipes`() = runTest(dispatcherRule.dispatcher) {
        val r1 = fakeRecipe()
        val r2 = RecipeOverview(id = 2, title = "Other", imageUrl = "", readyInMinutes = 0, dishType = "")
        val plan = fakePlan(id = 1L, name = "Plan A")
        coEvery { getAllWeeklyPlans() } returns listOf(plan)
        coJustRun { saveWeeklyPlan(any()) }

        val vm = viewModel()
        advanceUntilIdle()

        vm.selectPlan(plan)
        vm.assignRecipe(dayIndex = 1, recipe = r1)
        advanceUntilIdle()
        vm.assignRecipe(dayIndex = 1, recipe = r2)
        advanceUntilIdle()

        coVerify { saveWeeklyPlan(match { it.daySlots[1]?.size == 2 }) }
    }
}

private fun fakePlan(id: Long, name: String, daySlots: Map<Int, List<RecipeOverview>> = emptyMap()) =
    WeeklyPlan(id = id, planName = name, daySlots = daySlots)

private fun fakeRecipe() = RecipeOverview(
    id = 1, title = "Test Recipe",
    imageUrl = "https://example.com/image.jpg",
    readyInMinutes = 30, dishType = "dinner"
)
