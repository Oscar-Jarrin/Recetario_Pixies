package com.pixies.recetario.domain

import com.pixies.recetario.domain.model.RecipeOverview
import com.pixies.recetario.domain.model.WeeklyPlan
import com.pixies.recetario.domain.repository.WeeklyPlanRepository
import com.pixies.recetario.domain.usecase.GetAllWeeklyPlansUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAllWeeklyPlansUseCaseTest {

    private val repository: WeeklyPlanRepository = mockk()
    private val useCase = GetAllWeeklyPlansUseCase(repository)

    @Test
    fun `happy path returns list from repository`() = runTest {
        val expected = listOf(fakeWeeklyPlan())
        coEvery { repository.getAllPlans() } returns expected

        assertEquals(expected, useCase())
    }

    @Test
    fun `returns empty list when repository returns empty`() = runTest {
        coEvery { repository.getAllPlans() } returns emptyList()

        assertEquals(emptyList(), useCase())
    }
}

internal fun fakeWeeklyPlan(
    id: Long = 1L,
    planName: String = "Test Plan",
    daySlots: Map<Int, List<RecipeOverview>> = emptyMap()
) = WeeklyPlan(id = id, planName = planName, daySlots = daySlots)

internal fun fakeRecipeOverview(id: Int = 1) = RecipeOverview(
    id = id,
    title = "Test Recipe",
    imageUrl = "https://example.com/image.jpg",
    readyInMinutes = 30,
    dishType = "dinner"
)
