package com.pixies.recetario.presentation.search

import com.pixies.recetario.domain.model.IngredientSearchResult

sealed interface SearchState {
    object Idle : SearchState
    object Loading : SearchState
    data class Success(val results: List<IngredientSearchResult>) : SearchState
    data class Error(val exception: Exception) : SearchState
}
