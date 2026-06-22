package com.pixies.recetario.domain.usecase

import com.pixies.recetario.domain.model.WeeklyPlan
import com.pixies.recetario.domain.repository.WeeklyPlanRepository

class SaveWeeklyPlanUseCase(private val repository: WeeklyPlanRepository) {
    suspend operator fun invoke(plan: WeeklyPlan) = repository.savePlan(plan)
}
