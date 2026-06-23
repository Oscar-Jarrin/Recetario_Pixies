package com.pixies.recetario.presentation.planner

import com.pixies.recetario.domain.model.WeeklyPlan

sealed interface PlannerState {
    object Loading : PlannerState
    data class Success(val plans: List<WeeklyPlan>) : PlannerState
    data class Error(val exception: Exception) : PlannerState
}

sealed interface PlanDetailState {
    object Loading : PlanDetailState
    data class Success(val plan: WeeklyPlan) : PlanDetailState
    data class Error(val exception: Exception) : PlanDetailState
}
