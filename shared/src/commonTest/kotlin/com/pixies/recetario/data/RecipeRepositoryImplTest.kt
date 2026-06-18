package com.pixies.recetario.data

import com.pixies.recetario.data.local.dao.RecipeIngredientDao
import com.pixies.recetario.data.local.dao.RecipeOverviewDao
import com.pixies.recetario.data.local.entity.RecipeIngredientEntity
import com.pixies.recetario.data.local.entity.RecipeOverviewEntity
import com.pixies.recetario.data.mapper.categoriseOffline
import com.pixies.recetario.data.remote.SpoonacularApiService
import com.pixies.recetario.data.remote.dto.ExtendedIngredientDto
import com.pixies.recetario.data.remote.dto.IngredientItemDto
import com.pixies.recetario.data.remote.dto.IngredientSearchResponseDto
import com.pixies.recetario.data.remote.dto.RecipeOverviewDto
import com.pixies.recetario.domain.exception.NetworkException
import com.pixies.recetario.domain.exception.QuotaExhaustedException
import com.pixies.recetario.domain.model.RecipeOverview
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RecipeRepositoryImplTest {

    private val api: SpoonacularApiService = mockk()
    private val overviewDao: RecipeOverviewDao = mockk()
    private val ingredientDao: RecipeIngredientDao = mockk()
    private val repository = RecipeRepositoryImpl(api, overviewDao, ingredientDao)

    @Test
    fun `happy path fetches from API, caches both tables, returns domain models`() = runTest {
        coEvery { api.getRandomRecipes(any()) } returns listOf(fakeDto())
        coEvery { overviewDao.insertAll(any()) } just Runs
        coEvery { ingredientDao.insertAll(any()) } just Runs

        val result = repository.getRandomRecipes(10)

        coVerify(exactly = 1) { overviewDao.insertAll(any()) }
        coVerify(exactly = 1) { ingredientDao.insertAll(any()) }
        assertEquals(listOf(fakeDomain()), result)
    }

    @Test
    fun `network failure falls back to cache, insertAll never called`() = runTest {
        coEvery { api.getRandomRecipes(any()) } throws NetworkException(RuntimeException("no internet"))
        coEvery { overviewDao.getAllRecipes() } returns listOf(fakeEntity())

        val result = repository.getRandomRecipes(10)

        coVerify(exactly = 0) { overviewDao.insertAll(any()) }
        coVerify(exactly = 1) { overviewDao.getAllRecipes() }
        assertEquals(listOf(fakeDomain()), result)
    }

    @Test
    fun `quota exhausted falls back to cache`() = runTest {
        coEvery { api.getRandomRecipes(any()) } throws QuotaExhaustedException()
        coEvery { overviewDao.getAllRecipes() } returns listOf(fakeEntity())

        val result = repository.getRandomRecipes(10)

        coVerify(exactly = 0) { overviewDao.insertAll(any()) }
        coVerify(exactly = 1) { overviewDao.getAllRecipes() }
        assertEquals(listOf(fakeDomain()), result)
    }

    @Test
    fun `cold cache with network failure returns empty list`() = runTest {
        coEvery { api.getRandomRecipes(any()) } throws NetworkException(RuntimeException("offline"))
        coEvery { overviewDao.getAllRecipes() } returns emptyList()

        val result = repository.getRandomRecipes(10)

        assertEquals(emptyList(), result)
    }
}

private fun fakeDto() = RecipeOverviewDto(
    id = 1,
    title = "Test Recipe",
    image = "https://example.com/image.jpg",
    readyInMinutes = 30,
    dishTypes = listOf("dinner"),
    extendedIngredients = listOf(ExtendedIngredientDto("pasta"))
)

private fun fakeEntity() = RecipeOverviewEntity(
    id = 1,
    title = "Test Recipe",
    imageUrl = "https://example.com/image.jpg",
    readyInMinutes = 30,
    dishType = "dinner"
)

private fun fakeDomain() = RecipeOverview(
    id = 1,
    title = "Test Recipe",
    imageUrl = "https://example.com/image.jpg",
    readyInMinutes = 30,
    dishType = "dinner"
)

class SearchByIngredientsTest {

    private val api: SpoonacularApiService = mockk()
    private val overviewDao: RecipeOverviewDao = mockk()
    private val ingredientDao: RecipeIngredientDao = mockk()
    private val repository = RecipeRepositoryImpl(api, overviewDao, ingredientDao)

    @Test
    fun `happy path maps DTOs directly without overviewDao lookup`() = runTest {
        coEvery { api.findByIngredients(any(), any()) } returns listOf(fakeIngredientDto())

        val result = repository.searchByIngredients(listOf("apple", "flour"))

        coVerify(exactly = 0) { overviewDao.getRecipeById(any()) }
        assertEquals(1, result.size)
        assertEquals(listOf("apple"), result[0].usedIngredients)
        assertEquals(listOf("sugar"), result[0].missingIngredients)
    }

    @Test
    fun `network failure falls back to offline categorisation`() = runTest {
        val inputs = listOf("flour", "sugar")
        coEvery { api.findByIngredients(any(), any()) } throws NetworkException(RuntimeException("offline"))
        coEvery { ingredientDao.getRecipeIdsMatchingIngredients(inputs) } returns listOf(42)
        coEvery { overviewDao.getRecipeById(42) } returns fakeOverviewEntity(42)
        coEvery { ingredientDao.getIngredientsForRecipes(listOf(42)) } returns listOf(
            RecipeIngredientEntity(recipeId = 42, ingredientName = "butter"),
            RecipeIngredientEntity(recipeId = 42, ingredientName = "egg"),
            RecipeIngredientEntity(recipeId = 42, ingredientName = "flour")
        )

        val result = repository.searchByIngredients(inputs)

        assertEquals(1, result.size)
        assertEquals(listOf("flour"), result[0].usedIngredients)
        assertEquals(listOf("sugar"), result[0].missingIngredients)
        assertEquals(listOf("butter", "egg"), result[0].unusedIngredients.sorted())
    }

    @Test
    fun `quota exhausted falls back to offline, empty cache returns empty list`() = runTest {
        val inputs = listOf("flour")
        coEvery { api.findByIngredients(any(), any()) } throws QuotaExhaustedException()
        coEvery { ingredientDao.getRecipeIdsMatchingIngredients(inputs) } returns emptyList()
        coEvery { ingredientDao.getIngredientsForRecipes(emptyList()) } returns emptyList()

        val result = repository.searchByIngredients(inputs)

        assertEquals(emptyList(), result)
    }
}

class OfflineCategorisationTest {

    @Test
    fun `categoriseOffline returns used, missing, and unused correctly`() {
        val cached = listOf("butter", "egg", "flour")
        val userInputs = listOf("flour", "sugar")

        val (used, missing, unused) = categoriseOffline(cached, userInputs)

        assertEquals(listOf("flour"), used)
        assertEquals(listOf("sugar"), missing)
        assertEquals(listOf("butter", "egg"), unused.sorted())
    }
}

private fun fakeIngredientDto() = IngredientSearchResponseDto(
    id = 1,
    title = "Apple Cake",
    image = "https://example.com/cake.jpg",
    missedIngredients = listOf(IngredientItemDto("sugar")),
    usedIngredients = listOf(IngredientItemDto("apple")),
    unusedIngredients = emptyList()
)

private fun fakeOverviewEntity(id: Int) = RecipeOverviewEntity(
    id = id,
    title = "Recipe $id",
    imageUrl = "https://example.com/$id.jpg",
    readyInMinutes = 30,
    dishType = "dinner"
)
