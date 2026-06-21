package com.pixies.recetario.domain.model

data class RecipeInstructions(
    val id: Int,
    val instructions: Map<String, List<String>>
)
