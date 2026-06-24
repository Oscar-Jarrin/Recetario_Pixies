package com.pixies.recetario.domain.usecase

import com.pixies.recetario.domain.model.RecipeOverview
import com.pixies.recetario.domain.repository.RecipeRepository

class GetRandomRecipesUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke(): List<RecipeOverview> =
        repository.getRandomRecipes()
}
