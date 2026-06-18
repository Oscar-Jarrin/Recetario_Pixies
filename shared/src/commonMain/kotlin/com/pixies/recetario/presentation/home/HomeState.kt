package com.pixies.recetario.presentation.home

import com.pixies.recetario.domain.model.RecipeOverview

sealed interface HomeState {
    data object Loading : HomeState
    data class Success(val recipes: List<RecipeOverview>) : HomeState
    data class Error(val exception: Exception) : HomeState
}
