package com.pixies.recetario.data

import com.pixies.recetario.data.local.dao.RecipeOverviewDao
import com.pixies.recetario.data.local.dao.WeeklyPlanDao
import com.pixies.recetario.data.local.dao.WeeklyPlanSlotDao
import com.pixies.recetario.data.local.entity.RecipeOverviewEntity
import com.pixies.recetario.data.local.entity.WeeklyPlanEntity
import com.pixies.recetario.data.local.entity.WeeklyPlanSlotEntity
import com.pixies.recetario.domain.exception.PlanNotFoundException
import com.pixies.recetario.domain.model.RecipeOverview
import com.pixies.recetario.domain.model.WeeklyPlan
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WeeklyPlanRepositoryImplTest {

    private val planDao: WeeklyPlanDao = mockk()
    private val slotDao: WeeklyPlanSlotDao = mockk()
    private val overviewDao: RecipeOverviewDao = mockk()
    private val repository = WeeklyPlanRepositoryImpl(planDao, slotDao, overviewDao)

    @Test
    fun `savePlan inserts entity and slot rows, deletes old slots first`() = runTest {
        val recipe = fakeOverviewDomain()
        val plan = WeeklyPlan(id = 1L, planName = "Week A", daySlots = mapOf(0 to listOf(recipe)))
        coEvery { planDao.insert(any()) } returns 1L
        coEvery { slotDao.deleteSlotsForPlan(1L) } just Runs
        coEvery { slotDao.insertAll(any()) } just Runs
        coEvery { overviewDao.insertAll(any()) } just Runs

        repository.savePlan(plan)

        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            planDao.insert(any())
            slotDao.deleteSlotsForPlan(1L)
            slotDao.insertAll(any())
        }
        coVerify { overviewDao.insertAll(match { it.any { e -> e.id == recipe.id } }) }
    }

    @Test
    fun `savePlan with empty slots does not call overviewDao insertAll`() = runTest {
        val plan = WeeklyPlan(id = 1L, planName = "Week A", daySlots = emptyMap())
        coEvery { planDao.insert(any()) } returns 1L
        coEvery { slotDao.deleteSlotsForPlan(1L) } just Runs
        coEvery { slotDao.insertAll(any()) } just Runs

        repository.savePlan(plan)

        coVerify(exactly = 0) { overviewDao.insertAll(any()) }
    }

    @Test
    fun `getPlanByName assembles WeeklyPlan from entity, slots, and overview`() = runTest {
        coEvery { planDao.getPlanByName("Week A") } returns WeeklyPlanEntity(id = 1L, planName = "Week A")
        coEvery { slotDao.getSlotsForPlan(1L) } returns listOf(
            WeeklyPlanSlotEntity(planId = 1L, dayIndex = 2, recipeId = 99)
        )
        coEvery { overviewDao.getRecipeById(99) } returns fakeOverviewEntity(99)

        val result = repository.getPlanByName("Week A")

        assertEquals("Week A", result.planName)
        assertEquals(1, result.daySlots.size)
        assertEquals(99, result.daySlots[2]?.first()?.id)
    }

    @Test
    fun `getPlanByName throws PlanNotFoundException when plan is not found`() = runTest {
        coEvery { planDao.getPlanByName("Unknown") } returns null

        assertFailsWith<PlanNotFoundException> { repository.getPlanByName("Unknown") }
    }

    @Test
    fun `getAllPlans returns assembled list of WeeklyPlans`() = runTest {
        coEvery { planDao.getAllPlans() } returns listOf(
            WeeklyPlanEntity(id = 1L, planName = "Plan 1"),
            WeeklyPlanEntity(id = 2L, planName = "Plan 2")
        )
        coEvery { slotDao.getSlotsForPlan(1L) } returns emptyList()
        coEvery { slotDao.getSlotsForPlan(2L) } returns emptyList()

        val result = repository.getAllPlans()

        assertEquals(2, result.size)
        assertEquals("Plan 1", result[0].planName)
    }

    @Test
    fun `getAllPlans returns empty list when no plans stored`() = runTest {
        coEvery { planDao.getAllPlans() } returns emptyList()

        assertEquals(emptyList(), repository.getAllPlans())
    }

    @Test
    fun `getPlanByName assembles two recipes on same day into a list`() = runTest {
        coEvery { planDao.getPlanByName("Week A") } returns WeeklyPlanEntity(id = 1L, planName = "Week A")
        coEvery { slotDao.getSlotsForPlan(1L) } returns listOf(
            WeeklyPlanSlotEntity(planId = 1L, dayIndex = 2, recipeId = 99),
            WeeklyPlanSlotEntity(planId = 1L, dayIndex = 2, recipeId = 100)
        )
        coEvery { overviewDao.getRecipeById(99) } returns fakeOverviewEntity(99)
        coEvery { overviewDao.getRecipeById(100) } returns fakeOverviewEntity(100)

        val result = repository.getPlanByName("Week A")

        assertEquals(1, result.daySlots.size)
        assertEquals(2, result.daySlots[2]?.size)
    }

    @Test
    fun `deletePlan calls planDao deleteById and slotDao deleteSlotsForPlan`() = runTest {
        coEvery { planDao.deleteById(5L) } just Runs
        coEvery { slotDao.deleteSlotsForPlan(5L) } just Runs

        repository.deletePlan(5L)

        coVerify(exactly = 1) { planDao.deleteById(5L) }
        coVerify(exactly = 1) { slotDao.deleteSlotsForPlan(5L) }
    }
}

private fun fakeOverviewDomain(id: Int = 1) = RecipeOverview(
    id = id,
    title = "Recipe $id",
    imageUrl = "https://example.com/$id.jpg",
    readyInMinutes = 30,
    dishType = "dinner"
)

private fun fakeOverviewEntity(id: Int) = RecipeOverviewEntity(
    id = id,
    title = "Recipe $id",
    imageUrl = "https://example.com/$id.jpg",
    readyInMinutes = 30,
    dishType = "dinner"
)
