package com.pixies.recetario.presentation.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixies.recetario.domain.model.RecipeOverview
import com.pixies.recetario.domain.model.WeeklyPlan
import com.pixies.recetario.domain.usecase.DeleteWeeklyPlanUseCase
import com.pixies.recetario.domain.usecase.GetAllWeeklyPlansUseCase
import com.pixies.recetario.domain.usecase.SaveWeeklyPlanUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlannerViewModel(
    private val getAllWeeklyPlans: GetAllWeeklyPlansUseCase,
    private val saveWeeklyPlan: SaveWeeklyPlanUseCase,
    private val deleteWeeklyPlan: DeleteWeeklyPlanUseCase
) : ViewModel() {

    private val _plannerState = MutableStateFlow<PlannerState>(PlannerState.Loading)
    val plannerState: StateFlow<PlannerState> = _plannerState.asStateFlow()

    private val _detailState = MutableStateFlow<PlanDetailState>(PlanDetailState.Loading)
    val detailState: StateFlow<PlanDetailState> = _detailState.asStateFlow()

    private var currentPlan: WeeklyPlan? = null

    init { loadAll() }

    fun loadAll() {
        viewModelScope.launch {
            _plannerState.value = PlannerState.Loading
            _plannerState.value = runCatching { PlannerState.Success(getAllWeeklyPlans()) }
                .getOrElse { PlannerState.Error(it as Exception) }
        }
    }

    fun createPlan(name: String) {
        viewModelScope.launch {
            runCatching { saveWeeklyPlan(WeeklyPlan(planName = name, daySlots = emptyMap())) }
            loadAll()
        }
    }

    fun deletePlan(id: Long) {
        viewModelScope.launch {
            runCatching { deleteWeeklyPlan(id) }
            loadAll()
        }
    }

    fun selectPlan(plan: WeeklyPlan) {
        currentPlan = plan
        _detailState.value = PlanDetailState.Success(plan = plan)
    }

    fun assignRecipe(dayIndex: Int, recipe: RecipeOverview) {
        val plan = currentPlan ?: return
        viewModelScope.launch {
            val updated = plan.copy(daySlots = plan.daySlots + (dayIndex to recipe))
            currentPlan = updated
            runCatching { saveWeeklyPlan(updated) }
            _detailState.value = (_detailState.value as? PlanDetailState.Success)
                ?.copy(plan = updated) ?: _detailState.value
        }
    }

    fun removeRecipe(dayIndex: Int) {
        val plan = currentPlan ?: return
        viewModelScope.launch {
            val updated = plan.copy(daySlots = plan.daySlots - dayIndex)
            currentPlan = updated
            runCatching { saveWeeklyPlan(updated) }
            _detailState.value = (_detailState.value as? PlanDetailState.Success)
                ?.copy(plan = updated) ?: _detailState.value
        }
    }
}
