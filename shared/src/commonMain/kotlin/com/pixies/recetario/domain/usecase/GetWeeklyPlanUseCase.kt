package com.pixies.recetario.domain.usecase

import com.pixies.recetario.domain.model.WeeklyPlan
import com.pixies.recetario.domain.repository.WeeklyPlanRepository

class GetWeeklyPlanUseCase(private val repository: WeeklyPlanRepository) {
    suspend operator fun invoke(name: String): WeeklyPlan = repository.getPlanByName(name)
}
