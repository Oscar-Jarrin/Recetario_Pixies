package com.pixies.recetario.domain

import com.pixies.recetario.domain.repository.WeeklyPlanRepository
import com.pixies.recetario.domain.usecase.SaveWeeklyPlanUseCase
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SaveWeeklyPlanUseCaseTest {

    private val repository: WeeklyPlanRepository = mockk()
    private val useCase = SaveWeeklyPlanUseCase(repository)

    @Test
    fun `delegates savePlan to repository`() = runTest {
        val plan = fakeWeeklyPlan()
        coJustRun { repository.savePlan(plan) }

        useCase(plan)

        coVerify(exactly = 1) { repository.savePlan(plan) }
    }

    @Test
    fun `saves plan with populated day slots`() = runTest {
        val recipe = fakeRecipeOverview()
        val plan = fakeWeeklyPlan(daySlots = mapOf(0 to recipe, 3 to recipe))
        coJustRun { repository.savePlan(plan) }

        useCase(plan)

        coVerify(exactly = 1) { repository.savePlan(plan) }
    }
}
