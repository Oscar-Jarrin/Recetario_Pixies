package com.pixies.recetario.domain.model

data class WeeklyPlan(
    val id: Long = 0,
    val planName: String,
    val daySlots: Map<Int, List<RecipeOverview>>
)
