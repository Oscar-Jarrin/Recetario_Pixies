package com.pixies.recetario.domain

import com.pixies.recetario.domain.exception.NetworkException
import com.pixies.recetario.domain.exception.QuotaExhaustedException
import com.pixies.recetario.domain.model.RecipeOverview
import com.pixies.recetario.domain.repository.RandomRecipesSource
import com.pixies.recetario.domain.usecase.GetRandomRecipesUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetRandomRecipesUseCaseTest {

    private val repository: RandomRecipesSource = mockk()
    private val useCase = GetRandomRecipesUseCase(repository)

    @Test
    fun `happy path returns list from repository`() = runTest {
        val expected = listOf(fakeRecipeOverview())
        coEvery { repository.getRandomRecipes() } returns expected

        assertEquals(expected, useCase())
    }

    @Test
    fun `propagates NetworkException`() = runTest {
        coEvery { repository.getRandomRecipes() } throws
            NetworkException(RuntimeException("no internet"))

        assertFailsWith<NetworkException> { useCase() }
    }

    @Test
    fun `propagates QuotaExhaustedException`() = runTest {
        coEvery { repository.getRandomRecipes() } throws QuotaExhaustedException()

        assertFailsWith<QuotaExhaustedException> { useCase() }
    }

    @Test
    fun `returns empty list when repository returns empty`() = runTest {
        coEvery { repository.getRandomRecipes() } returns emptyList()

        assertEquals(emptyList(), useCase())
    }
}

private fun fakeRecipeOverview() = RecipeOverview(
    id = 1,
    title = "Test Recipe",
    imageUrl = "https://example.com/image.jpg",
    readyInMinutes = 30,
    dishType = "dinner"
)
