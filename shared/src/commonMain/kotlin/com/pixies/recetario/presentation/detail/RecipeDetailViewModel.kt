package com.pixies.recetario.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixies.recetario.domain.usecase.GetRecipeInstructionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeDetailViewModel(private val getRecipeInstructions: GetRecipeInstructionsUseCase) : ViewModel() {

    private val _state = MutableStateFlow<DetailState>(DetailState.Loading)
    val state: StateFlow<DetailState> = _state.asStateFlow()

    fun load(id: Int) {
        viewModelScope.launch {
            _state.value = DetailState.Loading
            _state.value = runCatching { DetailState.Success(getRecipeInstructions(id)) }
                .getOrElse { DetailState.Error(it as Exception) }
        }
    }

    fun retry(id: Int) { load(id) }
}
