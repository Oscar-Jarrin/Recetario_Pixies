package com.pixies.recetario.data

import com.pixies.recetario.data.local.dao.RecipeIngredientDao
import com.pixies.recetario.data.local.dao.RecipeInstructionStepDao
import com.pixies.recetario.data.local.dao.RecipeOverviewDao
import com.pixies.recetario.data.mapper.buildOfflineResult
import com.pixies.recetario.data.mapper.toDomain
import com.pixies.recetario.data.mapper.toEntities
import com.pixies.recetario.data.mapper.toEntity
import com.pixies.recetario.data.mapper.toIngredientEntities
import com.pixies.recetario.data.mapper.toInstructionsMap
import com.pixies.recetario.data.remote.RANDOM_RECIPES_COUNT
import com.pixies.recetario.data.remote.SpoonacularApiService
import com.pixies.recetario.domain.exception.RecipeNotFoundException
import com.pixies.recetario.domain.model.IngredientSearchResult
import com.pixies.recetario.domain.model.RecipeInstructions
import com.pixies.recetario.domain.model.RecipeOverview
import com.pixies.recetario.domain.repository.RecipeRepository

class RecipeRepositoryImpl(
    private val api: SpoonacularApiService,
    private val overviewDao: RecipeOverviewDao,
    private val ingredientDao: RecipeIngredientDao,
    private val stepDao: RecipeInstructionStepDao
) : RecipeRepository {

    override suspend fun getRandomRecipes(count: Int): List<RecipeOverview> =
        runCatching { fetchAndCache(count) }.getOrElse { fallbackToCache() }

    override suspend fun searchByIngredients(ingredients: List<String>): List<IngredientSearchResult> =
        runCatching { fetchIngredientResults(ingredients) }
            .getOrElse { offlineIngredientSearch(ingredients) }

    override suspend fun getRecipeInstructions(id: Int): RecipeInstructions =
        runCatching { fetchAndCacheInstructions(id) }
            .getOrElse { fallbackInstructions(id) }

    private suspend fun fetchAndCache(count: Int): List<RecipeOverview> {
        val dtos = api.getRandomRecipes(count)
        overviewDao.insertAll(dtos.map { it.toEntity() })
        ingredientDao.insertAll(dtos.flatMap { it.toIngredientEntities() })
        return dtos.map { it.toDomain() }
    }

    private suspend fun fallbackToCache(): List<RecipeOverview> =
        overviewDao.getAllRecipes().map { it.toDomain() }

    private suspend fun fetchIngredientResults(ingredients: List<String>): List<IngredientSearchResult> {
        val dtos = api.findByIngredients(ingredients.joinToString(","), RANDOM_RECIPES_COUNT)
        return dtos.map { it.toDomain() }
    }

    private suspend fun offlineIngredientSearch(ingredients: List<String>): List<IngredientSearchResult> {
        val recipeIds = ingredientDao.getRecipeIdsMatchingIngredients(ingredients)
        val overviews = recipeIds.mapNotNull { overviewDao.getRecipeById(it) }
        val allCached = ingredientDao.getIngredientsForRecipes(recipeIds)
        return overviews.map { entity -> buildOfflineResult(entity, allCached, ingredients) }
    }

    private suspend fun fetchAndCacheInstructions(id: Int): RecipeInstructions {
        val groups = api.getAnalyzedInstructions(id)
        stepDao.deleteStepsForRecipe(id)
        stepDao.insertAll(groups.toEntities(id))
        val map = groups.associate { g -> g.name to g.steps.sortedBy { it.number }.map { it.step } }
        return RecipeInstructions(id, map)
    }

    private suspend fun fallbackInstructions(id: Int): RecipeInstructions {
        val steps = stepDao.getStepsForRecipe(id)
        if (steps.isEmpty()) throw RecipeNotFoundException(id)
        return RecipeInstructions(id, steps.toInstructionsMap())
    }
}
