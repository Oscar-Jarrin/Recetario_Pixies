package com.pixies.recetario.presentation.detail

import com.pixies.recetario.domain.model.RecipeInstructions

sealed interface DetailState {
    object Loading : DetailState
    data class Success(val instructions: RecipeInstructions) : DetailState
    data class Error(val exception: Exception) : DetailState
}
