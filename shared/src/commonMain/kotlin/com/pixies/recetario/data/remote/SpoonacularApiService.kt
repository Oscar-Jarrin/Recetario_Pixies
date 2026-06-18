package com.pixies.recetario.data.remote

import com.pixies.recetario.data.remote.dto.IngredientSearchResponseDto
import com.pixies.recetario.data.remote.dto.RecipeOverviewDto

interface SpoonacularApiService {
    suspend fun getRandomRecipes(count: Int): List<RecipeOverviewDto>
    suspend fun findByIngredients(ingredients: String, count: Int): List<IngredientSearchResponseDto>
}
