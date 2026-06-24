package com.pixies.recetario.domain.usecase

import com.pixies.recetario.domain.model.RecipeInstructions
import com.pixies.recetario.domain.repository.RecipeInstructionsSource

class GetRecipeInstructionsUseCase(private val repository: RecipeInstructionsSource) {
    suspend operator fun invoke(id: Int): RecipeInstructions =
        repository.getRecipeInstructions(id)
}
