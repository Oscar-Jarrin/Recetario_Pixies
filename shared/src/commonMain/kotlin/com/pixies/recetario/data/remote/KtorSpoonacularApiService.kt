package com.pixies.recetario.data.remote

import com.pixies.recetario.data.remote.dto.IngredientSearchResponseDto
import com.pixies.recetario.data.remote.dto.InstructionGroupDto
import com.pixies.recetario.data.remote.dto.RandomRecipesResponseDto
import com.pixies.recetario.data.remote.dto.RecipeOverviewDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class KtorSpoonacularApiService(private val client: HttpClient) : SpoonacularApiService {

    override suspend fun getRandomRecipes(count: Int): List<RecipeOverviewDto> =
        client.get("$BASE_URL/recipes/random") {
            parameter("number", count)
        }.body<RandomRecipesResponseDto>().recipes

    override suspend fun findByIngredients(
        ingredients: String,
        count: Int
    ): List<IngredientSearchResponseDto> =
        client.get("$BASE_URL/recipes/findByIngredients") {
            parameter("ingredients", ingredients)
            parameter("number", count)
            parameter("ranking", MINIMIZE_MISSING_INGREDIENTS)
        }.body()

    override suspend fun getAnalyzedInstructions(id: Int): List<InstructionGroupDto> =
        client.get("$BASE_URL/recipes/$id/analyzedInstructions").body()
}
