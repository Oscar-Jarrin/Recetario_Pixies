package com.pixies.recetario.domain

import com.pixies.recetario.domain.repository.WeeklyPlanWriter
import com.pixies.recetario.domain.usecase.DeleteWeeklyPlanUseCase
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeleteWeeklyPlanUseCaseTest {

    private val repository: WeeklyPlanWriter = mockk()
    private val useCase = DeleteWeeklyPlanUseCase(repository)

    @Test
    fun `delegates deletePlan to repository with correct id`() = runTest {
        coJustRun { repository.deletePlan(42L) }

        useCase(42L)

        coVerify(exactly = 1) { repository.deletePlan(42L) }
    }
}
