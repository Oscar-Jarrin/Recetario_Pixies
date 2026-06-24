package com.pixies.recetario.domain.repository

import com.pixies.recetario.domain.model.IngredientSearchResult
import com.pixies.recetario.domain.model.RecipeInstructions
import com.pixies.recetario.domain.model.RecipeOverview

interface RecipeRepository {
    suspend fun getRandomRecipes(): List<RecipeOverview>
    suspend fun searchByIngredients(ingredients: List<String>): List<IngredientSearchResult>
    suspend fun getRecipeInstructions(id: Int): RecipeInstructions
}
