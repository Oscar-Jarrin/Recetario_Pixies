package com.pixies.recetario.domain.repository

import com.pixies.recetario.domain.model.WeeklyPlan

interface WeeklyPlanReader {
    suspend fun getAllPlans(): List<WeeklyPlan>
}
