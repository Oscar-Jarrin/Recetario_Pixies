package com.pixies.recetario.domain.repository

import com.pixies.recetario.domain.model.RecipeOverview

interface RecipeRepository {
    suspend fun getRandomRecipes(count: Int): List<RecipeOverview>
}
