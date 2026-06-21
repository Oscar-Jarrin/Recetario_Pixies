package com.pixies.recetario.data

import com.pixies.recetario.data.local.dao.RecipeIngredientDao
import com.pixies.recetario.data.local.dao.RecipeInstructionStepDao
import com.pixies.recetario.data.local.dao.RecipeOverviewDao
import com.pixies.recetario.data.local.entity.RecipeInstructionStepEntity
import com.pixies.recetario.data.remote.SpoonacularApiService
import com.pixies.recetario.data.remote.dto.InstructionGroupDto
import com.pixies.recetario.data.remote.dto.StepDto
import com.pixies.recetario.domain.exception.NetworkException
import com.pixies.recetario.domain.exception.QuotaExhaustedException
import com.pixies.recetario.domain.exception.RecipeNotFoundException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecipeInstructionsRepositoryImplTest {

    private val api: SpoonacularApiService = mockk()
    private val overviewDao: RecipeOverviewDao = mockk()
    private val ingredientDao: RecipeIngredientDao = mockk()
    private val stepDao: RecipeInstructionStepDao = mockk()
    private val repository = RecipeRepositoryImpl(api, overviewDao, ingredientDao, stepDao)

    @Test
    fun `happy path fetches from API, deletes old steps, inserts new, returns map`() = runTest {
        coEvery { api.getAnalyzedInstructions(42) } returns listOf(
            InstructionGroupDto(name = "", steps = listOf(StepDto(1, "Boil water"), StepDto(2, "Add pasta"))),
            InstructionGroupDto(name = "Sauce", steps = listOf(StepDto(1, "Mix sauce")))
        )
        coEvery { stepDao.deleteStepsForRecipe(42) } just Runs
        coEvery { stepDao.insertAll(any()) } just Runs

        val result = repository.getRecipeInstructions(42)

        coVerify(exactly = 1) { stepDao.deleteStepsForRecipe(42) }
        coVerify(exactly = 1) { stepDao.insertAll(any()) }
        assertEquals(listOf("Boil water", "Add pasta"), result.instructions[""])
        assertEquals(listOf("Mix sauce"), result.instructions["Sauce"])
    }

    @Test
    fun `network failure falls back to cached steps`() = runTest {
        coEvery { api.getAnalyzedInstructions(42) } throws NetworkException(RuntimeException("offline"))
        coEvery { stepDao.getStepsForRecipe(42) } returns listOf(
            RecipeInstructionStepEntity(recipeId = 42, groupName = "", stepOrder = 1, stepText = "Cached step")
        )

        val result = repository.getRecipeInstructions(42)

        coVerify(exactly = 0) { stepDao.deleteStepsForRecipe(any()) }
        assertEquals(listOf("Cached step"), result.instructions[""])
    }

    @Test
    fun `quota exhausted falls back to cache same as network failure`() = runTest {
        coEvery { api.getAnalyzedInstructions(42) } throws QuotaExhaustedException()
        coEvery { stepDao.getStepsForRecipe(42) } returns listOf(
            RecipeInstructionStepEntity(recipeId = 42, groupName = "", stepOrder = 1, stepText = "Cached step")
        )

        val result = repository.getRecipeInstructions(42)

        assertEquals(listOf("Cached step"), result.instructions[""])
    }

    @Test
    fun `empty cache on failure throws RecipeNotFoundException`() = runTest {
        coEvery { api.getAnalyzedInstructions(42) } throws NetworkException(RuntimeException("offline"))
        coEvery { stepDao.getStepsForRecipe(42) } returns emptyList()

        assertFailsWith<RecipeNotFoundException> { repository.getRecipeInstructions(42) }
    }

    @Test
    fun `map reconstruction orders steps by stepOrder within each group`() = runTest {
        val rows = listOf(
            RecipeInstructionStepEntity(recipeId = 42, groupName = "A", stepOrder = 2, stepText = "Second"),
            RecipeInstructionStepEntity(recipeId = 42, groupName = "A", stepOrder = 1, stepText = "First"),
            RecipeInstructionStepEntity(recipeId = 42, groupName = "", stepOrder = 1, stepText = "Ungrouped")
        )
        coEvery { api.getAnalyzedInstructions(42) } throws NetworkException(RuntimeException("offline"))
        coEvery { stepDao.getStepsForRecipe(42) } returns rows

        val result = repository.getRecipeInstructions(42)

        assertEquals(listOf("First", "Second"), result.instructions["A"])
        assertEquals(listOf("Ungrouped"), result.instructions[""])
    }
}
