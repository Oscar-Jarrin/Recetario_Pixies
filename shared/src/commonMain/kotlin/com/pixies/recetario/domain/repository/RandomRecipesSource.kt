package com.pixies.recetario.domain.repository

import com.pixies.recetario.domain.model.RecipeOverview

interface RandomRecipesSource {
    suspend fun getRandomRecipes(): List<RecipeOverview>
}
