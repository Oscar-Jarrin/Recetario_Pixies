package com.pixies.recetario.domain.usecase

import com.pixies.recetario.domain.repository.WeeklyPlanWriter

class DeleteWeeklyPlanUseCase(private val repository: WeeklyPlanWriter) {
    suspend operator fun invoke(id: Long) = repository.deletePlan(id)
}
