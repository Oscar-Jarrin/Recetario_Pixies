package com.pixies.recetario.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixies.recetario.data.local.entity.RecipeOverviewEntity

@Dao
interface RecipeOverviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recipes: List<RecipeOverviewEntity>)

    @Query("SELECT * FROM recipe_overviews")
    suspend fun getAllRecipes(): List<RecipeOverviewEntity>

    @Query("SELECT * FROM recipe_overviews WHERE id = :id")
    suspend fun getRecipeById(id: Int): RecipeOverviewEntity?
}
