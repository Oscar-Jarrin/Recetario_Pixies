package com.pixies.recetario.presentation

import com.pixies.recetario.domain.exception.NetworkException
import com.pixies.recetario.domain.model.RecipeOverview
import com.pixies.recetario.domain.usecase.GetRandomRecipesUseCase
import com.pixies.recetario.presentation.home.HomeState
import com.pixies.recetario.presentation.home.HomeViewModel
import com.pixies.recetario.TestDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcherRule = TestDispatcherRule()
    private val useCase: GetRandomRecipesUseCase = mockk()

    @BeforeTest
    fun setUp() = dispatcherRule.before()

    @AfterTest
    fun tearDown() = dispatcherRule.after()

    @Test
    fun `initial state is Loading before coroutine runs`() = runTest(dispatcherRule.dispatcher) {
        coEvery { useCase() } returns emptyList()

        val viewModel = HomeViewModel(useCase)

        assertEquals(HomeState.Loading, viewModel.state.value)
    }

    @Test
    fun `load transitions to Success with recipes`() = runTest(dispatcherRule.dispatcher) {
        val recipes = listOf(fakeRecipe())
        coEvery { useCase() } returns recipes

        val viewModel = HomeViewModel(useCase)
        advanceUntilIdle()

        assertEquals(HomeState.Success(recipes), viewModel.state.value)
    }

    @Test
    fun `load transitions to Success with empty list`() = runTest(dispatcherRule.dispatcher) {
        coEvery { useCase() } returns emptyList()

        val viewModel = HomeViewModel(useCase)
        advanceUntilIdle()

        assertEquals(HomeState.Success(emptyList()), viewModel.state.value)
    }

    @Test
    fun `load transitions to Error on network failure`() = runTest(dispatcherRule.dispatcher) {
        val exception = NetworkException(RuntimeException("no internet"))
        coEvery { useCase() } throws exception

        val viewModel = HomeViewModel(useCase)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is HomeState.Error)
        assertEquals(exception, state.exception)
    }

    @Test
    fun `retry after error succeeds`() = runTest(dispatcherRule.dispatcher) {
        val recipes = listOf(fakeRecipe())
        coEvery { useCase() } throws NetworkException(RuntimeException("first fail"))

        val viewModel = HomeViewModel(useCase)
        advanceUntilIdle()
        assertTrue(viewModel.state.value is HomeState.Error)

        coEvery { useCase() } returns recipes
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(HomeState.Success(recipes), viewModel.state.value)
    }
}

private fun fakeRecipe() = RecipeOverview(
    id = 1,
    title = "Test Recipe",
    imageUrl = "https://example.com/image.jpg",
    readyInMinutes = 30,
    dishType = "dinner"
)
