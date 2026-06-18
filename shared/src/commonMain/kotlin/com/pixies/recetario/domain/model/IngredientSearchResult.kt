package com.pixies.recetario.domain.model

data class IngredientSearchResult(
    val id: Int,
    val title: String,
    val imageUrl: String,
    val missingIngredients: List<String>,
    val usedIngredients: List<String>,
    val unusedIngredients: List<String>
)
