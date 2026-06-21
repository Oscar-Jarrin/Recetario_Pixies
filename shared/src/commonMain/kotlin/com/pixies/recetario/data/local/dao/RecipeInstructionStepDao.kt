package com.pixies.recetario.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixies.recetario.data.local.entity.RecipeInstructionStepEntity

@Dao
interface RecipeInstructionStepDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(steps: List<RecipeInstructionStepEntity>)

    @Query("SELECT * FROM recipe_instruction_steps WHERE recipeId = :id ORDER BY rowId ASC")
    suspend fun getStepsForRecipe(id: Int): List<RecipeInstructionStepEntity>

    @Query("DELETE FROM recipe_instruction_steps WHERE recipeId = :id")
    suspend fun deleteStepsForRecipe(id: Int)
}
