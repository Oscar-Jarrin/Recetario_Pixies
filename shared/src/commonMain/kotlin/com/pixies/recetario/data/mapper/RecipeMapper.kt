package com.pixies.recetario.data.mapper

import com.pixies.recetario.data.local.entity.RecipeIngredientEntity
import com.pixies.recetario.data.local.entity.RecipeOverviewEntity
import com.pixies.recetario.data.remote.dto.RecipeOverviewDto
import com.pixies.recetario.domain.model.RecipeOverview

fun RecipeOverviewDto.toDomain(): RecipeOverview = RecipeOverview(
    id = id,
    title = title,
    imageUrl = image,
    readyInMinutes = readyInMinutes,
    dishType = dishTypes.firstOrNull() ?: ""
)

fun RecipeOverviewDto.toEntity(): RecipeOverviewEntity = RecipeOverviewEntity(
    id = id,
    title = title,
    imageUrl = image,
    readyInMinutes = readyInMinutes,
    dishType = dishTypes.firstOrNull() ?: ""
)

fun RecipeOverviewDto.toIngredientEntities(): List<RecipeIngredientEntity> =
    extendedIngredients.map { RecipeIngredientEntity(recipeId = id, ingredientName = it.name) }

fun RecipeOverviewEntity.toDomain(): RecipeOverview = RecipeOverview(
    id = id,
    title = title,
    imageUrl = imageUrl,
    readyInMinutes = readyInMinutes,
    dishType = dishType
)

fun RecipeOverview.toEntity(): RecipeOverviewEntity = RecipeOverviewEntity(
    id = id,
    title = title,
    imageUrl = imageUrl,
    readyInMinutes = readyInMinutes,
    dishType = dishType
)
