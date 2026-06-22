package com.pixies.recetario.domain

import com.pixies.recetario.domain.exception.PlanNotFoundException
import com.pixies.recetario.domain.repository.WeeklyPlanRepository
import com.pixies.recetario.domain.usecase.GetWeeklyPlanUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetWeeklyPlanUseCaseTest {

    private val repository: WeeklyPlanRepository = mockk()
    private val useCase = GetWeeklyPlanUseCase(repository)

    @Test
    fun `happy path returns plan by name`() = runTest {
        val expected = fakeWeeklyPlan(planName = "Semana Saludable")
        coEvery { repository.getPlanByName("Semana Saludable") } returns expected

        assertEquals(expected, useCase("Semana Saludable"))
    }

    @Test
    fun `propagates PlanNotFoundException when plan not found`() = runTest {
        coEvery { repository.getPlanByName("Unknown") } throws PlanNotFoundException("Unknown")

        assertFailsWith<PlanNotFoundException> { useCase("Unknown") }
    }
}
