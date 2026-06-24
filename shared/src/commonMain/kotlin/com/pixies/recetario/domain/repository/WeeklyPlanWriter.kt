package com.pixies.recetario.domain.repository

import com.pixies.recetario.domain.model.WeeklyPlan

interface WeeklyPlanWriter {
    suspend fun savePlan(plan: WeeklyPlan)
    suspend fun deletePlan(id: Long)
}
