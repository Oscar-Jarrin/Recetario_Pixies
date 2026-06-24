package com.pixies.recetario.domain.usecase

import com.pixies.recetario.domain.model.WeeklyPlan
import com.pixies.recetario.domain.repository.WeeklyPlanWriter

class SaveWeeklyPlanUseCase(private val repository: WeeklyPlanWriter) {
    suspend operator fun invoke(plan: WeeklyPlan) = repository.savePlan(plan)
}
