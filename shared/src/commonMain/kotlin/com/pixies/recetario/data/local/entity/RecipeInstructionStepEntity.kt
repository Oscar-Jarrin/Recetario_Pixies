package com.pixies.recetario.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipe_instruction_steps")
data class RecipeInstructionStepEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val recipeId: Int,
    val groupName: String,
    val stepOrder: Int,
    val stepText: String
)