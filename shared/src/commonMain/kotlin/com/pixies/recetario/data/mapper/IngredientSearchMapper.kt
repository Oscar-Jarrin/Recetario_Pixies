package com.pixies.recetario.data.mapper

import com.pixies.recetario.data.local.entity.RecipeIngredientEntity
import com.pixies.recetario.data.local.entity.RecipeOverviewEntity
import com.pixies.recetario.data.remote.dto.IngredientSearchResponseDto
import com.pixies.recetario.domain.model.IngredientSearchResult

fun IngredientSearchResponseDto.toDomain(): IngredientSearchResult = IngredientSearchResult(
    id = id,
    title = title,
    imageUrl = image,
    missingIngredients = missedIngredients.map { it.originalName },
    usedIngredients = usedIngredients.map { it.name },
    unusedIngredients = unusedIngredients.map { it.name }
)

fun categoriseOffline(
    cachedIngredients: List<String>,
    userIngredients: List<String>
): Triple<List<String>, List<String>, List<String>> {
    val userSet = userIngredients.toSet()
    val cachedSet = cachedIngredients.toSet()
    return Triple(
        cachedIngredients.filter { it in userSet },
        userIngredients.filter { it !in cachedSet },
        cachedIngredients.filter { it !in userSet }
    )
}

fun buildOfflineResult(
    entity: RecipeOverviewEntity,
    allCached: List<RecipeIngredientEntity>,
    userIngredients: List<String>
): IngredientSearchResult {
    val cachedNames = allCached.filter { it.recipeId == entity.id }.map { it.ingredientName }
    val (used, missing, unused) = categoriseOffline(cachedNames, userIngredients)
    return IngredientSearchResult(
        id = entity.id,
        title = entity.title,
        imageUrl = entity.imageUrl,
        usedIngredients = used,
        missingIngredients = missing,
        unusedIngredients = unused
    )
}
