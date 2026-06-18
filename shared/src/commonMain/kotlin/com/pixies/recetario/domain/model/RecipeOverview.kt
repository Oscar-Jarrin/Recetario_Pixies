package com.pixies.recetario.domain.model

data class RecipeOverview(
    val id: Int,
    val title: String,
    val imageUrl: String,
    val readyInMinutes: Int,
    val dishType: String
)
