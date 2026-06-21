package com.pixies.recetario.domain.usecase

import com.pixies.recetario.domain.model.RecipeInstructions
import com.pixies.recetario.domain.repository.RecipeRepository

class GetRecipeInstructionsUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke(id: Int): RecipeInstructions =
        repository.getRecipeInstructions(id)
}
