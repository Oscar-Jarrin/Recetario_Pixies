package com.pixies.recetario.domain.repository

import com.pixies.recetario.domain.model.IngredientSearchResult

interface IngredientSearchSource {
    suspend fun searchByIngredients(ingredients: List<String>): List<IngredientSearchResult>
}
