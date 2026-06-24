package com.pixies.recetario.domain.usecase

import com.pixies.recetario.domain.model.RecipeOverview
import com.pixies.recetario.domain.repository.RandomRecipesSource

class GetRandomRecipesUseCase(private val repository: RandomRecipesSource) {
    suspend operator fun invoke(): List<RecipeOverview> =
        repository.getRandomRecipes()
}
