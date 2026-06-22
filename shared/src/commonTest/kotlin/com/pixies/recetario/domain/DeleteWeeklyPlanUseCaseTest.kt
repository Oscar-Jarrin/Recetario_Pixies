package com.pixies.recetario.domain

import com.pixies.recetario.domain.repository.WeeklyPlanRepository
import com.pixies.recetario.domain.usecase.DeleteWeeklyPlanUseCase
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeleteWeeklyPlanUseCaseTest {

    private val repository: WeeklyPlanRepository = mockk()
    private val useCase = DeleteWeeklyPlanUseCase(repository)

    @Test
    fun `delegates deletePlan to repository with correct id`() = runTest {
        coJustRun { repository.deletePlan(42L) }

        useCase(42L)

        coVerify(exactly = 1) { repository.deletePlan(42L) }
    }
}
