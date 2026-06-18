package com.pixies.recetario.data

import com.pixies.recetario.data.local.dao.RecipeIngredientDao
import com.pixies.recetario.data.local.dao.RecipeOverviewDao
import com.pixies.recetario.data.local.entity.RecipeIngredientEntity
import com.pixies.recetario.data.local.entity.RecipeOverviewEntity
import com.pixies.recetario.data.remote.SpoonacularApiService
import com.pixies.recetario.data.remote.dto.ExtendedIngredientDto
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
