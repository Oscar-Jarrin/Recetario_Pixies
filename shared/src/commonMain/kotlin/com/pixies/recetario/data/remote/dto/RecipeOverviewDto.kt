package com.pixies.recetario.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RandomRecipesResponseDto(val recipes: List<RecipeOverviewDto>)

@Serializable
data class RecipeOverviewDto(
    val id: Int,
    val title: String,
    val image: String,
    val readyInMinutes: Int,
    val dishTypes: List<String> = emptyList(),
    val extendedIngredients: List<ExtendedIngredientDto> = emptyList()
)

@Serializable
data class ExtendedIngredientDto(val name: String)
