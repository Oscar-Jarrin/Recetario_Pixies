package com.pixies.recetario.domain

import com.pixies.recetario.domain.exception.NetworkException
import com.pixies.recetario.domain.exception.QuotaExhaustedException
import com.pixies.recetario.domain.model.IngredientSearchResult
import com.pixies.recetario.domain.repository.RecipeRepository
import com.pixies.recetario.domain.usecase.SearchRecipesByIngredientsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchRecipesByIngredientsUseCaseTest {

    private val repo: RecipeRepository = mockk()
    private val useCase = SearchRecipesByIngredientsUseCase(repo)

    @Test
    fun `happy path returns results from repository`() = runTest {
        val ingredients = listOf("apple", "flour")
        coEvery { repo.searchByIngredients(ingredients) } returns listOf(fakeResult())

        val result = useCase(ingredients)

        assertEquals(listOf(fakeResult()), result)
    }

    @Test
    fun `empty ingredients list returns empty without calling repository`() = runTest {
        val result = useCase(emptyList())

        coVerify(exactly = 0) { repo.searchByIngredients(any()) }
        assertEquals(emptyList(), result)
    }

    @Test
    fun `network failure propagates exception`() = runTest {
        coEvery { repo.searchByIngredients(any()) } throws NetworkException(RuntimeException("offline"))

        val ex = runCatching { useCase(listOf("apple")) }.exceptionOrNull()

        assertTrue(ex is NetworkException)
    }

    @Test
    fun `quota exhausted propagates exception`() = runTest {
        coEvery { repo.searchByIngredients(any()) } throws QuotaExhaustedException()

        val ex = runCatching { useCase(listOf("apple")) }.exceptionOrNull()

        assertTrue(ex is QuotaExhaustedException)
    }
}

private fun fakeResult() = IngredientSearchResult(
    id = 1,
    title = "Apple Flour Cake",
    imageUrl = "https://example.com/cake.jpg",
    missingIngredients = listOf("sugar"),
    usedIngredients = listOf("apple", "flour"),
    unusedIngredients = emptyList()
)
