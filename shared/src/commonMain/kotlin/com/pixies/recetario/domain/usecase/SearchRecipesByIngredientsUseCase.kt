package com.pixies.recetario.domain.usecase

import com.pixies.recetario.domain.model.IngredientSearchResult
import com.pixies.recetario.domain.repository.IngredientSearchSource

class SearchRecipesByIngredientsUseCase(private val repository: IngredientSearchSource) {
    suspend operator fun invoke(ingredients: List<String>): List<IngredientSearchResult> {
        if (ingredients.isEmpty()) return emptyList()
        return repository.searchByIngredients(ingredients)
    }
}
