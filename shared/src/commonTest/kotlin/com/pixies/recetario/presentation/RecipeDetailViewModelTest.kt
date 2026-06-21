package com.pixies.recetario.presentation

import com.pixies.recetario.TestDispatcherRule
import com.pixies.recetario.domain.exception.NetworkException
import com.pixies.recetario.domain.exception.RecipeNotFoundException
import com.pixies.recetario.domain.model.RecipeInstructions
import com.pixies.recetario.domain.usecase.GetRecipeInstructionsUseCase
import com.pixies.recetario.presentation.detail.DetailState
import com.pixies.recetario.presentation.detail.RecipeDetailViewModel
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
class RecipeDetailViewModelTest {

    private val dispatcherRule = TestDispatcherRule()
    private val useCase: GetRecipeInstructionsUseCase = mockk()

    @BeforeTest
    fun setUp() = dispatcherRule.before()

    @AfterTest
    fun tearDown() = dispatcherRule.after()

    @Test
    fun `initial state is Loading`() = runTest(dispatcherRule.dispatcher) {
        val viewModel = RecipeDetailViewModel(useCase)

        assertEquals(DetailState.Loading, viewModel.state.value)
    }

    @Test
    fun `load transitions to Success with instructions`() = runTest(dispatcherRule.dispatcher) {
        val instructions = fakeInstructions()
        coEvery { useCase(42) } returns instructions

        val viewModel = RecipeDetailViewModel(useCase)
        viewModel.load(42)
        advanceUntilIdle()

        assertEquals(DetailState.Success(instructions), viewModel.state.value)
    }

    @Test
    fun `load transitions to Error on network failure`() = runTest(dispatcherRule.dispatcher) {
        val exception = NetworkException(RuntimeException("no internet"))
        coEvery { useCase(42) } throws exception

        val viewModel = RecipeDetailViewModel(useCase)
        viewModel.load(42)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is DetailState.Error)
        assertEquals(exception, state.exception)
    }

    @Test
    fun `retry after error transitions back to Loading then Success`() = runTest(dispatcherRule.dispatcher) {
        coEvery { useCase(42) } throws NetworkException(RuntimeException("fail"))

        val viewModel = RecipeDetailViewModel(useCase)
        viewModel.load(42)
        advanceUntilIdle()
        assertTrue(viewModel.state.value is DetailState.Error)

        val instructions = fakeInstructions()
        coEvery { useCase(42) } returns instructions
        viewModel.retry(42)
        advanceUntilIdle()

        assertEquals(DetailState.Success(instructions), viewModel.state.value)
    }

    @Test
    fun `RecipeNotFoundException leads to Error state`() = runTest(dispatcherRule.dispatcher) {
        val exception = RecipeNotFoundException(42)
        coEvery { useCase(42) } throws exception

        val viewModel = RecipeDetailViewModel(useCase)
        viewModel.load(42)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is DetailState.Error)
        assertEquals(exception, state.exception)
    }
}

private fun fakeInstructions() = RecipeInstructions(
    id = 42,
    instructions = mapOf(
        "" to listOf("Boil water", "Add pasta"),
        "Sauce" to listOf("Mix sauce")
    )
)
