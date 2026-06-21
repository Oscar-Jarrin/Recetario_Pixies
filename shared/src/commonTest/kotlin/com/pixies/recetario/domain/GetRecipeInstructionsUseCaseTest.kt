package com.pixies.recetario.domain

import com.pixies.recetario.domain.exception.NetworkException
import com.pixies.recetario.domain.exception.QuotaExhaustedException
import com.pixies.recetario.domain.exception.RecipeNotFoundException
import com.pixies.recetario.domain.model.RecipeInstructions
import com.pixies.recetario.domain.repository.RecipeRepository
import com.pixies.recetario.domain.usecase.GetRecipeInstructionsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetRecipeInstructionsUseCaseTest {

    private val repository: RecipeRepository = mockk()
    private val useCase = GetRecipeInstructionsUseCase(repository)

    @Test
    fun `happy path returns instructions from repository`() = runTest {
        val instructions = fakeInstructions()
        coEvery { repository.getRecipeInstructions(42) } returns instructions

        val result = useCase(42)

        assertEquals(instructions, result)
    }

    @Test
    fun `network failure propagates NetworkException`() = runTest {
        coEvery { repository.getRecipeInstructions(42) } throws NetworkException(RuntimeException("no internet"))

        assertFailsWith<NetworkException> { useCase(42) }
    }

    @Test
    fun `quota exhausted propagates QuotaExhaustedException`() = runTest {
        coEvery { repository.getRecipeInstructions(42) } throws QuotaExhaustedException()

        assertFailsWith<QuotaExhaustedException> { useCase(42) }
    }

    @Test
    fun `recipe not in cache propagates RecipeNotFoundException`() = runTest {
        coEvery { repository.getRecipeInstructions(42) } throws RecipeNotFoundException(42)

        assertFailsWith<RecipeNotFoundException> { useCase(42) }
    }
}

private fun fakeInstructions() = RecipeInstructions(
    id = 42,
    instructions = mapOf(
        "" to listOf("Boil water", "Add pasta"),
        "Sauce" to listOf("Mix sauce")
    )
)
