package com.pixies.recetario.presentation

import com.pixies.recetario.TestDispatcherRule
import com.pixies.recetario.domain.exception.NetworkException
import com.pixies.recetario.domain.model.IngredientSearchResult
import com.pixies.recetario.domain.usecase.SearchRecipesByIngredientsUseCase
import com.pixies.recetario.presentation.search.SearchState
import com.pixies.recetario.presentation.search.SearchViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val dispatcherRule = TestDispatcherRule()
    private val useCase: SearchRecipesByIngredientsUseCase = mockk()
    private val viewModel by lazy { SearchViewModel(useCase) }

    @BeforeTest
    fun setUp() = dispatcherRule.before()

    @AfterTest
    fun tearDown() = dispatcherRule.after()

    @Test
    fun `initial state is Idle`() = runTest(dispatcherRule.dispatcher) {
        assertEquals(SearchState.Idle, viewModel.state.value)
    }

    @Test
    fun `search with results transitions to Success`() = runTest(dispatcherRule.dispatcher) {
        val results = listOf(fakeResult())
        coEvery { useCase(listOf("apple")) } returns results

        viewModel.search("apple")
        advanceUntilIdle()

        assertEquals(SearchState.Success(results), viewModel.state.value)
    }

    @Test
    fun `search failure transitions to Error`() = runTest(dispatcherRule.dispatcher) {
        val exception = NetworkException(RuntimeException("offline"))
        coEvery { useCase(any()) } throws exception

        viewModel.search("apple")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is SearchState.Error)
        assertEquals(exception, state.exception)
    }

    @Test
    fun `empty query stays Idle without network call`() = runTest(dispatcherRule.dispatcher) {
        viewModel.search("")
        advanceUntilIdle()

        assertEquals(SearchState.Idle, viewModel.state.value)
    }

    @Test
    fun `clearSearch resets state to Idle`() = runTest(dispatcherRule.dispatcher) {
        coEvery { useCase(any()) } returns listOf(fakeResult())
        viewModel.search("apple")
        advanceUntilIdle()
        assertTrue(viewModel.state.value is SearchState.Success)

        viewModel.clearSearch()

        assertEquals(SearchState.Idle, viewModel.state.value)
    }
}

private fun fakeResult() = IngredientSearchResult(
    id = 1,
    title = "Apple Cake",
    imageUrl = "https://example.com/cake.jpg",
    missingIngredients = listOf("sugar"),
    usedIngredients = listOf("apple"),
    unusedIngredients = emptyList()
)
