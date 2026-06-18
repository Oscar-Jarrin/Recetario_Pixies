package com.pixies.recetario.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipe_overviews")
data class RecipeOverviewEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val imageUrl: String,
    val readyInMinutes: Int,
    val dishType: String
)
