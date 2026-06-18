package com.pixies.recetario.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class IngredientSearchResponseDto(
    val id: Int,
    val title: String,
    val image: String,
    val missedIngredients: List<IngredientItemDto>,
    val usedIngredients: List<IngredientItemDto>,
    val unusedIngredients: List<IngredientItemDto>
)

@Serializable
data class IngredientItemDto(val name: String)
