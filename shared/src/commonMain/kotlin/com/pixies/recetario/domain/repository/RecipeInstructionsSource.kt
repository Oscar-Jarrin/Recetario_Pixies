package com.pixies.recetario.domain.repository

import com.pixies.recetario.domain.model.RecipeInstructions

interface RecipeInstructionsSource {
    suspend fun getRecipeInstructions(id: Int): RecipeInstructions
}
