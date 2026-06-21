package com.pixies.recetario.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.pixies.recetario.data.local.dao.RecipeIngredientDao
import com.pixies.recetario.data.local.dao.RecipeInstructionStepDao
import com.pixies.recetario.data.local.dao.RecipeOverviewDao
import com.pixies.recetario.data.local.entity.RecipeIngredientEntity
import com.pixies.recetario.data.local.entity.RecipeInstructionStepEntity
import com.pixies.recetario.data.local.entity.RecipeOverviewEntity

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>

@Database(
    entities = [
        RecipeOverviewEntity::class,
        RecipeIngredientEntity::class,
        RecipeInstructionStepEntity::class
    ],
    version = 2
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeOverviewDao(): RecipeOverviewDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao
    abstract fun recipeInstructionStepDao(): RecipeInstructionStepDao
}
