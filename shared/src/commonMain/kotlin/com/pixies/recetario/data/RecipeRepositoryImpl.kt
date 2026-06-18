package com.pixies.recetario.data

import com.pixies.recetario.data.local.dao.RecipeIngredientDao
import com.pixies.recetario.data.local.dao.RecipeOverviewDao
import com.pixies.recetario.data.mapper.toDomain
import com.pixies.recetario.data.mapper.toEntity
import com.pixies.recetario.data.mapper.toIngredientEntities
import com.pixies.recetario.data.remote.SpoonacularApiService
import com.pixies.recetario.domain.model.RecipeOverview
import com.pixies.recetario.domain.repository.RecipeRepository

class RecipeRepositoryImpl(
    private val api: SpoonacularApiService,
    private val overviewDao: RecipeOverviewDao,
    private val ingredientDao: RecipeIngredientDao
) : RecipeRepository {

    override suspend fun getRandomRecipes(count: Int): List<RecipeOverview> =
        runCatching { fetchAndCache(count) }.getOrElse { fallbackToCache() }

    private suspend fun fetchAndCache(count: Int): List<RecipeOverview> {
        val dtos = api.getRandomRecipes(count)
        overviewDao.insertAll(dtos.map { it.toEntity() })
        ingredientDao.insertAll(dtos.flatMap { it.toIngredientEntities() })
        return dtos.map { it.toDomain() }
    }

    private suspend fun fallbackToCache(): List<RecipeOverview> =
        overviewDao.getAllRecipes().map { it.toDomain() }
}
