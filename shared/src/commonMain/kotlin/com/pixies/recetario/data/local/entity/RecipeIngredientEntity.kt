package com.pixies.recetario.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipe_ingredients")
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val recipeId: Int,
    val ingredientName: String
)
