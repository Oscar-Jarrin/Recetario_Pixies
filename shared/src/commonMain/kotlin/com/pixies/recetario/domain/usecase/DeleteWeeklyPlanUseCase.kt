package com.pixies.recetario.domain.usecase

import com.pixies.recetario.domain.repository.WeeklyPlanRepository

class DeleteWeeklyPlanUseCase(private val repository: WeeklyPlanRepository) {
    suspend operator fun invoke(id: Long) = repository.deletePlan(id)
}
