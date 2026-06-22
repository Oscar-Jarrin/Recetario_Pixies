package com.pixies.recetario.domain.repository

import com.pixies.recetario.domain.model.WeeklyPlan

interface WeeklyPlanRepository {
    suspend fun getAllPlans(): List<WeeklyPlan>
    suspend fun getPlanByName(name: String): WeeklyPlan
    suspend fun savePlan(plan: WeeklyPlan)
    suspend fun deletePlan(id: Long)
}
