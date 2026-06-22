package com.pixies.recetario.domain.usecase

import com.pixies.recetario.domain.model.WeeklyPlan
import com.pixies.recetario.domain.repository.WeeklyPlanRepository

class GetAllWeeklyPlansUseCase(private val repository: WeeklyPlanRepository) {
    suspend operator fun invoke(): List<WeeklyPlan> = repository.getAllPlans()
}
