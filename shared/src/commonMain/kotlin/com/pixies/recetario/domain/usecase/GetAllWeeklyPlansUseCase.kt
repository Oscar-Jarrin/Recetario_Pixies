package com.pixies.recetario.domain.usecase

import com.pixies.recetario.domain.model.WeeklyPlan
import com.pixies.recetario.domain.repository.WeeklyPlanReader

class GetAllWeeklyPlansUseCase(private val repository: WeeklyPlanReader) {
    suspend operator fun invoke(): List<WeeklyPlan> = repository.getAllPlans()
}
